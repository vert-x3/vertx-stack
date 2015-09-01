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

package io.vertx.stack.builder.mojo;

import io.vertx.stack.builder.StackBuilder;
import io.vertx.stack.builder.StackBuilderOptions;
import io.vertx.stack.builder.model.Stack;
import io.vertx.stack.builder.model.StackDependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Mojo(name = "add",
    requiresProject = false)
public class AddMojo extends AbstractMojo {

  @Parameter(required = true, defaultValue = "${stack.directory}")
  private File directory;

  @Parameter(required = true, defaultValue = "${artifact}")
  private String artifact;

  @Parameter(defaultValue = "${ignoreTransitive}")
  private boolean ignoreTransitive = false;

  @Parameter(defaultValue = "${ignoreConflicts}")
  private boolean ignoreConflicts = false;

  @Override
  public void execute() throws MojoExecutionException {
    if (!directory.isDirectory()) {
      throw new MojoExecutionException("The directory must exist - "
          + directory.getAbsolutePath() + " does not exist");
    }

    getLog().info("Adding " + artifact + " to stack");
    StackBuilder builder = new StackBuilder();
    builder
        .setStack(new Stack()
            .setDirectory(directory)
            .addDependency(new StackDependency(artifact).setIgnoreTransitive(ignoreTransitive)))
        .build(new StackBuilderOptions().setIgnoreArtifactConflict(ignoreConflicts));
  }
}
