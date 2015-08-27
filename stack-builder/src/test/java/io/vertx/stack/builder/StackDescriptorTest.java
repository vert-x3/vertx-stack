package io.vertx.stack.builder;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the different descriptor format.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackDescriptorTest {

  @Before
  public void setUp() {
    FileUtils.deleteQuietly(new File("target/stack/json"));
    FileUtils.deleteQuietly(new File("target/stack/xml"));
    FileUtils.deleteQuietly(new File("target/stack/yaml"));
  }

  @Test
  public void testFromJson() {
    StackBuilder builder = new StackBuilder()
        .fromJsonFile(new File("src/test/resources/stack.json"));
    builder.build();
    ensureStack(new File("target/stack/json"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithMissingJsonFile() {
    new StackBuilder()
        .fromJsonFile(new File("src/test/resources/missing.json"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithErroneousJsonFile() {
    new StackBuilder()
        .fromJsonFile(new File("src/test/resources/stack-erroneous.json"));
  }

  @Test
  public void testFromXML() {
    StackBuilder builder = new StackBuilder()
        .fromXmlFile(new File("src/test/resources/stack.xml"));
    builder.build();
    ensureStack(new File("target/stack/xml"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithMissingXMLFile() {
    new StackBuilder()
        .fromXmlFile(new File("src/test/resources/missing.xml"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithErroneousXmlFile() {
    new StackBuilder()
        .fromXmlFile(new File("src/test/resources/stack-erroneous.xml"));
  }


  @Test
  public void testFromYaml() {
    StackBuilder builder = new StackBuilder()
        .fromYamlFile(new File("src/test/resources/stack.yaml"));
    builder.build();
    ensureStack(new File("target/stack/yaml"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithMissingYamlFile() {
    new StackBuilder()
        .fromYamlFile(new File("src/test/resources/missing.yaml"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithErroneousYamlFile() {
    new StackBuilder()
        .fromYamlFile(new File("src/test/resources/stack-erroneous.yaml"));
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

    File lib = new File(root, "lib");
    assertThatDirectoryHasArtifact(lib, "jackson-core");

    // vertx-rx-java without its dependencies
    assertThatDirectoryHasArtifact(lib, "vertx-rx-java");
    assertThatDirectoryDoesNotHaveArtifact(lib, "rxjava");
    assertThatDirectoryDoesNotHaveArtifact(lib, "vertx-docgen");

    // hsqldb
    assertThatDirectoryHasArtifact(lib, "hsqldb");
  }

  private String assertThatDirectoryHasArtifact(File directory, String artifactId) {
    File[] files = directory.listFiles((dir, name) -> {
      return name.startsWith(artifactId + "-");
    });
    assertThat(files).isNotEmpty().hasSize(1);
    // Extract version
    String name = files[0].getName();
    int indexOfDot = name.lastIndexOf(".");
    int indexOfDash = name.indexOf("-", artifactId.length());
    return name.substring(indexOfDash + 1, indexOfDot);
  }

  private void assertThatDirectoryDoesNotHaveArtifact(
      File directory, String artifactId) {
    File[] files = directory.listFiles((dir, name) -> {
      return name.startsWith(artifactId + "-");
    });
    assertThat(files).isEmpty();
  }
}
