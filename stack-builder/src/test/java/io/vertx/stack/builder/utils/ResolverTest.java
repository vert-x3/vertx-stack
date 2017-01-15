package io.vertx.stack.builder.utils;

import io.vertx.stack.builder.model.StackDependency;
import org.apache.maven.model.Exclusion;
import org.eclipse.aether.artifact.Artifact;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolverTest {

  Resolver resolver = new Resolver();

  @Test
  public void resolveArtifactWithoutTransitives() {
    StackDependency dependency = new StackDependency("io.vertx", "vertx-core", "3.0.0").setIgnoreTransitive(true);
    List<Artifact> artifacts = resolver.resolve(dependency);
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.get(0).getFile())
        .exists().isFile().hasName("vertx-core-3.0.0.jar");
  }

  @Test
  public void resolveArtifactWithTransitives() {
    StackDependency dependency = new StackDependency("io.vertx", "vertx-core", "3.0.0");
    List<Artifact> artifacts = resolver.resolve(dependency);
    // Include compile, transitive, ignore optional and excluded dependencies.
    assertThat(artifacts).hasSize(10);
    assertThat(artifacts.get(0).getFile()).isFile().hasName("vertx-core-3.0.0.jar");
    for (Artifact artifact : artifacts) {
      assertThat(artifact.getFile()).isFile();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void resolutionOfMissingArtifact() {
    StackDependency dependency = new StackDependency("io.vertx", "vertx-core-missing", "3.0.0")
        .setIgnoreTransitive(true);
    resolver.resolve(dependency);
  }

  @Test
  public void resolutionOfOptional() {
    StackDependency dependency = new StackDependency("io.vertx", "vertx-core", "3.0.0");
    List<String> list = resolver.resolve(dependency)
        .stream().map(a -> a.getFile().getName()).collect(Collectors.toList());
    assertThat(list)
        .contains("vertx-core-3.0.0.jar")
        .doesNotContain("vertx-codegen-3.0.0.jar")
        .doesNotContain("vertx-docgen-3.0.0.jar")
        .doesNotContain("log4j-1.2.17.jar")
        .doesNotContain("junit-4.11.jar");
  }

  @Test
  public void resolutionOfExclusion() {
    Exclusion exclusion = new Exclusion();
    exclusion.setGroupId("com.fasterxml.jackson.core");
    exclusion.setArtifactId("jackson-annotations");

    StackDependency dependency = new StackDependency("io.vertx", "vertx-core", "3.0.0");
    dependency.setExclusions(Collections.singletonList(exclusion));

    List<String> list = resolver.resolve(dependency)
        .stream().map(a -> a.getFile().getName()).collect(Collectors.toList());
    assertThat(list)
        .contains("vertx-core-3.0.0.jar")
        .doesNotContain("vertx-codegen-3.0.0.jar")
        .doesNotContain("vertx-docgen-3.0.0.jar")
        .doesNotContain("log4j-1.2.17.jar")
        .doesNotContain("jackson-annotations-2.5.0.jar");
  }

}