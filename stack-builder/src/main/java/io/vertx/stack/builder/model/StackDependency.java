package io.vertx.stack.builder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Represents a dependency added to the stack.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackDependency extends Dependency {

  private boolean ignoreTransitive = false;

  public StackDependency() {
    super();
  }

  public StackDependency(String groupId, String artifactId, String version) {
    super();
    setGroupId(groupId);
    setArtifactId(artifactId);
    setVersion(version);
  }

  /**
   * @return whether or not the transitive dependencies need to be resolved for this dependency.
   */
  public boolean isIgnoreTransitive() {
    return ignoreTransitive;
  }


  /**
   * Enables or disables the resolution of transitive dependencies to this dependency.
   *
   * @param ignoreTransitive whether or not transitive dependencies needs to be resolved
   * @return the current {@link StackDependency}
   */
  public StackDependency setIgnoreTransitive(boolean ignoreTransitive) {
    this.ignoreTransitive = ignoreTransitive;
    return this;
  }

  /**
   * Configures this dependency using the GAVC string. It uses the Maven format.
   *
   * @param gacv the gacv
   * @return the current {@link StackDependency}
   */
  public StackDependency setGACV(String gacv) {
    DefaultArtifact artifact = new DefaultArtifact(gacv);
    setGroupId(artifact.getGroupId());
    setArtifactId(artifact.getArtifactId());
    setVersion(artifact.getVersion());
    setType(artifact.getExtension());
    if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
      setClassifier(artifact.getClassifier());
    }
    return this;
  }

  /**
   * @return the GACV representation of the dependency.
   */
  public String getGACV() {
    return getManagementKey() + ":" + getVersion();
  }

  /**
   * Not supported property, as the concept of 'optional' does not make sense when building a stack.
   *
   * @param optional ignored
   */
  @Override
  @JsonIgnore
  public void setOptional(boolean optional) {
    throw new UnsupportedOperationException("You cannot add an optional dependency to a stack - optional does not " +
        "make sense in this case");
  }
}
