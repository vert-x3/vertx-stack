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

package io.vertx.stack.builder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.vertx.stack.builder.model.*;
import io.vertx.stack.builder.utils.Artifacts;
import io.vertx.stack.builder.utils.Resolver;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Creates stack.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackBuilder {

  public static final Logger LOGGER = Logger.getLogger("vertx-stack-builder");

  private Stack stack;
  private Resolver resolver = new Resolver();
  private StackBuilderOptions options;

  /**
   * Reads the stack description from a json file.
   *
   * @param json the json file
   * @return the current {@link StackBuilder}
   */
  public StackBuilder fromJsonFile(File json) {
    ObjectMapper mapper = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    try {
      stack = mapper.readValue(json, Stack.class).setDescriptor(json);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + json.getAbsolutePath(), e);
    }
    if (!stack.getArtifacts().isEmpty()) {
      throw new IllegalArgumentException("Stack descriptor cannot define the artifact set");
    }
    return this;
  }

  /**
   * Reads the stack description from a xml file.
   *
   * @param xml the xml file
   * @return the current {@link StackBuilder}
   */
  public StackBuilder fromXmlFile(File xml) {
    XmlMapper mapper = new XmlMapper();
    try {
      stack = mapper.readValue(xml, Stack.class).setDescriptor(xml);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + xml.getAbsolutePath(), e);
    }
    if (!stack.getArtifacts().isEmpty()) {
      throw new IllegalArgumentException("Stack descriptor cannot define the artifact set");
    }
    return this;
  }

  /**
   * Reads the stack description from a yaml file.
   *
   * @param yaml the yaml file
   * @return the current {@link StackBuilder}
   */
  public StackBuilder fromYamlFile(File yaml) {
    YAMLMapper mapper = new YAMLMapper();
    try {
      stack = mapper.readValue(yaml, Stack.class).setDescriptor(yaml);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + yaml.getAbsolutePath(), e);
    }
    if (!stack.getArtifacts().isEmpty()) {
      throw new IllegalArgumentException("Stack descriptor cannot define the artifact set");
    }
    return this;
  }

  /**
   * Sets the stack.
   *
   * @param stack the stack
   * @return the current {@link StackBuilder}
   */
  public StackBuilder setStack(Stack stack) {
    this.stack = stack;
    return this;
  }

  public Stack add(String gacv, boolean transitive) {
    Objects.requireNonNull(stack, "Stack not defined");
    stack.addDependency(new StackDependency(gacv).setIgnoreTransitive(!transitive));
    return build();
  }

  public Stack add(String gacv) {
    return add(gacv, true);
  }

  public Stack remove(String gacv) {
    // Removing a dependency is a bit tricky as we need to be sure that is does not break the integrity of the stack
    // Case are:
    // 1) the artifact is not contained => noop
    // 2) the artifact is contained as a top level artifact => remove file, remove all no more used dependency
    // 3) the artifact is contained as a dependency => do nothing (warning)
    Artifact artifactToRemove = new DefaultArtifact(gacv);
    List<Runnable> actions = new ArrayList<>();

    for (StackArtifact artifact : new ArrayList<>(stack.getArtifacts())) {
      if (artifact.matches(artifactToRemove)) {
        // We have found the artifact, check it is not used somewhere else
        List<StackArtifact> usages = getArtifactUsingDependency(artifact, null);
        if (!usages.isEmpty()) {
          throw new IllegalStateException("Cannot remove artifact " + artifactToRemove
              + " because it is used by " + usages);
        }
        stack.getArtifacts().remove(artifact);
        LOGGER.info("Removing " + artifact.getFile().getName());
        actions.add(() -> FileUtils.deleteQuietly(artifact.getFile()));
        for (StackArtifact dependency : artifact.getDependencies()) {
          // Check if someone else use this dependency, or if it's a top level artifact
          if (isTopLevelArtifact(dependency)) {
            LOGGER.info("Skip deletion of " + dependency.getFile().getName()
                + " - it belongs to another artifact");
          } else {
            List<StackArtifact> users = getArtifactUsingDependency(dependency, artifact);
            if (!users.isEmpty()) {
              LOGGER.info("Skip deletion of " + dependency.getFile().getName()
                  + " - it is used by " + users);
            } else {
              LOGGER.info("Removing " + dependency.getFile().getName());
              actions.add(() -> FileUtils.deleteQuietly(dependency.getFile()));
            }
          }
        }
      }
    }

    actions.stream().forEach(Runnable::run);
    return stack;
  }

  private List<StackArtifact> getArtifactUsingDependency(StackArtifact dependency, StackArtifact exclusion) {
    List<StackArtifact> users = new ArrayList<>();
    for (StackArtifact artifact : stack.getArtifacts()) {
      if (artifact == exclusion) {
        continue;
      }
      users.addAll(
          artifact.getDependencies().stream()
              .filter(dep -> dep.toString().equals(dependency.toString()))
              .map(a -> artifact)
              .collect(Collectors.toList()));
    }
    return users;
  }

  private boolean isTopLevelArtifact(StackArtifact artifact) {
    return stack.getArtifacts().stream().filter(a -> a.toString().equals(artifact.toString()))
        .findFirst().isPresent();
  }

  public Stack build() {
    return build(new StackBuilderOptions());
  }

  public Stack build(StackBuilderOptions options) {
    Objects.requireNonNull(stack, "Stack not defined");
    this.options = options;
    createOutputDirectory();
    manageFromStack();
    readDotStack();
    resolveDependencies();
    writeDotStack();
    copyAdditionalFiles();
    return stack;
  }

  private void readDotStack() {
    File file = new File(stack.getDirectory(), Stack.DOT_STACK_FILE_NAME);
    if (file.isFile()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, StackArtifact.class);
        stack.setArtifacts(mapper.readValue(file, type));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read " + file.getAbsolutePath(), e);
      }
    }
  }

  private void writeDotStack() {
    File file = new File(stack.getDirectory(), Stack.DOT_STACK_FILE_NAME);
    try {
      new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, stack.getArtifacts());
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write " + file.getAbsolutePath(), e);
    }
  }

  private void copyAdditionalFiles() {
    for (AdditionalFile file : stack.getFiles()) {
      File output = new File(stack.getDirectory(), file.getTargetDirectory());
      output.mkdirs();
      if (!file.getFile().exists()) {
        throw new IllegalStateException("Cannot copy additional file " + file.getFile().getAbsolutePath() + " - the " +
            "file does not exit");
      }
      if (file.getFile().isDirectory()) {
        LOGGER.info("Copying directory " + file.getFile().getAbsolutePath() + " to " + output.getAbsolutePath());
        try {
          FileUtils.copyDirectory(file.getFile(), output);
        } catch (IOException e) {
          throw new IllegalStateException("Cannot copy additional file " + file.getFile().getAbsolutePath(), e);
        }
      } else {
        // File case.
        try {
          FileUtils.copyFileToDirectory(file.getFile(), output);
        } catch (IOException e) {
          throw new IllegalStateException("Cannot copy additional file " + file.getFile().getAbsolutePath(), e);
        }
      }

    }
  }

  private void resolveDependencies() {
    List<Runnable> actions = new ArrayList<>();
    for (StackDependency dependency : stack.getDependencies()) {
      List<Artifact> artifacts = resolver.resolve(dependency);
      if (artifacts.isEmpty()) {
        throw new IllegalStateException("Cannot resolve " + dependency.getGACV());
      }

      Artifact main = artifacts.get(0);
      List<Artifact> dependencies = new ArrayList<>();
      if (artifacts.size() > 1) {
        dependencies = artifacts.subList(1, artifacts.size());
      }

      LOGGER.info("Resolution result for " + main + ": " + dependencies);

      computeActionsAndEnsureNoConflicts(main, dependencies, actions);
    }
    // If we reach this point, we didn't detect a conflict, run the actions
    actions.stream().forEach(Runnable::run);
  }

  private void computeActionsAndEnsureNoConflicts(Artifact main, List<Artifact> dependencies, List<Runnable> actions) {
    File lib = new File(stack.getDirectory(), "lib");

    // Dependencies
    for (Artifact artifact : dependencies) {
      computeActionForArtifact(actions, lib, artifact, main);
    }

    // Main
    computeActionForArtifact(actions, lib, main, null);

    // Update artifacts
    updateArtifactList(main, dependencies, lib);
  }

  private void computeActionForArtifact(List<Runnable> actions, File lib, Artifact artifact,
                                        Artifact maybeMain) {
    ensureNoStackConflict(artifact, maybeMain);
    File maybeFile = Artifacts.getArtifactFile(lib, artifact);
    if (maybeFile == null) {
      // No conflict, the file does not exit
      actions.add(copyArtifactAction(lib, artifact, actions));
    } else if (artifact.getFile().getName().equals(maybeFile.getName())) {
      // Ok same file name
      if (artifact.isSnapshot()) {
        actions.add(copyArtifactAction(lib, artifact, actions));
      } else {
        actions.add(skippingArtifactAction(artifact));
      }
    } else if (!artifact.isSnapshot()) {
      // Release conflict
      String message = "conflict detected between " + artifact + " and " +
          maybeFile.getName() + ".";
      if (options.isIgnoreArtifactConflict()) {
        LOGGER.warning(message + " Ignoring " + artifact);
      } else {
        throw new IllegalStateException("Cannot build stack - " + message);
      }
    } else {
      // The artifact is a SNAPSHOT
      String version = Artifacts.getSnapshotVersion(artifact, maybeFile.getName());
      if (version != null && version.equals(artifact.getVersion())) {
        // Same SNAPSHOT version
        actions.add(removeArtifactAction(maybeFile));
        actions.add(copyArtifactAction(lib, artifact, actions));
      } else {
        // Snapshot conflicts
        String message = "conflict detected between " + artifact + " and " +
            maybeFile.getName() + ".";
        if (options.isIgnoreArtifactConflict()) {
          LOGGER.warning(message + " Ignoring " + artifact);
        } else {
          throw new IllegalStateException("Cannot build stack - " + message);
        }
      }
    }
  }

  private Runnable removeArtifactAction(File file) {
    return () -> FileUtils.deleteQuietly(file);
  }

  private Runnable copyArtifactAction(File lib, Artifact artifact, List<Runnable> actions) {
    // Check the that artifact was not already enqueued.
    for (Runnable runnable : actions) {
      if (runnable instanceof CopyFileAction
          && ((CopyFileAction) runnable).artifact.getFile().getAbsolutePath().equals(artifact.getFile().getAbsolutePath())) {
        // There is another task that will copy the artifact, so noop:
        return () -> {
        };
      }
    }
    return new CopyFileAction(lib, artifact);
  }

  private Runnable skippingArtifactAction(Artifact artifact) {
    return () -> LOGGER.info("Skip artifact " + artifact + " - already present");
  }

  private void ensureNoStackConflict(Artifact artifact, Artifact maybeMain) {
    List<StackArtifact> artifacts = stack.getArtifacts();
    for (StackArtifact a : artifacts) {
      if (!a.matches(artifact) && a.matchesPartially(artifact)) {
        // Conflict
        String message;
        if (maybeMain != null) {
          message = "conflict detected between " + artifact
              + " (a dependency of " + maybeMain + ") and the existing artifact " + a + ".";
        } else {
          message = "conflict detected between " + artifact
              + " and the existing artifact " + a + ".";
        }
        if (options.isIgnoreArtifactConflict()) {
          LOGGER.warning(message + " Ignoring " + artifact);
        } else {
          throw new IllegalStateException("Cannot build stack - " + message);
        }
      }

      for (StackArtifact dependency : a.getDependencies()) {
        if (!dependency.matches(artifact) && dependency.matchesPartially(artifact)) {
          // Conflict
          String message = "conflict detected between " + artifact + " and a " +
              "dependency of " + a + " (" + dependency + ").";
          if (options.isIgnoreArtifactConflict()) {
            LOGGER.warning(message + " Ignoring " + artifact);
          } else {
            throw new IllegalStateException("Cannot build stack - " + message);
          }
        }
      }

    }
  }

  private void updateArtifactList(Artifact artifact, List<Artifact> dependencies, File lib) {
    // Is it contained ?
    for (StackArtifact existing : stack.getArtifacts()) {
      if (existing.matches(artifact)) {
        existing.setFile(new File(lib, Artifacts.getFileName(artifact)));
        existing.setDependencies(dependencies.stream()
            .map(d -> new StackArtifact(d)
                .setFile(new File(lib, Artifacts.getFileName(d))))
            .collect(Collectors.toList()));
        return;
      }
    }

    StackArtifact sa = new StackArtifact(artifact);
    sa.setDependencies(dependencies.stream()
        .map(d -> new StackArtifact(d)
            .setFile(new File(lib, Artifacts.getFileName(d))))
        .collect(Collectors.toList()));
    sa.setFile(new File(lib, Artifacts.getFileName(artifact)));
    stack.addArtifact(sa);
  }

  private void manageFromStack() {
    BaseStackDependency from = stack.getFrom();
    if (from == null) {
      LOGGER.fine("No 'from' stack specified");
      return;
    }

    if (from.getDescriptor() != null) {
      // If the stack is loaded form a descriptor, compute the path relative to this descriptor
      // else, use the current directory
      File descriptorFile = null;
      if (stack.getDescriptor() == null) {
        descriptorFile = new File(from.getDescriptor());
      } else {
        descriptorFile = new File(stack.getDescriptor().getParent(), from.getDescriptor());
      }
      LOGGER.info("'from' stack is a descriptor: " + descriptorFile.getAbsolutePath());
      if (!descriptorFile.isFile()) {
        throw new IllegalArgumentException("The 'from' descriptor must be a file: " + descriptorFile.getAbsolutePath());
      }

      LOGGER.info("Resolving parent stack");
      StackBuilder builder = new StackBuilder().fromDescriptor(descriptorFile);
      // Override directory.
      builder.stack.setDirectory(stack.getDirectory());

      builder.build();
      return;
    }

    if (from.getId() != null) {
      LOGGER.info("'from' stack identified by id: " + from.getId());
      if (from.getId().equalsIgnoreCase("full")) {
        from = BaseStackDependency.FULL;
      } else if (from.getId().equalsIgnoreCase("base")) {
        from = BaseStackDependency.BASE;
      } else if (from.getId().equalsIgnoreCase("min")) {
        from = BaseStackDependency.MIN;
      } else {
        throw new IllegalArgumentException("Unknown id - the id '" + from.getId() + "' is invalid, it must be either " +
            "'full', 'base' or 'min'");
      }
    }

    // No transitive.
    from.setIgnoreTransitive(true);
    from.setType("zip");

    final List<Artifact> artifacts = resolver.resolve(from);
    if (artifacts.isEmpty()) {
      throw new IllegalStateException("Cannot resolve the 'from' stack");
    }
    File distribution = artifacts.get(0).getFile();

    LOGGER.info("'From' stack resolved : " + distribution.getAbsolutePath());
    LOGGER.info("Unpacking...");

    try {
      uncompress(from, distribution.getAbsoluteFile());
    } catch (IOException e) {
      throw new IllegalStateException("Error while unzipping base stack", e);
    }

  }

  private void uncompress(BaseStackDependency base, File distribution) throws IOException {
    FileInputStream stream = null;
    ZipInputStream zis = null;
    try {
      stream = new FileInputStream(distribution);
      zis = new ZipInputStream(stream);
      ZipEntry ze = zis.getNextEntry();
      while (ze != null) {
        if (ze.isDirectory()) {
          ze = zis.getNextEntry();
          continue;
        }

        String fileName = ze.getName();

        // Compute the name of the output file and strip the number of specified directory
        if (base.getStrip() != 0) {
          for (int i = 0; i < base.getStrip(); i++) {
            int index = fileName.indexOf("/");
            if (index == -1) {
              // No more content, will skip this entry
              fileName = null;
              break;
            } else {
              fileName = fileName.substring(index + 1);
            }
          }
        }

        if (fileName == null) {
          ze = zis.getNextEntry();
          continue;
        }

        File newFile = new File(stack.getDirectory() + File.separator + fileName);
        LOGGER.fine("Unzipping " + ze.getName() + " to " + newFile.getAbsolutePath());
        new File(newFile.getParent()).mkdirs();
        FileOutputStream output = FileUtils.openOutputStream(newFile);
        IOUtils.copy(zis, output);
        IOUtils.closeQuietly(output);
        ze = zis.getNextEntry();
      }
    } finally {
      IOUtils.closeQuietly(stream);
      IOUtils.closeQuietly(zis);
    }

  }

  private void createOutputDirectory() {
    File directory = stack.getDirectory();
    Objects.requireNonNull(directory, "The stack directory must be set");
    final boolean r = directory.mkdirs();
    LOGGER.fine("Creation of " + directory.getAbsolutePath() + " : " + r);
  }


  public StackBuilder fromDescriptor(File descriptor) {
    if (descriptor.getName().endsWith(".xml")) {
      fromXmlFile(descriptor);
    } else if (descriptor.getName().endsWith(".json")) {
      fromJsonFile(descriptor);
    } else if (descriptor.getName().endsWith(".yaml")) {
      fromYamlFile(descriptor);
    } else {
      throw new IllegalArgumentException("Not supported file type " + descriptor.getName());
    }
    return this;
  }


  private class CopyFileAction implements Runnable {

    private final File lib;
    private final Artifact artifact;

    private CopyFileAction(File lib, Artifact artifact) {
      this.lib = lib;
      this.artifact = artifact;
    }

    public void run() {
      File output = new File(lib, Artifacts.getFileName(artifact));
      try {
        LOGGER.info("Copy artifact " + artifact + " to " + output.getAbsolutePath());
        FileUtils.copyFile(artifact.getFile(), output);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot copy artifact " + artifact + " to the 'lib' directory", e);
      }
    }
  }
}
