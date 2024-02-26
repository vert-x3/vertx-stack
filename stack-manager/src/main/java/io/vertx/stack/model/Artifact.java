package io.vertx.stack.model;

import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class Artifact extends AbstractArtifact {

  private org.eclipse.aether.artifact.Artifact internal;
  private final Artifact via;

  public Artifact(String coordinates) {
    this(coordinates, null);
  }

  public Artifact(String coordinates, Artifact via) {
    this(new DefaultArtifact(coordinates), via);
  }

  public Artifact(String groupId, String artifactId, String classifier, String extension, String version) {
    this(groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version, null);
  }

  public Artifact(String groupId, String artifactId, String extension, String version) {
    this(groupId + ":" + artifactId + ":" + extension + ":" + version, null);
  }

  public Artifact(org.eclipse.aether.artifact.Artifact fromArtifact, Artifact via) {
    internal = fromArtifact;
    this.via = via;
  }

  @Override
  public String getGroupId() {
    return internal.getGroupId();
  }

  @Override
  public String getArtifactId() {
    return internal.getArtifactId();
  }

  @Override
  public String getVersion() {
    return internal.getVersion();
  }

  @Override
  public String getClassifier() {
    return internal.getClassifier();
  }

  @Override
  public String getExtension() {
    return internal.getExtension();
  }

  @Override
  public Artifact setFile(File file) {
    internal = internal.setFile(file);
    return this;
  }

  @Override
  public File getFile() {
    return internal.getFile();
  }

  @Override
  public Map<String, String> getProperties() {
    return internal.getProperties();
  }

  public Artifact getVia() {
    return this.via;
  }

  public String getCoordinates() {
    return coordinates(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Artifact artifact = (Artifact) o;
    return Objects.equals(internal, artifact.internal) && Objects.equals(via, artifact.via);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), internal, via);
  }

  private static String coordinates(org.eclipse.aether.artifact.Artifact artifact) {
    // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
    return artifact.getGroupId() + ":" + artifact.getArtifactId()
      + ":" + artifact.getExtension()
      + (artifact.getClassifier().isEmpty() ? "" : ":" + artifact.getClassifier())
      + ":" + artifact.getVersion();
  }
}
