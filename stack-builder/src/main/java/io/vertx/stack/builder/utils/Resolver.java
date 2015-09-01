package io.vertx.stack.builder.utils;

import io.vertx.stack.builder.model.StackDependency;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Automates dependency resolution based on Aether.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Resolver {

  public static final Logger LOGGER = Logger.getLogger("vertx-stack-builder-resolver");

  public static final String LOCAL_REPO_SYS_PROP = "vertx.maven.localRepo";
  public static final String REMOTE_REPOS_SYS_PROP = "vertx.maven.remoteRepos";
  public static final String HTTP_PROXY_SYS_PROP = "vertx.maven.httpProxy";
  public static final String HTTPS_PROXY_SYS_PROP = "vertx.maven.httpsProxy";
  public static final String REMOTE_SNAPSHOT_POLICY_SYS_PROP = "vertx.maven.remoteSnapshotPolicy";

  private static final String USER_HOME = System.getProperty("user.home");
  private static final String FILE_SEP = System.getProperty("file.separator");
  private static final String DEFAULT_MAVEN_LOCAL = USER_HOME + FILE_SEP + ".m2" + FILE_SEP + "repository";
  private static final String DEFAULT_MAVEN_REMOTES =
      "http://central.maven.org/maven2/ https://oss.sonatype.org/content/repositories/snapshots/";

  private final RepositorySystem system;
  private final LocalRepository localRepo;
  private final List<RemoteRepository> remotes = new ArrayList<>();

  public Resolver() {
    String localMavenRepo = System.getProperty(LOCAL_REPO_SYS_PROP, DEFAULT_MAVEN_LOCAL);
    String remoteString = System.getProperty(REMOTE_REPOS_SYS_PROP, DEFAULT_MAVEN_REMOTES);
    // They are space delimited (space is illegal char in urls)
    List<String> remoteMavenRepos = Arrays.asList(remoteString.split(" "));
    String httpProxy = System.getProperty(HTTP_PROXY_SYS_PROP);
    String httpsProxy = System.getProperty(HTTPS_PROXY_SYS_PROP);

    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        LOGGER.log(Level.SEVERE, "Service creation failure: " + exception.getMessage(), exception);
      }
    });

    system = locator.getService(RepositorySystem.class);
    localRepo = new LocalRepository(localMavenRepo);
    Proxy proxy = null;
    if (httpProxy != null) {
      URL url = url(httpProxy);
      Authentication authentication = extractAuth(url);
      proxy = new Proxy("http", url.getHost(), url.getPort(), authentication);
    }
    Proxy secureProxy = null;
    if (httpsProxy != null) {
      URL url = url(httpsProxy);
      Authentication authentication = extractAuth(url);
      secureProxy = new Proxy("https", url.getHost(), url.getPort(), authentication);
    }

    int count = 0;
    for (String remote : remoteMavenRepos) {
      URL url = url(remote);
      Authentication auth = extractAuth(url);
      if (auth != null) {
        url = url(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
      }
      RemoteRepository.Builder builder = new RemoteRepository.Builder("repo" + (count++), "default", url.toString());
      if (auth != null) {
        builder.setAuthentication(auth);
      }
      switch (url.getProtocol()) {
        case "http":
          if (proxy != null) {
            builder.setProxy(proxy);
          }
          break;
        case "https":
          if (secureProxy != null) {
            builder.setProxy(secureProxy);
          }
          break;
      }
      customizeRemoteRepoBuilder(builder);
      RemoteRepository remoteRepo = builder.build();
      remotes.add(remoteRepo);
    }
  }

  private URL url(String u) {
    try {
      return new URL(u);
    } catch (MalformedURLException e) {
      LOGGER.log(Level.SEVERE, "Cannot create url from " + u, e);
      throw new IllegalArgumentException("Invalid url " + u);
    }
  }

  private URL url(String protocol, String host, int port, String file) {
    try {
      return new URL(protocol, host, port, file);
    } catch (MalformedURLException e) {
      final String url = "{protocol:" + protocol + ", host:" + host + ", port:" + port + ", file:" + file + "}";
      LOGGER.log(Level.SEVERE, "Cannot create url from " + url, e);
      throw new IllegalArgumentException("Invalid url " + url);
    }
  }

  public List<Artifact> resolve(StackDependency dep) {

    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    LOGGER.info("Resolving " + dep.getGACV());

    Artifact artifact = new DefaultArtifact(dep.getGACV());
    DependencyFilter filter =
        DependencyFilterUtils.andFilter(
            DependencyFilterUtils.classpathFilter(
                JavaScopes.COMPILE
            ),
            // Remove optionals and dependencies of optionals
            (dependencyNode, list) -> {
              for (DependencyNode parent : list) {
                if (parent.getDependency().isOptional()) {
                  return false;
                }
              }

              return !dependencyNode.getDependency().isOptional();
            },

            // Remove excluded dependencies
            (dependencyNode, list) -> {
              // Build the list of exclusion, traverse the tree.
              Collection<Exclusion> exclusions = new ArrayList<>();
              for (DependencyNode parent : list) {
                exclusions.addAll(parent.getDependency().getExclusions());
              }

              for (Exclusion e : exclusions) {
                // Check the the passed artifact is excluded
                if (e.getArtifactId().equals(dependencyNode.getArtifact().getArtifactId())
                    && e.getGroupId().equals(dependencyNode.getArtifact().getGroupId())) {
                  return false;
                }

                // Check if a parent artifact is excluded
                for (DependencyNode parent : list) {
                  if (e.getArtifactId().equals(parent.getArtifact().getArtifactId())
                      && e.getGroupId().equals(parent.getArtifact().getGroupId())) {
                    return false;
                  }
                }
              }
              return true;
            },

            // Remove provided dependencies and transitive dependencies of provided dependencies
            (dependencyNode, list) -> {
              for (DependencyNode parent : list) {
                if (! parent.getDependency().getScope().toLowerCase().equals("compile")) {
                  return false;
                }
              }
              return dependencyNode.getDependency().getScope().toLowerCase().equals("compile");
            }
        );




    List<ArtifactResult> artifactResults;
    try {
      if (dep.isIgnoreTransitive()) {
        ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remotes, null);
        artifactResults = Collections.singletonList(system.resolveArtifact(session, artifactRequest));
      } else {
        CollectRequest collectRequest = new CollectRequest();
        Dependency root = new Dependency(artifact, JavaScopes.COMPILE)
            .setExclusions(
                dep.getExclusions().stream()
                    .map(e -> new Exclusion(e.getGroupId(), e.getArtifactId(), null, null))
                    .collect(Collectors.toList()));
        collectRequest.setRoot(root);
        collectRequest.setRepositories(remotes);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
        artifactResults =
            system.resolveDependencies(session, dependencyRequest).getArtifactResults();
      }
    } catch (DependencyResolutionException | ArtifactResolutionException e) {
      throw new IllegalArgumentException("Cannot resolve artifact " + dep.getGACV() +
          " in maven repositories: " + e.getMessage());
    } catch (NullPointerException e) {
      // Sucks, but aether throws a NPE if repository name is invalid....
      throw new IllegalArgumentException("Cannot find module " + dep.getGACV() + ". Maybe repository URL is invalid?");
    }

    return artifactResults.stream().map(ArtifactResult::getArtifact)
        .collect(Collectors.toList());
  }

  protected void customizeRemoteRepoBuilder(RemoteRepository.Builder builder) {
    String updatePolicy = System.getProperty(REMOTE_SNAPSHOT_POLICY_SYS_PROP);
    if (updatePolicy != null && !updatePolicy.isEmpty()) {
      builder.setSnapshotPolicy(new RepositoryPolicy(true, updatePolicy, RepositoryPolicy.CHECKSUM_POLICY_WARN));
    }
  }


  private static Authentication extractAuth(URL url) {
    String userInfo = url.getUserInfo();
    if (userInfo != null) {
      AuthenticationBuilder authBuilder = new AuthenticationBuilder();
      int sep = userInfo.indexOf(':');
      if (sep != -1) {
        authBuilder.addUsername(userInfo.substring(0, sep));
        authBuilder.addPassword(userInfo.substring(sep + 1));
      } else {
        authBuilder.addUsername(userInfo);
      }
      return authBuilder.build();
    }
    return null;
  }

  public LocalRepository getLocalRepository() {
    return localRepo;
  }
}
