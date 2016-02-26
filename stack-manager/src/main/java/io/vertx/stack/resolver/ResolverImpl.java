/*
 *  Copyright (c) 2011-2015 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.stack.resolver;


import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Resolver} based on Aether.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolverImpl implements Resolver {

  private final static Logger LOGGER = LoggerFactory.getLogger("vertx-stack-resolver");

  public static final String REMOTE_SNAPSHOT_POLICY_SYS_PROP = "vertx.maven.remoteSnapshotPolicy";

  private final RepositorySystem system;
  private LocalRepository localRepo;
  private final List<RemoteRepository> remotes = new ArrayList<>();

  /**
   * Creates a new instance of {@link ResolverImpl} with the given options.
   *
   * @param options the options
   */
  public ResolverImpl(ResolverOptions options) {
    String localMavenRepo = options.getLocalRepository();
    List<String> remoteMavenRepos = options.getRemoteRepositories();
    String httpProxy = options.getHttpProxy();
    String httpsProxy = options.getHttpsProxy();

    DefaultServiceLocator locator = getDefaultServiceLocator();

    system = locator.getService(RepositorySystem.class);
    localRepo = new LocalRepository(localMavenRepo);
    Proxy proxy = getHttpProxy(httpProxy);
    Proxy secureProxy = getHttpsProxy(httpsProxy);

    configureRemoteRepositories(remoteMavenRepos, proxy, secureProxy);
  }

  private Proxy getHttpsProxy(String httpsProxy) {
    Proxy secureProxy = null;
    if (httpsProxy != null) {
      URL url = url(httpsProxy);
      Authentication authentication = extractAuth(url);
      secureProxy = new Proxy("https", url.getHost(), url.getPort(), authentication);
    }
    return secureProxy;
  }

  private Proxy getHttpProxy(String httpProxy) {
    Proxy proxy = null;
    if (httpProxy != null) {
      URL url = url(httpProxy);
      Authentication authentication = extractAuth(url);
      proxy = new Proxy("http", url.getHost(), url.getPort(), authentication);
    }
    return proxy;
  }

  private void configureRemoteRepositories(List<String> remoteMavenRepos, Proxy proxy, Proxy secureProxy) {
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

  private static DefaultServiceLocator getDefaultServiceLocator() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        LOGGER.error("Service creation failure: " + exception.getMessage(), exception);
      }
    });
    return locator;
  }

  private URL url(String u) {
    try {
      return new URL(u);
    } catch (MalformedURLException e) {
      LOGGER.error("Cannot create url from " + u, e);
      throw new IllegalArgumentException("Invalid url " + u);
    }
  }

  private URL url(String protocol, String host, int port, String file) {
    try {
      return new URL(protocol, host, port, file);
    } catch (MalformedURLException e) {
      final String url = "{protocol:" + protocol + ", host:" + host + ", port:" + port + ", file:" + file + "}";
      LOGGER.error("Cannot create url from " + url, e);
      throw new IllegalArgumentException("Invalid url " + url);
    }
  }

  /**
   * Resolve the given artifact.
   *
   * @param artifact   the artifact
   * @param transitive whether or not the transitive dependencies needs to be resolved too
   * @param exclusions the list of exclusions
   * @return the list of artifact
   */
  public List<Artifact> resolve(Artifact artifact, boolean transitive, List<String> exclusions) {

    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    LOGGER.info("Resolving " + artifact.toString());

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
              Collection<Exclusion> ex = new ArrayList<>();
              for (DependencyNode parent : list) {
                ex.addAll(parent.getDependency().getExclusions());
              }

              for (Exclusion e : ex) {
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
                if (!parent.getDependency().getScope().toLowerCase().equals("compile")) {
                  return false;
                }
              }
              return dependencyNode.getDependency().getScope().toLowerCase().equals("compile");
            }
        );


    List<ArtifactResult> artifactResults;
    try {
      if (!transitive) {
        ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remotes, null);
        artifactResults = Collections.singletonList(system.resolveArtifact(session, artifactRequest));
      } else {
        CollectRequest collectRequest = new CollectRequest();
        Dependency root = new Dependency(artifact, JavaScopes.COMPILE)
            .setExclusions(
                exclusions.stream()
                    .map(e -> {
                      // Exclusion are structured as groupId:artifactId.
                      String[] segments = e.split(":");
                      if (segments.length != 2) {
                        throw new IllegalStateException("Invalid exclusion format: " + e + " - exclusion are " +
                            "structured as follows: groupId:artifactId");
                      }
                      return new Exclusion(segments[0], segments[1], null, null);
                    })
                    .collect(Collectors.toList()));
        collectRequest.setRoot(root);
        collectRequest.setRepositories(remotes);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
        artifactResults =
            system.resolveDependencies(session, dependencyRequest).getArtifactResults();
      }
    } catch (DependencyResolutionException | ArtifactResolutionException e) {
      throw new IllegalArgumentException("Cannot resolve artifact " + artifact.toString() +
          " in maven repositories: " + e.getMessage());
    } catch (NullPointerException e) {
      // Sucks, but aether throws a NPE if repository name is invalid....
      throw new IllegalArgumentException("Cannot find module " + artifact.toString() + ". Maybe repository URL is invalid?");
    }

    List<Artifact> artifacts = artifactResults.stream().map(ArtifactResult::getArtifact)
        .collect(Collectors.toList());
    LOGGER.trace("Dependencies resolved by " + artifact.getArtifactId() + " => " + artifacts);
    return artifacts;
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

  @Override
  public List<Artifact> resolve(String gacv, ResolutionOptions options) {
    DefaultArtifact artifact = new DefaultArtifact(gacv);
    return resolve(artifact, options.isWithTransitive(), options.getExclusions());
  }
}

