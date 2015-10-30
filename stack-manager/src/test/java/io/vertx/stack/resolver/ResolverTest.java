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


import io.vertx.stack.utils.FileUtils;
import io.vertx.stack.utils.LocalArtifact;
import io.vertx.stack.utils.LocalDependency;
import io.vertx.stack.utils.LocalRepoBuilder;
import org.eclipse.aether.artifact.Artifact;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolverTest {

  public final static File ROOT = new File("target/test-repos");

  public final static File LOCAL = new File(ROOT, "fake-local-maven-repo");

  private Resolver resolver = Resolver.create(new ResolverOptions().setLocalRepository(LOCAL.getAbsolutePath()));

  @Before
  public void setUp() {
    FileUtils.delete(LOCAL);
  }

  @Test
  public void testSimpleResolutionWithoutTransitive() {
    new LocalRepoBuilder(LOCAL).addArtifact(new LocalArtifact("com.acme", "acme", "1.0").generateMainArtifact()).build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(false));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .containsExactly("com.acme:acme:txt:1.0");
  }

  @Test
  public void testSimpleResolutionWithTransitive() {
    new LocalRepoBuilder(LOCAL).addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
        .generateMainArtifact()).build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .containsExactly("com.acme:acme:txt:1.0");
  }

  @Test
  public void testResolutionOfArtifactsWithDependencies() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(3)
        .contains("com.acme:acme-lib:txt:1.0", "com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0");
  }

  @Test
  public void testResolutionOfArtifactsWithProvidedDependencies() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt").scope("provided"))
            .addDependency(new LocalDependency("com.acme", "acme-test", "1.0").type("txt").scope("test"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(2)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0");
  }

  @Test
  public void testResolutionWhenADependencyIsPresentTwiceInTheGraph() {
    
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-test", "1.0").type("txt").scope("test"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(3)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0", "com.acme:acme-lib:txt:1.0");
  }

  @Test
  public void testResolutionWhenADependencyIsPresentTwiceInTheGraphWithDifferentScope() {
    
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt").scope("provided"))
        )
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-test", "1.0").type("txt").scope("test"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(3)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0", "com.acme:acme-lib:txt:1.0");
  }


  @Test
  public void testResolutionWhenADependencyIsPresentTwiceInTheGraphWithVersion() {
    
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.1").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-test", "1.0").type("txt").scope("test"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.1").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(3)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.1", "com.acme:acme-lib:txt:1.0");
  }

  @Test
  public void testResolutionWhenADependencyIsPresentTwiceInTheGraphWithVersionInverted() {
    
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.1").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-lib", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.1").type("txt"))
        )
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-lib", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-test", "1.0").type("txt").scope("test"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(3)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0", "com.acme:acme-lib:txt:1.0");
  }


  @Test
  public void testThatOptionalDependenciesAndThereDependenciesAreNotResolved() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme-optional", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-optional-2", "1.0").type("txt")))
        .addArtifact(new LocalArtifact("com.acme", "acme-optional-2", "1.0").generateMainArtifact())
        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-optional", "1.0").type("txt").optional(true))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::toString).collect(Collectors.toList()))
        .hasSize(2)
        .contains("com.acme:acme:txt:1.0", "com.acme:acme-api:txt:1.0");
  }

  @Test
  public void testThatOptionalDependenciesAndThereDependenciesAreResolvedWithATransitiveNotOptionalMentionedFirst() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-optional", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-log", "1.0").generateMainArtifact())

        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-optional", "1.0").type("txt").optional(true))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::getArtifactId).collect(Collectors.toList()))
        .hasSize(3)
        .contains("acme", "acme-api", "acme-log");
  }

  @Test
  public void testThatOptionalDependenciesAndThereDependenciesAreNotResolvedWithATransitiveNotOptionalFirst() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-optional", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-log", "1.0").generateMainArtifact())

        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-optional", "1.0").type("txt").optional(true))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0", new ResolutionOptions().setWithTransitive(true));
    assertThat(artifacts.stream().map(Artifact::getArtifactId).collect(Collectors.toList()))
        .hasSize(2)
        .contains("acme", "acme-api");
  }


  @Test
  public void testThatExcludedDependenciesAndThereDependenciesAreResolvedWithATransitiveNotExcludedMentionedFirst() {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-excluded", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-log", "1.0").generateMainArtifact())

        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-excluded", "1.0").type("txt"))
        )
        .build();
    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true).addExclusion("com.acme:acme-excluded"));
    assertThat(artifacts.stream().map(Artifact::getArtifactId).collect(Collectors.toList()))
        .hasSize(3)
        .contains("acme", "acme-api", "acme-log");
  }

  @Test
  public void testThatExcludedDependenciesAndThereDependenciesAreNotResolvedWithoutATransitiveNotExcludedMentionedFirst
      () {
    new LocalRepoBuilder(LOCAL)
        .addArtifact(new LocalArtifact("com.acme", "acme-api", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-excluded", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-log", "1.0").type("txt")))

        .addArtifact(new LocalArtifact("com.acme", "acme-log", "1.0").generateMainArtifact())

        .addArtifact(new LocalArtifact("com.acme", "acme", "1.0")
            .generateMainArtifact()
            .addDependency(new LocalDependency("com.acme", "acme-excluded", "1.0").type("txt"))
            .addDependency(new LocalDependency("com.acme", "acme-api", "1.0").type("txt"))
        )
        .build();

    List<Artifact> artifacts = resolver.resolve("com.acme:acme:txt:1.0",
        new ResolutionOptions().setWithTransitive(true).addExclusion("com.acme:acme-excluded"));
    assertThat(artifacts.stream().map(Artifact::getArtifactId).collect(Collectors.toList()))
        .hasSize(2)
        .contains("acme", "acme-api");
  }

}
