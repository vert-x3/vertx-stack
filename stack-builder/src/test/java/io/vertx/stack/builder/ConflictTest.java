package io.vertx.stack.builder;

import io.vertx.stack.builder.model.BaseStackDependency;
import io.vertx.stack.builder.model.Stack;
import io.vertx.stack.builder.model.StackDependency;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ConflictTest {

  public static File BASE = new File("target/base/min");

  private static File ROOT = new File("target/tmp/conflicts");

  @Before
  public void setup() throws IOException {
    if (BASE.isDirectory()) {
      return;
    } else {
      StackBuilder builder = new StackBuilder()
          .setStack(
              new Stack()
                  .setFrom(BaseStackDependency.MIN)
                  .setDirectory(BASE));
      builder.build();
    }
    FileUtils.deleteQuietly(ROOT);
    FileUtils.copyDirectory(BASE, ROOT);
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(ROOT);
  }

  @Test
  public void testNoConflict() {
    assertThat(new File(ROOT, "lib/hsqldb-2.3.3.jar")).doesNotExist();
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency().setGACV("org.hsqldb:hsqldb:2.3.3")));
    builder.build();
    assertThat(new File(ROOT, "lib/hsqldb-2.3.3.jar")).isFile();

    //Exactly the same file.
    builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency().setGACV("org.hsqldb:hsqldb:2.3.3")));
    builder.build();
    assertThat(new File(ROOT, "lib/hsqldb-2.3.3.jar")).isFile();
  }

  @Test
  public void testReleaseConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency().setGACV("org.hsqldb:hsqldb:2.3.2")));
    builder.build();

    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency().setGACV("org.hsqldb:hsqldb:2.3.3")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void testReleaseVsSnapshotConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.0")));
    builder.build();

    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency()
                  .setGACV("org.acme:test-artifact:txt:1.1-SNAPSHOT")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void testSnapshotVsReleaseConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.1-SNAPSHOT")));
    builder.build();

    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency()
                  .setGACV("org.acme:test-artifact:txt:1.0")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void testSnapshotVsSnapshotConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.1-SNAPSHOT")));
    builder.build();

    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency()
                  .setGACV("org.acme:test-artifact:txt:1.2-SNAPSHOT")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void testSnapshotVsTimestampSnapshotConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.1-SNAPSHOT")));
    builder.build();

    File file = new File(ROOT, "lib/test-artifact-1.1-SNAPSHOT.txt");
    file.renameTo(new File(ROOT, "lib/test-artifact-1.1-20150827.080600-1.txt"));
    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency()
                  .setGACV("org.acme:test-artifact:txt:1.2-SNAPSHOT")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void testNoUpdateSnapshotVsTimestampSnapshot() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.2-SNAPSHOT")));
    builder.build();

    File file = new File(ROOT, "lib/test-artifact-1.2-SNAPSHOT.txt");
    file.renameTo(new File(ROOT, "lib/test-artifact-1.2-20150827.080600-1.txt"));
    builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.2-SNAPSHOT")));
    builder.build();
    assertThat(new File(ROOT, "lib/test-artifact-1.2-20150827.080600-1.txt")).isFile();
  }

  @Test
  public void testTimestampSnapshotVsReleaseConflict() {
    StackBuilder builder = new StackBuilder()
        .setStack(new Stack()
            .setDirectory(ROOT)
            .setFrom(null)
            .addDependency(new StackDependency()
                .setGACV("org.acme:test-artifact:txt:1.1-SNAPSHOT")));
    builder.build();
    File file = new File(ROOT, "lib/test-artifact-1.1-SNAPSHOT.txt");
    file.renameTo(new File(ROOT, "lib/test-artifact-1.1-20150827.080600-1.txt"));

    try {
      builder = new StackBuilder()
          .setStack(new Stack()
              .setDirectory(ROOT)
              .setFrom(null)
              .addDependency(new StackDependency()
                  .setGACV("org.acme:test-artifact:txt:1.0")));
      builder.build();
      fail("Conflict expected");
    } catch (IllegalStateException e) {
      // OK
    }
  }
}
