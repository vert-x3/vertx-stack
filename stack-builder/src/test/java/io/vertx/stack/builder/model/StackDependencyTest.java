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

package io.vertx.stack.builder.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackDependencyTest {

  @Test
  public void testCreationFromGAVC() {
    // Jar file without classifier
    StackDependency dependency = new StackDependency().setGACV("io.vertx:vertx-core:3.0.0");
    assertThat(dependency.getGroupId()).isEqualTo("io.vertx");
    assertThat(dependency.getArtifactId()).isEqualTo("vertx-core");
    assertThat(dependency.getVersion()).isEqualTo("3.0.0");
    assertThat(dependency.getClassifier()).isNull();
    assertThat(dependency.getType()).isEqualTo("jar");
    assertThat(dependency.getGACV()).isEqualTo("io.vertx:vertx-core:jar:3.0.0");

    // Zip file with classifier
    dependency = new StackDependency().setGACV("io.vertx:vertx-stack-dist:zip:full:3.0.0");
    assertThat(dependency.getGroupId()).isEqualTo("io.vertx");
    assertThat(dependency.getArtifactId()).isEqualTo("vertx-stack-dist");
    assertThat(dependency.getVersion()).isEqualTo("3.0.0");
    assertThat(dependency.getClassifier()).isEqualTo("full");
    assertThat(dependency.getType()).isEqualTo("zip");
    assertThat(dependency.getGACV()).isEqualTo("io.vertx:vertx-stack-dist:zip:full:3.0.0");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testErroneousCreationFromGACV() {
    new StackDependency().setGACV("io.vertx:vertx-core-no-version");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testErroneousCreationFromGACVTooMuch() {
    new StackDependency().setGACV("io.vertx:vertx-core:3.0.0:zip:full:too_much");
  }

}