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

package io.vertx.stack.model;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.stack.resolver.Resolver;
import io.vertx.stack.utils.Actions;
import io.vertx.stack.utils.Cache;
import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Object responsible for resolving a stack. This object is stateful, and must be used only for a single resolution.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackResolution {

  private final static Logger LOGGER = LoggerFactory.getLogger("Stack Resolution");

  private final File directory;
  private final Stack stack;

  private final Map<String, String> selectedVersions = new LinkedHashMap<>();
  private final Map<String, ResolvedArtifact> selectedArtifacts = new LinkedHashMap<>();

  /**
   * Map keeping a trace from who has resolved which version. It imrpvoes the reporting when a conflict has been
   * detected.
   */
  private final Map<String, List<String>> traces = new LinkedHashMap<>();

  private final StackResolutionOptions options;
  private Resolver resolver;

  private final Cache cache;

  /**
   * Creates an instance of {@link StackResolution}.
   *
   * @param stack     the stack
   * @param directory the output directory, created if not existing
   * @param options   the stack resolution options
   */
  public StackResolution(Stack stack, File directory, StackResolutionOptions options) {
    Objects.requireNonNull(stack);
    Objects.requireNonNull(options);
    Objects.requireNonNull(directory);
    this.stack = stack;
    this.options = options;
    this.directory = directory;
    this.cache = new Cache(options.isCacheDisabled(), options.isCacheDisabledForSnapshots(), options.getCacheFile());
  }

  /**
   * Resolves the stack.
   *
   * @return the map artifact's management key - file composing the stack
   */
  public Map<String, File> resolve() {
    traces.clear();
    selectedVersions.clear();
    init();
    stack.getDependencies().forEach(this::resolve);
    List<Actions.Action> chain = computeChainOfActions();

    chain.forEach(Actions.Action::execute);

    Map<String, File> resolved = new LinkedHashMap<>();
    for (ResolvedArtifact artifact : selectedArtifacts.values()) {
      Path source = artifact.artifact.getFile().toPath();
      Path output = directory.toPath().resolve(source.getFileName());
      resolved.put(artifact.artifact.toString(), output.toFile());
    }

    return resolved;
  }

  public Map<String, File> resolve(Predicate<String> validator) {
    traces.clear();
    selectedVersions.clear();
    init();
    stack.getDependencies().forEach(this::resolve);
    List<Actions.Action> chain = computeChainOfActions();

    chain.forEach(Actions.Action::execute);

    Map<String, File> resolved = new LinkedHashMap<>();
    for (ResolvedArtifact artifact : selectedArtifacts.values()) {
      String gav = artifact.artifact.toString();
      if (!validator.test(gav)) {
        throw new IllegalStateException("Invalid artifact " + gav + ", used by " + artifact.getUsages());
      }
      Path source = artifact.artifact.getFile().toPath();
      Path output = directory.toPath().resolve(source.getFileName());
      resolved.put(gav, output.toFile());
    }

    return resolved;
  }

  private void init() {
    if (!directory.isDirectory()) {
      LOGGER.info("Creating directory " + directory.getAbsolutePath());
      boolean mkdirs = directory.mkdirs();
      LOGGER.debug("Directory created: " + mkdirs);
    }
    stack.applyFiltering();
    stack.getDependencies().filter(Dependency::isIncluded).forEach(
      dependency -> selectedVersions.put(dependency.getManagementKey(), dependency.getVersion()));
    resolver = Resolver.create(options);
  }

  private List<Actions.Action> computeChainOfActions() {
    File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
    if (files == null) {
      throw new IllegalStateException("Unable to read from the file system");
    }

    Map<String, Boolean> marks = new HashMap<>();
    for (File file : files) {
      marks.put(file.getName(), false);
    }

    List<Actions.Action> chain = new ArrayList<>();

    selectedArtifacts.forEach((key, artifact) -> {
      String fileName = artifact.getArtifact().getFile().getName();

      if (marks.containsKey(fileName)) {
        // Mark the file.
        marks.put(fileName, true);
        chain.add(Actions.skip(artifact.getArtifact()));
      } else {
        chain.add(Actions.copy(artifact.getArtifact(), directory));
      }
    });

    // Schedule the deletion of all non-marked file.
    marks.forEach((fileName, mark) -> {
      if (!mark && !fileName.startsWith("vertx-stack-manager-")) { // Do not delete me
        chain.add(Actions.remove(new File(directory, fileName)));
      }
    });

    return chain;
  }

  private void resolve(Dependency dependency) {
    List<io.vertx.stack.model.Artifact> list;
    if (dependency.isIncluded()) {
      list = cache.get(dependency.getGACV(), dependency.getResolutionOptions());
      // artifacts = cache.get(dependency.getGACV(), dependency.getResolutionOptions());
      if (list == null || list.isEmpty()) {
        // list = resolver.resolve(dependency.getGACV(), dependency.getResolutionOptions());
        list = resolver.resolveTree(dependency.getGACV(), dependency.getResolutionOptions());
        cache.put(dependency.getGACV(), dependency.getResolutionOptions(), list);
        cache.writeCacheOnFile();
      } else {
        LOGGER.info("Dependency " + dependency + " loaded from cache");
      }
    } else {
      return;
    }

    if (list == null || list.isEmpty()) {
      throw new IllegalArgumentException("Cannot resolve " + dependency);
    }

    LOGGER.debug("Artifacts resolved for " + dependency.getGACV() + " : "
      + list.stream().map(Object::toString).collect(Collectors.toList()));

    list.forEach(artifact -> {
      String gaec = getManagementKey(artifact);
      String version = selectedVersions.get(gaec);
      if (version == null || version.equalsIgnoreCase(artifact.getBaseVersion())) {
        selectedVersions.put(gaec, artifact.getBaseVersion());
        keepATrace(dependency, artifact);
      } else {
        List<String> trace = traces.get(gaec + ":" + version);
        if (options.isFailOnConflicts()) {
          throw new DependencyConflictException(gaec, version, trace, dependency.getGACV(), artifact);
        }
      }
      addSelectedArtifact(dependency, artifact, version);
    });
  }

  /**
   * Keep a trace of the resolution.
   *
   * @param dependency the dependency having triggered the resolution
   * @param artifact   the resolved artifact
   */
  private void keepATrace(Dependency dependency, Artifact artifact) {
    String traceId = getManagementKey(artifact) + ":" + artifact.getBaseVersion();
    List<String> deps = traces.computeIfAbsent(traceId, k -> new ArrayList<>());
    deps.add(dependency.getGACV());
  }

  private void addSelectedArtifact(Dependency dependency, Artifact artifact, String version) {
    String key = getManagementKey(artifact);
    ResolvedArtifact resolved = selectedArtifacts.get(key);
    if (resolved != null) {
      resolved.addUsage(getManagementKey(dependency));
    } else {
      selectedArtifacts.put(key,
        new ResolvedArtifact().addUsage(getManagementKey(dependency))
          .setSelectedVersion(version).setArtifact(artifact));
    }
  }

  private String getManagementKey(Artifact artifact) {
    return artifact.getGroupId()
      + ":" + artifact.getArtifactId()
      + ":" + artifact.getExtension()
      + (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()
      ? ":" + artifact.getClassifier() : "");
  }

  private String getManagementKey(Dependency dependency) {
    return dependency.getManagementKey();
  }

  /**
   * Represents a resolved artifact.
   */
  public static class ResolvedArtifact {
    private Artifact artifact;
    private String selectedVersion;
    private final Set<String> usages = new LinkedHashSet<>();

    public Artifact getArtifact() {
      return artifact;
    }

    public ResolvedArtifact setArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    @SuppressWarnings("unused")
    public String getSelectedVersion() {
      return selectedVersion;
    }

    public ResolvedArtifact setSelectedVersion(String selectedVersion) {
      this.selectedVersion = selectedVersion;
      return this;
    }

    public ResolvedArtifact addUsage(String key) {
      usages.add(key);
      return this;
    }

    @SuppressWarnings("unused")
    public Set<String> getUsages() {
      return usages;
    }
  }

}
