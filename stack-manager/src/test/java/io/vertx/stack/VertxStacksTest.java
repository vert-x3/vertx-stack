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

package io.vertx.stack;

import com.jayway.awaitility.Awaitility;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.stack.model.Dependency;
import io.vertx.stack.model.Stack;
import io.vertx.stack.model.StackResolution;
import io.vertx.stack.model.StackResolutionOptions;
import io.vertx.stack.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxStacksTest {

  private static final List<String> BASE = Arrays.asList(
      "io.vertx:vertx-http-service-factory:jar",
      "io.vertx:vertx-dropwizard-metrics:jar",
      "io.vertx:vertx-maven-service-factory:jar",
      "io.vertx:vertx-reactive-streams:jar",
      "io.vertx:vertx-rx-java:jar",
      "io.vertx:vertx-service-factory:jar"
  );


  private File root = new File("target/stack");

  @Before
  public void setUp() {
    FileUtils.delete(root);
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !root.exists());
    String vertxVersion = new VersionCommand().getVersion();
    assertThat(vertxVersion).isNotEmpty();
    System.setProperty("vertx.version", vertxVersion);
  }

  @After
  public void tearDown() {
    System.clearProperty("vertx.version");
  }

  @Test
  public void testResolutionOfMinSwitchToBaseAndRevertToMin() {
    Stack stack = Stack.fromDescriptor(new File("target/vertx-stack/vertx-stack.json"));

    // Min stack
    StackResolution resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true));
    Map<String, File> resolved = resolution.resolve();
    hasKeysStartingBy(resolved, "io.vertx:vertx-core:jar:").ensureThatAllFilesExist(resolved);
    int minArtifacts = resolved.size();

    // Base
    setUpBaseStack(stack);
    resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true));
    resolved = resolution.resolve();
    for (String b : BASE) {
      hasKeysStartingBy(resolved, b);
    }
    hasKeysStartingBy(resolved, "io.vertx:vertx-core:jar:").ensureThatAllFilesExist(resolved);

    tearDownBaseStack(stack);
    resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true));
    resolved = resolution.resolve();
    for (String b : BASE) {
      doNotHaveKeysStartingBy(resolved, b);
    }
    hasKeysStartingBy(resolved, "io.vertx:vertx-core:jar:").ensureThatAllFilesExist(resolved);
    assertThat(resolved.size()).isEqualTo(minArtifacts);
  }

  /**
   * This tests checks that all our dependencies converge to the same version.
   */
  @Test
  public void testConvergence() {
    // Prepare the stack - use full stack, include everything
    Stack stack = Stack.fromDescriptor(new File("target/vertx-stack/vertx-stack-full.json"));
    // Stack stack = new Stack().addDependency(new Dependency("io.vertx", "vertx-core", "4.5.6"));
    stack.getDependencies()
        .forEach(d -> d.setIncluded(true));

    StackResolution resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true).setCacheDisabled(true));
    Map<String, File> resolved = resolution.resolve(gav -> {
      // Check we don't have loggers in the distrib
      return !gav.startsWith("log4j:log4j") && !gav.startsWith("org.apache.logging.log4j:");
    });
    assertThat(resolved).isNotEmpty();
  }

  /**
   * This tests checks that all our dependencies converge to the same version. This test check the Scala stack.
   */
//  @Test
//  public void testScalaConvergence() {
//    // Prepare the stack - use full stack, include everything
//    Stack stack = Stack.fromDescriptor(new File("target/vertx-stack/vertx-stack-scala.json"));
//    stack.getDependencies().stream()
//      .forEach(d -> d.setIncluded(true));
//
//    StackResolution resolution = new StackResolution(stack, root,
//      new StackResolutionOptions().setFailOnConflicts(true).setCacheDisabled(true));
//    Map<String, File> resolved = resolution.resolve();
//    assertThat(resolved).isNotEmpty();
//  }

  @Test
  public void testTheResolutionOfTheWebStack() {
    Stack stack = Stack.fromDescriptor(new File("src/test/resources/stacks/vertx-web-stack.json"));

    StackResolution resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true));
    Map<String, File> resolved = resolution.resolve();
    assertThat(resolved).isNotEmpty();
  }

  @Test
  public void testTheCoreWithoutNettyBufferStack() {
    Stack stack = Stack.fromDescriptor(new File("src/test/resources/stacks/vertx-core-only.json"));

    StackResolution resolution = new StackResolution(stack, root,
        new StackResolutionOptions().setFailOnConflicts(true));
    Map<String, File> resolved = resolution.resolve();
    assertThat(resolved).isNotEmpty().doesNotContainKey("io.netty:netty-buffer");
  }

  private void setUpBaseStack(Stack stack) {
    stack.getDependencies()
        .filter(dependency -> wasPartOfTheBaseStack(dependency.getManagementKey()))
        .forEach(dependency -> dependency.setIncluded(true));
  }

  private void tearDownBaseStack(Stack stack) {
    stack.getDependencies()
        .filter(dependency -> wasPartOfTheBaseStack(dependency.getManagementKey()))
        .forEach(dependency -> dependency.setIncluded(false));
  }

  private boolean wasPartOfTheBaseStack(String gacv) {
    return BASE.contains(gacv);
  }

  private VertxStacksTest hasKeysStartingBy(Map<String, File> files, String k) {
    if (!containsKeyStartingByPrefix(files.keySet(), k)) {
      fail("Expected to have a key starting with '" + k + "' in " + files.keySet());
      return this;
    }
    return this;
  }

  private VertxStacksTest doNotHaveKeysStartingBy(Map<String, File> files, String k) {
    if (containsKeyStartingByPrefix(files.keySet(), k)) {
      fail("Expected to not have a key starting with '" + k + "' in " + files.keySet());
      return this;
    }
    return this;
  }

  private VertxStacksTest ensureThatAllFilesExist(Map<String, File> files) {
    for (File f : files.values()) {
      assertThat(f).isFile();
    }
    return this;
  }

  private boolean containsKeyStartingByPrefix(Set<String> keys, String prefix) {
    for (String k : keys) {
      if (k.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

}
