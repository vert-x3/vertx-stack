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

package io.vertx.stack.builder.utils;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ArtifactsTest {

  @Test
  public void testIsSnapshot() {
    Artifact artifact
        = new DefaultArtifact("vertx.io", "vertx-auth-shiro", "jar", "3.1.0-SNAPSHOT");

    String fileName = "vertx-auth-shiro-3.1.0-20150825.122952-106.jar";
    assertThat(Artifacts.isSnapshot(artifact, fileName)).isTrue();
    assertThat(Artifacts.getSnapshotVersion(artifact, fileName)).isEqualTo("3.1.0-SNAPSHOT");

    fileName = "vertx-auth-shiro-3.1.0.jar";
    assertThat(Artifacts.isSnapshot(artifact, fileName)).isFalse();
    assertThat(Artifacts.getSnapshotVersion(artifact, fileName)).isNull();

    fileName = "vertx-auth-shiro-3.1.0-full.jar";
    Artifact artifactWithClassifier =
        new DefaultArtifact("vertx.io", "vertx-auth-shiro", "full", "jar",
            "3.1.0-SNAPSHOT");
    assertThat(Artifacts.isSnapshot(artifactWithClassifier, fileName)).isFalse();

    fileName = "vertx-auth-shiro-3.1.0-SNAPSHOT.jar";
    assertThat(Artifacts.isSnapshot(artifact, fileName)).isTrue();
    assertThat(Artifacts.getSnapshotVersion(artifact, fileName)).isEqualTo("3.1.0-SNAPSHOT");

    fileName = "vertx-auth-shiro-3.1.0-SNAPSHOT-full.jar";
    assertThat(Artifacts.isSnapshot(artifactWithClassifier, fileName)).isTrue();
    assertThat(Artifacts.getSnapshotVersion(artifactWithClassifier, fileName)).isEqualTo("3.1.0-SNAPSHOT");

    fileName = "vertx-auth-shiro-3.1.0-20150825.122952-106-full.jar";
    assertThat(Artifacts.isSnapshot(artifactWithClassifier, fileName)).isTrue();
    assertThat(Artifacts.getSnapshotVersion(artifactWithClassifier, fileName)).isEqualTo("3.1.0-SNAPSHOT");
  }

  @Test
  public void testMatchesWhenTheArtifactIdIsThePrefix() {
    Artifact artifact
        = new DefaultArtifact("vertx.io", "vertx-auth", "jar", "3.1.0-SNAPSHOT");
    assertThat(
        Artifacts.matchesArtifact(artifact, "vertx-auth-shiro-3.1.0-SNAPSHOT.jar"))
        .isFalse();
  }

  @Test
  public void testMatchesWhenItsTheSameArtifactInDifferentVersions() {
    Artifact artifact = new DefaultArtifact("org.hsqldb", "hsqldb", "jar", "2.3.2");
    assertThat(Artifacts.matchesArtifact(artifact, "hsqldb-2.3.3.jar")).isTrue();
  }



}