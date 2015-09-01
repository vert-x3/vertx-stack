package io.vertx.stack.builder;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackBuilderOptions {

  private boolean ignoreArtifactConflict = false;

  public boolean isIgnoreArtifactConflict() {
    return ignoreArtifactConflict;
  }

  public StackBuilderOptions setIgnoreArtifactConflict(boolean ignoreArtifactConflict) {
    this.ignoreArtifactConflict = ignoreArtifactConflict;
    return this;
  }
}
