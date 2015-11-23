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
import io.vertx.core.Launcher;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.spi.launcher.ExecutionContext;
import io.vertx.stack.command.ResolveCommand;
import io.vertx.stack.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DescriptorTest {

  private File root = new File("target/stack");

  @Before
  public void setUp() {
    FileUtils.delete(root);
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !root.exists());
  }

  @Test
  public void testResolutionOfCore() {
    ResolveCommand cmd = new ResolveCommand();
    cmd.setFailOnConflict(false);
    cmd.setDirectory(root.getAbsolutePath());
    cmd.setStackDescriptor(new File("src/test/resources/stacks/core.json").getAbsolutePath());
    cmd.setUp(new ExecutionContext(cmd, new VertxCommandLauncher(), null));
    cmd.run();
    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }

  @Test
  public void testResolutionOfCoreWithVariable() {
    ResolveCommand cmd = new ResolveCommand();
    cmd.setFailOnConflict(false);
    cmd.setDirectory(root.getAbsolutePath());
    cmd.setStackDescriptor(new File("src/test/resources/stacks/core-with-variable.json").getAbsolutePath());
    cmd.setUp(new ExecutionContext(cmd, new VertxCommandLauncher(), null));
    cmd.run();
    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }

  @Test
  public void testResolutionOfCoreUsingSystemVariable() {
    List<String> args = new ArrayList<>();
    args.add("resolve");
    args.add("--dir=" + root.getAbsolutePath());
    args.add("-Dvertx.version=3.1.0");
    args.add(new File("src/test/resources/stacks/core-with-system-variable.json").getAbsolutePath());
    Launcher.main(args.toArray(new String[args.size()]));

    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }

  @Test
  public void testResolutionWithDefaultDescriptor() {
    File defaultStack = new File("vertx-stack.json");
    FileUtils.copyFile(new File("src/test/resources/stacks/core-with-system-variable.json"),
        defaultStack);

    List<String> args = new ArrayList<>();
    args.add("resolve");
    args.add("--dir=" + root.getAbsolutePath());
    args.add("-Dvertx.version=3.1.0");
    Launcher.main(args.toArray(new String[args.size()]));

    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
    defaultStack.delete();
  }

  @Test
  public void testResolutionWithDefaultDescriptorInVertxHome() {
    File home = new File("target/home");
    home.mkdirs();
    System.setProperty("vertx.home", home.getAbsolutePath());

    FileUtils.copyFile(new File("src/test/resources/stacks/core-with-system-variable.json"),
        new File(home, "vertx-stack.json"));

    List<String> args = new ArrayList<>();
    args.add("resolve");
    args.add("-Dvertx.version=3.1.0");
    Launcher.main(args.toArray(new String[args.size()]));

    assertThat(new File(home, "lib/vertx-core-3.1.0.jar")).isFile();
    System.clearProperty("vertx.home");
  }


}
