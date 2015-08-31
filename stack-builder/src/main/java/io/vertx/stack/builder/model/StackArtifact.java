package io.vertx.stack.builder.model;

import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an artifact resolved and part of the stack. This structure is made to keep a track of the origin or
 * each artifact.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackArtifact {


  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String extension;

  private File file;

  private List<StackArtifact> dependencies = new ArrayList<>();

  public StackArtifact() {
    super();
  }

  public StackArtifact(Artifact artifact) {
    this();
    setGroupId(artifact.getGroupId());
    setArtifactId(artifact.getArtifactId());
    setVersion(artifact.getVersion());
    setExtension(artifact.getExtension());
    if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
      setClassifier(artifact.getClassifier());
    }
  }

  public String getArtifactId() {
    return artifactId;
  }

  public StackArtifact setArtifactId(String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  public String getClassifier() {
    return classifier;
  }

  public StackArtifact setClassifier(String classifier) {
    this.classifier = classifier;
    return this;
  }

  public String getExtension() {
    return extension;
  }

  public StackArtifact setExtension(String extension) {
    this.extension = extension;
    return this;
  }

  public File getFile() {
    return file;
  }

  public StackArtifact setFile(File file) {
    this.file = file;
    return this;
  }

  public String getGroupId() {
    return groupId;
  }

  public StackArtifact setGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public List<StackArtifact> getDependencies() {
    return dependencies;
  }

  public StackArtifact setDependencies(List<StackArtifact> dependencies) {
    this.dependencies = dependencies;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public StackArtifact setVersion(String version) {
    this.version = version;
    return this;
  }

  public boolean matches(Artifact artifact) {
    return this.getGroupId().equals(artifact.getGroupId())
        && this.getArtifactId().equals(artifact.getArtifactId())
        && this.getVersion().equals(artifact.getVersion())
        && compareClassifier(this.classifier, artifact.getClassifier())
        && (this.getExtension() != null && this.getExtension().equals(artifact.getExtension()));
  }

  public boolean matchesPartially(Artifact artifact) {
    return this.getGroupId().equals(artifact.getGroupId())
        && this.getArtifactId().equals(artifact.getArtifactId())
        && compareClassifier(this.classifier, artifact.getClassifier())
        && (this.getExtension() != null && this.getExtension().equals(artifact.getExtension()));
  }

  private boolean compareClassifier(String classifier1, String classifier2) {
    if (classifier1 == null) {
      return classifier2 == null  || classifier2.isEmpty();
    }
    if (classifier2 == null) {
      return classifier1.isEmpty();
    }
    return classifier1.equals(classifier2);
  }

  public String toString() {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append(getGroupId());
    buffer.append(':').append(getArtifactId());
    buffer.append(':').append(getExtension());
    if (getClassifier() != null  && getClassifier().length() > 0) {
      buffer.append(':').append(getClassifier());
    }
    buffer.append(':').append(getVersion());
    return buffer.toString();
  }
}
