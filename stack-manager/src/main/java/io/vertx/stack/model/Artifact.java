package io.vertx.stack.model;

import org.eclipse.aether.artifact.AbstractArtifact;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Artifact extends AbstractArtifact {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String extension;
  private final File file;
  private final Map<String, String> properties;
  private final Artifact via;
  private static final Pattern coordinatesPattern =
    Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

  public Artifact(String coordinates) {
    this(coordinates, null);
  }

  public Artifact(String groupId, String artifactId, String classifier, String extension, String version, Artifact via) {
    this(groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version, via);
  }

  public Artifact(String groupId, String artifactId, String extension, String version, Artifact via) {
    this(groupId + ":" + artifactId + ":" + extension + ":" + version, via);
  }

  public Artifact(org.eclipse.aether.artifact.Artifact fromArtifact, Artifact via) {
    this(coordinates(fromArtifact), via, fromArtifact.getFile(), fromArtifact.getProperties());
  }

  public Artifact(String coordinates, Artifact viaArtifact) {
    this(coordinates, viaArtifact, null, new HashMap<>());
  }

  private Artifact(String coordinates, Artifact viaArtifact, File artifactFile, Map<String, String> props) {
    Matcher m = coordinatesPattern.matcher(coordinates);

    if (!m.matches()) {
      throw new IllegalArgumentException("Bad artifact coordinates " + coordinates
        + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }

    groupId = m.group(1);
    artifactId = m.group(2);
    extension = defaultIfEmpty(m.group(4), "jar");
    classifier = defaultIfEmpty(m.group(6), "");
    version = m.group(7);
    file = artifactFile;
    properties = copyProperties(props);
    via = viaArtifact;
  }

  private String defaultIfEmpty(String value, String defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    } else {
      return value;
    }
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getClassifier() {
    return classifier;
  }

  @Override
  public String getExtension() {
    return extension;
  }

  @Override
  public Artifact setFile(File file) {
    if (Objects.equals(this.file, file)) {
      return this;
    } else {
      return new Artifact(coordinates(this), via, file, properties);
    }
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public Artifact setProperties(Map<String, String> properties) {
    if (Objects.equals(this.properties, properties)) {
      return this;
    } else {
      return new Artifact(coordinates(this), via, file, copyProperties(properties));
    }
  }

  public Artifact getVia() {
    return via;
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
    return Objects.equals(groupId, artifact.groupId)
      && Objects.equals(artifactId, artifact.artifactId)
      && Objects.equals(version, artifact.version)
      && Objects.equals(classifier, artifact.classifier)
      && Objects.equals(extension, artifact.extension)
      && Objects.equals(file, artifact.file)
      && Objects.equals(properties, artifact.properties)
      && Objects.equals(via, artifact.via);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), groupId, artifactId, version, classifier, extension, file, properties, via);
  }

  private static String coordinates(org.eclipse.aether.artifact.Artifact artifact) {
    // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
    return artifact.getGroupId() + ":" + artifact.getArtifactId()
      + ":" + artifact.getExtension()
      + (artifact.getClassifier().isEmpty() ? "" : ":" + artifact.getClassifier())
      + ":" + artifact.getVersion();
  }
}
