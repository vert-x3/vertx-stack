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
import io.vertx.stack.utils.Home;

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

  private String directory;
  private String descriptor;
  private boolean failOnConflict;
  private String localRepository;
  private List<String> remoteRepositories;
  private String httpProxy;
  private String httpsProxy;
  private boolean disableCache;
  private boolean disableCacheForSnapshots;
  private File cacheFile;

  @Option(longName = "dir")
  @Description("The directory containing the artifacts composing the stack. Defaults to the '$VERTX_HOME/lib' " +
      "directory, if $VERTX_HOME is set, './lib' otherwise.")
  public void setDirectory(String file) {
    this.directory = file;
  }

  @Argument(index = 0, required = false, argName = "stack-descriptor")
  @DefaultValue("vertx-stack.json")
  @Description("The path to the stack descriptor. Defaults to '$VERTX_HOME/vertx-stack.json', if $VERTX_HOME is set, " +
      "'./vertx-stack.json' otherwise.")
  public void setStackDescriptor(String file) {
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

  @Option(longName = "no-cache", flag = true)
  @Description("Disable the resolver cache")
  public void setDisableCache(boolean disableCache) {
    this.disableCache = disableCache;
  }

  @Option(longName = "no-cache-for-snapshots", flag = true)
  @Description("Disable the caching of snapshot resolution")
  public void setDisableCacheForSnapshots(boolean disableCache) {
    this.disableCacheForSnapshots = disableCache;
  }

  @Option(longName = "cache-file")
  @Hidden
  public void setCacheLocation(File cache) {
    this.cacheFile = cache;
  }

  /**
   * Executes the command.
   * @throws CLIException if something bad happened during the execution.
   */
  @Override
  public void run() throws CLIException {
    File descriptorFile = new File(descriptor);
    if (! descriptorFile.isFile()) {
      // Try with vert.x home
      if (Home.getVertxHome() != null) {
        descriptorFile = new File(Home.getVertxHome(), descriptor);
      }
    }

    if (! descriptorFile.isFile()) {
      String message = "Cannot find the stack descriptor. Have been tried: \n\t - ./" + descriptorFile;
      if (Home.getVertxHome() != null) {
        message += "\n\t - " + descriptorFile.getAbsolutePath();
      }
      throw new CLIException(message);
    }

    File lib;
    if (directory == null) {
      if (Home.getVertxHome() != null) {
        lib = new File(Home.getVertxHome(), "lib");
      } else {
        lib = new File("lib");
      }
    } else {
      lib = new File(directory);
    }

    out().println("lib directory set to: " + lib.getAbsolutePath());

    Stack stack = Stack.fromDescriptor(descriptorFile);
    StackResolutionOptions options = new StackResolutionOptions()
        .setFailOnConflicts(failOnConflict)
        .setCacheDisabled(disableCache)
        .setCacheDisabledForSnapshots(disableCacheForSnapshots)
        .setCacheFile(cacheFile);

    if (localRepository != null) {
      options.setLocalRepository(localRepository);
    }

    if (remoteRepositories != null) {
      options.setRemoteRepositories(remoteRepositories);
    }

    options.setHttpProxy(httpProxy);
    options.setHttpsProxy(httpsProxy);

    StackResolution resolution = new StackResolution(stack, lib, options);
    resolution.resolve();
  }

}
