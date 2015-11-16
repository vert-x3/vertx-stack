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

package io.vertx.stack.command;

import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import io.vertx.core.spi.launcher.ExecutionContext;
import io.vertx.stack.model.Stack;
import io.vertx.stack.model.StackResolution;
import io.vertx.stack.model.StackResolutionOptions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * The resolve command.
 * <p/>
 * The resolve command maintains the files contained in the `lib` directory of a vert.x stack based on a stack
 * descriptor.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Name("resolve")
@Summary("Resolve the vert.x stack according to the content the stack description.")
@Description("Synchronize the content of a vert.x distribution based on the description given in a 'json' file. From " +
    "the 'VERTX_HOME' directory, launch it with: 'bin/vertx resolve'.")
public class ResolveCommand extends DefaultCommand {

  /**
   * Main method using in the build process to generate the min lib directory.
   * We cannot use the Launcher because the exec:java does not let us look for commands in the classpath and in target
   *
   * @param args the arguments
   */
  public static void main(String[] args) {
    CLI cli = CLIConfigurator.define(ResolveCommand.class);
    ResolveCommand command = new ResolveCommand();
    CommandLine commandLine = cli.parse(Arrays.asList(args));
    CLIConfigurator.inject(commandLine, command);
    command.run();
  }

  private File directory;
  private File descriptor;
  private boolean failOnConflict;
  private String localRepository;
  private List<String> remoteRepositories;
  private String httpProxy;
  private String httpsProxy;

  @Option(longName = "dir")
  @DefaultValue("lib")
  @Description("The directory containing the artifacts composing the stack. Defaults to the './lib' directory.")
  public void setDirectory(File file) {
    this.directory = file.getAbsoluteFile();
  }

  @Option(longName = "stack")
  @DefaultValue("vertx-stack.json")
  @Description("The path to the stack descriptor. Defaults to 'vertx-stack.json'.")
  public void setStackDescriptor(File file) {
    this.descriptor = file;
  }

  @Option(longName = "fail-on-conflict", flag = true)
  @DefaultValue("false")
  @Description("Set whether or not the resolver should fail or conflict or just log a warning. Disabled by default.")
  public void setFailOnConflict(boolean fail) {
    this.failOnConflict = fail;
  }

  @Option(longName = "local-repo")
  @Description("Set the path to the local Maven repository. Defaults to '~/.m2/repository'.")
  public void setLocalRepository(String localRepository) {
    this.localRepository = localRepository;
  }

  @Option(longName = "remote-repo", acceptMultipleValues = true)
  @Description("Set the path to a remote Maven repository. Can be set multiple times.")
  public void setRemoteRepositories(List<String> remoteRepositories) {
    this.remoteRepositories = remoteRepositories;
  }

  @Option(longName = "http-proxy")
  @Description("Set the HTTP proxy address if any.")
  public void setHttpProxy(String p) {
    this.httpProxy = p;
  }

  @Option(longName = "https-proxy")
  @Description("Set the HTTPS proxy address if any.")
  public void setHttpsProxy(String p) {
    this.httpsProxy = p;
  }

  /**
   * Checks whether or not the descriptor file exists. Fails if not.
   *
   * @param ec the execution environment
   * @throws CLIException thrown if the descriptor file does not exist.
   */
  @Override
  public void setUp(ExecutionContext ec) throws CLIException {
    super.setUp(ec);
    if (!descriptor.isFile()) {
      throw new CLIException("The descriptor '" + descriptor.getAbsolutePath() + "' is not a file.");
    }
  }

  /**
   * Executes the command.
   * @throws CLIException if something bad happened during the execution.
   */
  @Override
  public void run() throws CLIException {
    Stack stack = Stack.fromDescriptor(descriptor);
    StackResolutionOptions options = new StackResolutionOptions().setFailOnConflicts(failOnConflict);

    if (localRepository != null) {
      options.setLocalRepository(localRepository);
    }

    if (remoteRepositories != null) {
      options.setRemoteRepositories(remoteRepositories);
    }

    options.setHttpProxy(httpProxy);
    options.setHttpsProxy(httpsProxy);

    StackResolution resolution = new StackResolution(stack, directory, options);
    resolution.resolve();
  }
}
