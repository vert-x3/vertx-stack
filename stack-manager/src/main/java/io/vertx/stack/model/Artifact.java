package io.vertx.stack.model;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.util.*;

public class Artifact extends AbstractArtifact {

  private org.eclipse.aether.artifact.Artifact internal;
  private HashSet<Artifact> via;
  private static final Map<String, Artifact> globalArtifacts = new HashMap<>();

  /**
   * Use {@link #artifact(String)} or {@link #artifact(String, Artifact...)}.
   */
  private Artifact() {
    // use static artifact(String) or artifact(String, Artifact...) methods
  }

  private Artifact(String coordinates, Artifact... via) {
    this(new DefaultArtifact(coordinates), via);
  }

  private Artifact(org.eclipse.aether.artifact.Artifact fromArtifact, Artifact... via) {
    internal = fromArtifact;
    this.via = new HashSet<>(Arrays.asList(via));
  }

  public static Artifact artifact(String coordinates) {
    return artifact(coordinates, new Artifact[]{});
  }

  public static Artifact artifact(String groupId, String artifactId, String classifier, String extension, String version, Artifact... via) {
    return artifact(groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version, via);
  }

  public static Artifact artifact(String groupId, String artifactId, String extension, String version, Artifact... via) {
    return artifact(groupId + ":" + artifactId + ":" + extension + ":" + version, via);
  }

  public static Artifact artifact(String coordinates, Artifact... via) {
    return globalArtifacts.merge(coordinates, new Artifact(coordinates, via), (a1, a2) -> a1.addVia(a2.via));
  }

  /**
   * Add more artifacts pointing to this.
   */
  public void addVia(Artifact... moreVia) {
    addVia(Arrays.asList(moreVia));
  }

  /**
   * Add more artifacts pointing to this.
   */
  public Artifact addVia(Collection<Artifact> moreVia) {
    this.via.addAll(moreVia);
    return this;
  }

  /**
   * Renders the chain of artifacts that led to this artifact as String.
   */
  public static String renderChain(Artifact fromArtifact) {
    return renderChain(fromArtifact, 0, new ArrayList<>());
  }

  private static String renderChain(Artifact fromArtifact, int level, List<String> acc) {
    Optional<Artifact> maybeVia = fromArtifact.via.stream().findFirst();
    if (maybeVia.isPresent()) {
      Artifact via = maybeVia.get();
      acc.add(0, fromArtifact.getCoordinates());
      return renderChain(via, level + 1, acc);
    } else {
      StringBuilder chainBuilder = new StringBuilder(fromArtifact.getCoordinates());
      for (int i = 0; i < acc.size(); i++) {
        String indent = StringUtils.repeat("\t", i + 1);
        chainBuilder
          .append("\n")
          .append(indent).append("\\-- ").append(acc.get(i));
      }
      return chainBuilder.toString();
    }
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

  /**
   * @return the artifact coordinates in the form {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
   */
  public String getCoordinates() {
    // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
    return getGroupId() + ":" + getArtifactId()
      + ":" + getExtension()
      + (getClassifier().isEmpty() ? "" : ":" + getClassifier())
      + ":" + getVersion();
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
}
