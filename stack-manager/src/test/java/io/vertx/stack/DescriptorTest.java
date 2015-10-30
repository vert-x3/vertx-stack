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

import io.vertx.core.Launcher;
import io.vertx.stack.command.ResolveCommand;
import io.vertx.stack.model.StackResolutionOptions;
import io.vertx.stack.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DescriptorTest {

  private File root = new File("target/stack");

  @Before
  public void setUp() {
    FileUtils.delete(root);
    // TODO wait until we are sure the directory is deleted
  }

  @Test
  public void testResolutionOfCore() {
    ResolveCommand cmd = new ResolveCommand();
    cmd.setFailOnConflict(false);
    cmd.setDirectory(root);
    cmd.setStackDescriptor(new File("src/test/resources/stacks/core.yaml"));
    cmd.run();
    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }

  @Test
  public void testResolutionOfCoreWithVariable() {
    ResolveCommand cmd = new ResolveCommand();
    cmd.setFailOnConflict(false);
    cmd.setDirectory(root);
    cmd.setStackDescriptor(new File("src/test/resources/stacks/core-with-variable.yaml"));
    cmd.run();
    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }

  @Test
  public void testResolutionOfCoreUsingSystemVariable() {
    List<String> args = new ArrayList<>();
    args.add("resolve");
    args.add("--dir=" + root.getAbsolutePath());
    args.add("-Dvertx.version=3.1.0");
    args.add("--stack=" + new File("src/test/resources/stacks/core-with-system-variable.yaml").getAbsolutePath());
    Launcher.main(args.toArray(new String[args.size()]));

    assertThat(new File(root, "vertx-core-3.1.0.jar")).isFile();
  }


}
