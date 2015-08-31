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
      stack = mapper.readValue(json, Stack.class);
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
   * @param json the xml file
   * @return the current {@link StackBuilder}
   */
  public StackBuilder fromXmlFile(File json) {
    XmlMapper mapper = new XmlMapper();
    try {
      stack = mapper.readValue(json, Stack.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + json.getAbsolutePath(), e);
    }
    if (!stack.getArtifacts().isEmpty()) {
      throw new IllegalArgumentException("Stack descriptor cannot define the artifact set");
    }
    return this;
  }

  /**
   * Reads the stack description from a yaml file.
   *
   * @param json the yaml file
   * @return the current {@link StackBuilder}
   */
  public StackBuilder fromYamlFile(File json) {
    YAMLMapper mapper = new YAMLMapper();
    try {
      stack = mapper.readValue(json, Stack.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + json.getAbsolutePath(), e);
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
            if (! users.isEmpty()) {
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
    Objects.requireNonNull(stack, "Stack not defined");
    createOutputDirectory();
    readDotStack();
    downloadBaseStack();
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
        System.out.println("Reloaded : " + stack.getArtifacts());
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
        dependencies = artifacts.subList(1, artifacts.size() - 1);
      }

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

  private void computeActionForArtifact(List<Runnable> actions, File lib, Artifact artifact, Artifact maybeMain) {
    ensureNoStackConflict(artifact, maybeMain);
    File maybeFile = Artifacts.getArtifactFile(lib, artifact);
    if (maybeFile == null) {
      // No conflict, the file does not exit
      actions.add(copyArtifactAction(lib, artifact));
    } else if (artifact.getFile().getName().equals(maybeFile.getName())) {
      // Ok same file name
      if (artifact.isSnapshot()) {
        actions.add(copyArtifactAction(lib, artifact));
      } else {
        actions.add(skippingArtifactAction(artifact));
      }
    } else if (!artifact.isSnapshot()) {
      // Release conflict
      throw new IllegalStateException("Cannot build stack - conflict detected between " + artifact + " and " +
          maybeFile.getName());
    } else {
      // The artifact is a SNAPSHOT
      String version = Artifacts.getSnapshotVersion(artifact, maybeFile.getName());
      if (version != null && version.equals(artifact.getVersion())) {
        // Same SNAPSHOT version
        actions.add(removeArtifactAction(maybeFile));
        actions.add(copyArtifactAction(lib, artifact));
      } else {
        // Snapshot conflicts
        throw new IllegalStateException("Cannot build stack - conflict detected between " + artifact + " and " +
            maybeFile.getName());
      }
    }
  }

  private Runnable removeArtifactAction(File file) {
    return () -> FileUtils.deleteQuietly(file);
  }

  private Runnable copyArtifactAction(File lib, Artifact artifact) {
    return () -> {
      File output = new File(lib, Artifacts.getFileName(artifact));
      try {
        LOGGER.info("Copy artifact " + artifact + " to " + output.getAbsolutePath());
        FileUtils.copyFile(artifact.getFile(), output);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot copy artifact " + artifact + " to the 'lib' directory", e);
      }
    };
  }

  private Runnable skippingArtifactAction(Artifact artifact) {
    return () -> LOGGER.info("Skip artifact " + artifact + " - already present");
  }

  private void ensureNoStackConflict(Artifact artifact, Artifact maybeMain) {
    List<StackArtifact> artifacts = stack.getArtifacts();
    System.out.println("Checking conflict for " + artifact + " / " + artifacts);
    for (StackArtifact a : artifacts) {
      System.out.println(a + " Complete match " + a.matches(artifact));
      System.out.println(a + " Partial match " + a.matchesPartially(artifact));
      if (!a.matches(artifact) && a.matchesPartially(artifact)) {
        // Conflict
        if (maybeMain != null) {
          throw new IllegalStateException("Cannot build stack: conflict detected between " + artifact
              + " (a dependency of " + maybeMain + ") and the existing artifact " + a);
        }
        throw new IllegalStateException("Cannot build stack: conflict detected between " + artifact + " and the " +
            "existing artifact " + a);
      }

      System.out.println(artifact + " - Checking conflict against dependencies: " + a.getDependencies());
      for (StackArtifact dependency : a.getDependencies()) {
        if (!dependency.matches(artifact) && dependency.matchesPartially(artifact)) {
          // Conflict
          throw new IllegalStateException("Cannot build stack: conflict detected between " + artifact + " and a " +
              "dependency of " + a + " (" + dependency + ")");
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

  private void downloadBaseStack() {
    BaseStackDependency from = stack.getFrom();
    if (from == null) {
      LOGGER.fine("No 'from' stack specified");
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


}
