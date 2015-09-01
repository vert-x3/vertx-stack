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

package io.vertx.stack.builder;

import com.google.common.collect.ImmutableList;
import io.vertx.stack.builder.model.*;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ConflictTest extends ResolverTestBase {

  public static File BASE = new File("target/base/min");

  private static File ROOT = new File("target/tmp/conflicts");

  @Before
  public void setup() throws IOException {
    if (!BASE.isDirectory()) {
      BaseStackDependency base = new BaseStackDependency();
      base.setGACV("io.vertx:vertx-stack-dist:zip:min:3.0.0");
      base.setStrip(1);
      StackBuilder builder = new StackBuilder()
          .setStack(
              new Stack()
                  .setFrom(base)
                  .setDirectory(BASE));
      builder.build();
    }
    FileUtils.deleteQuietly(ROOT);
    FileUtils.copyDirectory(BASE, ROOT);
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
    assertThat(new File(ROOT, "lib/test-artifact-1.2-SNAPSHOT.txt")).isFile();
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

  @Test
  public void testStackConflictInTheSameStackUsingReleases() {
    try {
      StackBuilder builder = new StackBuilder().setStack(
          new Stack()
              .setDirectory(ROOT)
              .setDependencies(
                  ImmutableList.of(
                      new StackDependency("io.vertx", "vertx-rx-java", "3.0.0"),
                      new StackDependency("org.hsqldb", "hsqldb", "2.3.3"),
                      new StackDependency("org.hsqldb", "hsqldb", "2.3.2")))
              .setFiles(ImmutableList.of(
                  new AdditionalFile().setFile(new File("src/test/resources/hello.html")).setTargetDirectory("files")))
      );
      builder.build();
      fail("A conflict should have been detected");
    } catch (IllegalStateException e) {
      assertThat(e)
          .hasMessageContaining("org.hsqldb:hsqldb:jar:2.3.2")
          .hasMessageContaining("org.hsqldb:hsqldb:jar:2.3.3");
    }
  }

  @Test
  public void testStackConflictInTheSameStackOnDependenciesUsingReleases() {
    try {
      StackBuilder builder = new StackBuilder().setStack(
          new Stack()
              .setDirectory(ROOT)
              .setDependencies(
                  ImmutableList.of(
                      new StackDependency("io.reactivex", "rxjava", "1.0.14"),
                      new StackDependency("io.vertx", "vertx-rx-java", "3.0.0"),
                      new StackDependency("org.hsqldb", "hsqldb", "2.3.3")
                  ))
              .setFiles(ImmutableList.of(
                  new AdditionalFile().setFile(new File("src/test/resources/hello.html")).setTargetDirectory("files")))
      );
      builder.build();
      fail("A conflict should have been detected");
    } catch (IllegalStateException e) {
      assertThat(e)
          .hasMessageContaining("io.reactivex:rxjava:jar:1.0.8")
          .hasMessageContaining("io.vertx:vertx-rx-java:jar:3.0.0")
          .hasMessageContaining("io.reactivex:rxjava:jar:1.0.14");
    }
  }

  @Test
  public void testStackConflictInTheSameStackOnDependenciesInvertedUsingReleases() {
    try {
      StackBuilder builder = new StackBuilder().setStack(
          new Stack()
              .setDirectory(ROOT)
              .setDependencies(
                  ImmutableList.of(
                      new StackDependency("io.vertx", "vertx-rx-java", "3.0.0"),
                      new StackDependency("io.reactivex", "rxjava", "1.0.14"),
                      new StackDependency("org.hsqldb", "hsqldb", "2.3.3")
                  ))
              .setFiles(ImmutableList.of(
                  new AdditionalFile().setFile(new File("src/test/resources/hello.html")).setTargetDirectory("files")))
      );
      builder.build();
      fail("A conflict should have been detected");
    } catch (IllegalStateException e) {
      e.printStackTrace();
      assertThat(e)
          .hasMessageContaining("io.reactivex:rxjava:jar:1.0.8")
          .hasMessageContaining("io.vertx:vertx-rx-java:jar:3.0.0")
          .hasMessageContaining("io.reactivex:rxjava:jar:1.0.14");
    }
  }

  @Test
  public void testStackConflictInTheSameStackOnTransitiveDependencies() {
    // The setup is a bit tricky here. The conflicts is going to happen on SLF4J API as two dependencies are using
    // different version of this artifacts.
    try {
      StackBuilder builder = new StackBuilder().setStack(
          new Stack()
              .setDirectory(ROOT)
              .setDependencies(
                  ImmutableList.of(
                      new StackDependency("uk.co.jemos.podam", "podam", "5.5.1.RELEASE"),
                      new StackDependency("org.apache.logging.log4j", "log4j-slf4j-impl", "2.3")
                  ))
              .setFiles(ImmutableList.of(
                  new AdditionalFile().setFile(new File("src/test/resources/hello.html")).setTargetDirectory("files")))
      );
      builder.build();
      fail("A conflict should have been detected");
    } catch (IllegalStateException e) {
      e.printStackTrace();
      assertThat(e)
          .hasMessageContaining("org.slf4j:slf4j-api:jar:1.7.12")
          .hasMessageContaining("uk.co.jemos.podam:podam:jar:5.5.1.RELEASE")
          .hasMessageContaining("org.slf4j:slf4j-api:jar:1.7.7");
    }
  }
}
