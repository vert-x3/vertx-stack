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

package io.vertx.stack.utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class LocalArtifact {

  public static final String MAIN_ARTIFACT_CLASSIFIER = "MAIN";
  private final String groupId;
  private final String artifactId;
  private final String version;
  private String packaging;
  private Map<String, File> files = new HashMap<>();
  private LocalArtifact parent;
  private List<LocalDependency> dependencies = new ArrayList<>();
  private List<LocalDependency> dependencyManagement = new ArrayList<>();
  private Map<String, String> properties = new LinkedHashMap<>();
  private boolean inheritGroupId;
  private boolean inheritVersion;

  public LocalArtifact(String groupId, String artifactId, String version) {
    Objects.requireNonNull(groupId);
    Objects.requireNonNull(artifactId);
    Objects.requireNonNull(version);
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public LocalArtifact inheritGroupId() {
    this.inheritGroupId = true;
    return this;
  }

  public LocalArtifact inheritVersion() {
    this.inheritVersion = true;
    return this;
  }

  public LocalArtifact packaging(String type) {
    this.packaging = type;
    return this;
  }

  public LocalArtifact file(File file) {
    this.files.put(MAIN_ARTIFACT_CLASSIFIER, file);
    return this;
  }

  public LocalArtifact file(File file, String classifier) {
    this.files.put(classifier, file);
    return this;
  }

  public LocalArtifact parent(LocalArtifact parent) {
    this.parent = parent;
    return this;
  }

  public LocalArtifact parent(String groupId, String artifactId, String version) {
    this.parent = new LocalArtifact(groupId, artifactId, version);
    return this;
  }

  public LocalArtifact addDependency(LocalDependency dependency) {
    dependencies.add(dependency);
    return this;
  }

  public LocalArtifact addDependencyToDependencyManagement(LocalDependency dependency) {
    dependencyManagement.add(dependency);
    return this;
  }

  public LocalArtifact addProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public File getArtifactDirectory(File root) {
    return new File(root, groupId.replace(".", File.separator) + File.separator + artifactId
        + File.separator + version);
  }

  public File getPomFile(File root) {
    return new File(getArtifactDirectory(root), artifactId + "-" + version + ".pom");
  }

  public File getFile(File root) {
    String ext = FileUtils.getExtension(files.get(MAIN_ARTIFACT_CLASSIFIER));
    return new File(getArtifactDirectory(root), artifactId + "-" + version + "." + ext);
  }

  public File getFile(String classifier, File root) {
    String ext = FileUtils.getExtension(files.get(classifier));
    return new File(getArtifactDirectory(root), artifactId + "-" + version + "-" + classifier + "." + ext);
  }

  public String toPom() {
    StringBuilder builder = new StringBuilder();
    line(builder, 0, "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
        "                      http://maven.apache.org/xsd/maven-4.0.0.xsd\">");

    line(builder, 1, "<modelVersion>4.0.0</modelVersion>");

    // Coordinates
    if (!inheritGroupId) {
      line(builder, 1, "<groupId>" + groupId + "</groupId>");
    }
    line(builder, 1, "<artifactId>" + artifactId + "</artifactId>");
    if (!inheritVersion) {
      line(builder, 1, "<version>" + version + "</version>");
    }

    // Parent if any
    if (parent != null) {
      line(builder, 1, "<parent>");
      line(builder, 2, "<groupId>" + parent.groupId + "</groupId>");
      line(builder, 2, "<artifactId>" + parent.artifactId + "</artifactId>");
      line(builder, 2, "<version>" + parent.version + "</version>");
      line(builder, 1, "</parent>");
    }

    if (packaging != null) {
      line(builder, 1, "<packaging>" + packaging + "</packaging>");
    }

    if (!properties.isEmpty()) {
      line(builder, 1, "<properties>");
      properties.forEach((k, v) -> line(builder, 2, "<" + k + ">" + v + "</" + k + ">"));
      line(builder, 1, "</properties>");
    }

    if (!dependencyManagement.isEmpty()) {
      line(builder, 1, "<dependencyManagement>");
      dependencyManagement.stream().forEach(dep -> builder.append(dep.toDependency(2)));
      line(builder, 1, "</dependencyManagement>");
    }

    if (!dependencies.isEmpty()) {
      line(builder, 1, "<dependencies>");
      dependencies.stream().forEach(dep -> builder.append(dep.toDependency(2)));
      line(builder, 1, "</dependencies>");
    }

    line(builder, 0, "</project>");
    return builder.toString();
  }

  private void line(StringBuilder builder, int indent, String line) {
    builder.append(PomUtils.indentation(indent)).append(line).append("\n");
  }

  public Map<String, File> getFiles() {
    return files;
  }

  public String gav() {
    return groupId + ":" + artifactId + ":" + version;
  }

  public LocalArtifact generateMainArtifact() {
    try {
      File tmp = File.createTempFile("local-artifact-" + artifactId, ".txt");
      FileUtils.write(tmp, gav());
      return file(tmp).packaging("txt");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
