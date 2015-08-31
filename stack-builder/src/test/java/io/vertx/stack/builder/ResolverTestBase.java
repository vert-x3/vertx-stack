package io.vertx.stack.builder;

import io.vertx.stack.builder.utils.Resolver;
import org.codehaus.plexus.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolverTestBase {

  public static File REMOTE;
  public static File LOCAL;

  @BeforeClass
  public static void setRepoLocations() throws MalformedURLException {
    REMOTE = new File("target/remote");
    LOCAL = new File("target/local");

    FileUtils.mkdir("target/local");
    FileUtils.mkdir("target/remote");

    System.setProperty(Resolver.LOCAL_REPO_SYS_PROP, "target/local");
    System.setProperty(Resolver.REMOTE_REPOS_SYS_PROP,  REMOTE.toURI().toURL().toExternalForm()
        + " http://central.maven.org/maven2/"
        + " https://oss.sonatype.org/content/repositories/snapshots/");

    System.out.println("Provision local repository");
    installInLocalRepository("org.acme", "test-artifact", "1.1-SNAPSHOT", "txt",
        new File("src/test/resources/artifact/file.txt"));
    installInLocalRepository("org.acme", "test-artifact", "1.2-SNAPSHOT", "txt",
        new File("src/test/resources/artifact/file.txt"));
    installInLocalRepository("org.acme", "test-artifact", "1.0", "txt",
        new File("src/test/resources/artifact/file.txt"));
    installInLocalRepository("org.acme", "test-artifact-2", "1.0", "txt",
        new File("src/test/resources/artifact/file.txt"));
  }

  @AfterClass
  public static void cleanupRepoLocations() {
    System.clearProperty(Resolver.LOCAL_REPO_SYS_PROP);
    System.clearProperty(Resolver.REMOTE_REPOS_SYS_PROP);
  }

  public static void installInLocalRepository(String groupId, String artifactId, String version, String extension, File file) {
    assertThat(file).isFile();
    File directory = new File(LOCAL, groupId.replace(".", File.separator) + File.separator + artifactId + File
        .separator + version);
    directory.mkdirs();
    File target = new File(directory, artifactId + "-" + version + "." + extension);
    try {
      System.out.println("Copying artifact to " + target.getAbsolutePath());
      FileUtils.copyFile(file, target);
      assertThat(target).isFile();
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot copy artifact to fake local repository", e);
    }
  }
}
