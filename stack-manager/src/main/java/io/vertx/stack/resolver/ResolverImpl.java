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


import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.stack.model.Artifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link Resolver} based on Aether.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolverImpl implements Resolver {

  private final static Logger LOGGER = LoggerFactory.getLogger("vertx-stack-resolver");

  public static final String REMOTE_SNAPSHOT_POLICY_SYS_PROP = "vertx.maven.remoteSnapshotPolicy";

  private final RepositorySystem system;
  private final LocalRepository localRepo;
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
   * @param withTransitive whether the transitive dependencies needs to be resolved too
   * @param exclusions the list of exclusions
   * @return the list of resolved artifacts
   */
  private DependencyNode resolve(Artifact artifact, boolean withTransitive, List<String> exclusions) {
    CollectRequest collectRequest = collectRequest(artifact, exclusions, remotes);
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter());
    RepositorySystemSession session = session(system, localRepo);
    try {
      DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);
      DependencyNode root = dependencyResult.getRoot();
      if (withTransitive) {
        return root;
      } else {
        root.setChildren(new ArrayList<>());
        return root;
      }
    } catch (DependencyResolutionException e) {
      throw new IllegalArgumentException("Cannot resolve artifact " + artifact.toString() +
        " in maven repositories: " + e.getMessage());
    }
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

  private static RepositorySystemSession session(RepositorySystem system, LocalRepository localRepo) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
    return session;
  }

  private static CollectRequest collectRequest(Artifact artifact, List<String> exclusions, List<RemoteRepository> remotes) {
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
    return collectRequest;
  }

  private static DependencyFilter dependencyFilter() {
    return
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
            if (!parent.getDependency().getScope().equalsIgnoreCase("compile")) {
              return false;
            }
          }
          return dependencyNode.getDependency().getScope().equalsIgnoreCase("compile");
        }
      );
  }

  @Override
  public List<Artifact> resolve(String gacv, ResolutionOptions options) {
    DependencyNode root = resolve(new Artifact(gacv), options.isWithTransitive(), options.getExclusions());
    List<Exclusion> exclusions = Stream.concat(Stream.of(root), root.getChildren().stream())
      .map(DependencyNode::getDependency)
      .flatMap(dependency -> dependency.getExclusions().stream())
      .collect(Collectors.toList());
    Artifact rootArtifact = new Artifact(root.getArtifact(), null);
    return Stream
      .concat(Stream.of(rootArtifact), toArtifacts(root, rootArtifact, exclusions))
      .collect(Collectors.toList());
  }

  private Stream<Artifact> toArtifacts(DependencyNode dependencyNode, Artifact rootArtifact, List<Exclusion> exclusions) {
    return dependencyNode.getChildren().stream()
      // remove optional dependencies
      .filter(childNode -> !childNode.getDependency().isOptional())
      // remove excluded dependencies
      .filter(childNode -> exclusions.stream().noneMatch(exclusion ->
        exclusion.getGroupId().equals(childNode.getArtifact().getGroupId())
          && exclusion.getArtifactId().equals(childNode.getArtifact().getArtifactId())))
      // remove provided dependencies and transitive dependencies of provided dependencies
      .filter(childNode -> childNode.getDependency().getScope().equalsIgnoreCase("compile"))
      .flatMap(childNode -> {
        Artifact childArtifact = new Artifact(childNode.getArtifact(), rootArtifact);
        return Stream.concat(Stream.of(childArtifact), toArtifacts(childNode, childArtifact, exclusions));
      });
  }

}

