package io.vertx.stack.builder.mojo;

import io.vertx.stack.builder.StackBuilder;
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
@Mojo(name = "build",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildMojo extends AbstractMojo {

  @Parameter(required = true, defaultValue = "${descriptor}")
  private File descriptor;

  @Override
  public void execute() throws MojoExecutionException {
    if (!descriptor.isFile()) {
      throw new MojoExecutionException("The descriptor must exist - "
          + descriptor.getAbsolutePath() + " does not exist");
    }

    getLog().info("Building stack from " + descriptor.getAbsolutePath());
    StackBuilder builder = new StackBuilder();
    builder
        .fromDescriptor(descriptor)
        .build();
  }
}
