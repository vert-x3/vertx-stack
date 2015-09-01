package io.vertx.stack.builder.mojo;

import io.vertx.stack.builder.StackBuilder;
import io.vertx.stack.builder.model.Stack;
import io.vertx.stack.builder.model.StackDependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Mojo(name = "remove",
    requiresProject = false)
public class RemoveMojo extends AbstractMojo {

  @Parameter(required = true, defaultValue = "${stack.directory}")
  private File directory;

  @Parameter(required = true, defaultValue = "${artifact}")
  private String artifact;

  @Override
  public void execute() throws MojoExecutionException {
    if (!directory.isDirectory()) {
      throw new MojoExecutionException("The directory must exist - "
          + directory.getAbsolutePath() + " does not exist");
    }

    getLog().info("Removing " + artifact + " from stack");
    StackBuilder builder = new StackBuilder();
    builder
        .setStack(new Stack()
        .setDirectory(directory))
        .build();
    builder.remove(artifact);
  }
}
