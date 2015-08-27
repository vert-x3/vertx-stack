package io.vertx.stack.builder;

import com.google.common.collect.ImmutableList;
import io.vertx.stack.builder.model.AdditionalFile;
import io.vertx.stack.builder.model.BaseStackDependency;
import io.vertx.stack.builder.model.Stack;
import io.vertx.stack.builder.model.StackDependency;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Exclusion;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackBuilderTest {


  @Test
  public void testExtension() {
    File directory = new File("target/tmp/extension");
    StackBuilder builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(directory)
            .setDependencies(
                ImmutableList.of(
                    new StackDependency("io.vertx", "vertx-rx-java", "3.0.0").setIgnoreTransitive(true),
                    new StackDependency("org.hsqldb", "hsqldb", "2.3.3")))
            .setFiles(ImmutableList.of(
                new AdditionalFile().setFile(new File("src/test/resources/hello.html")).setTargetDirectory("files")))
    );
    builder.build();
    assertThat(new File(directory, "lib/hsqldb-2.3.3.jar")).isFile();
    assertThat(new File(directory, "lib/vertx-rx-java-3.0.0.jar")).isFile();
    assertThat(new File(directory, "lib/rxjava-1.0.8.jar")).doesNotExist();
  }

  @Test
  public void testResolutionById() {
    StackBuilder builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(new File("target/tmp/ids/full"))
            .setFrom(new BaseStackDependency().setId("full")));
    builder.build();
    ensureStack(new File("target/tmp/ids/full"));

    builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(new File("target/tmp/ids/base"))
            .setFrom(new BaseStackDependency().setId("base")));
    builder.build();
    ensureStack(new File("target/tmp/ids/base"));

    builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(new File("target/tmp/ids/min"))
            .setFrom(new BaseStackDependency().setId("min")));
    builder.build();
    ensureStack(new File("target/tmp/ids/min"));
  }

  @Test
  public void testDependencyExclusions() {
    File root = new File("target/tmp/exclusions");
    FileUtils.deleteQuietly(root);

    Exclusion exclusion = new Exclusion();
    exclusion.setGroupId("io.reactivex");
    exclusion.setArtifactId("rxjava");

    StackDependency dependency =
        new StackDependency("io.vertx", "vertx-rx-java", "3.0.0");
    dependency.setExclusions(Collections.singletonList(exclusion));

    StackBuilder builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(root)
            .setFrom(null)
            .setDependencies(
                ImmutableList.of(dependency)
            )
    );
    builder.build();
    assertThat(new File(root, "lib/vertx-rx-java-3.0.0.jar")).isFile();
    assertThat(new File(root, "lib/rxjava-1.0.8.jar")).doesNotExist();
  }

  @Test
  public void testAddingDirectory() {
    StackBuilder builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(new File("target/tmp/directory"))
            .addFile(new AdditionalFile()
                    .setFile(new File("src/test/resources/files"))
                    .setTargetDirectory("files")
            ));
    builder.build();

    assertThat(new File("target/tmp/directory/files/file.txt")).isFile();
  }

  @Test(expected = IllegalStateException.class)
  public void testAddingMissingDirectory() {
    StackBuilder builder = new StackBuilder().setStack(
        new Stack()
            .setDirectory(new File("target/tmp/foo"))
            .addFile(new AdditionalFile()
                    .setFile(new File("src/test/resources/missing"))
                    .setTargetDirectory("files")
            ));
    builder.build();
  }

  private void ensureStack(File root) {
    assertThat(root).isDirectory();

    assertThat(new File(root, "bin/vertx")).isFile();
    assertThat(new File(root, "bin/vertx.bat")).isFile();

    assertThat(new File(root, "conf/default-cluster.xml")).isFile();
    assertThat(new File(root, "conf/logging.properties")).isFile();

    assertThat(new File(root, "lib").list((dir, name) ->
      name.startsWith("vertx-core-") && name.endsWith(".jar")
    )).hasSize(1);
  }
}