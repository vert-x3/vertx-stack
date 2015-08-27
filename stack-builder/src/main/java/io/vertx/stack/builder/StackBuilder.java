package io.vertx.stack.builder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.vertx.stack.builder.model.AdditionalFile;
import io.vertx.stack.builder.model.BaseStackDependency;
import io.vertx.stack.builder.model.Stack;
import io.vertx.stack.builder.model.StackDependency;
import io.vertx.stack.builder.utils.Artifacts;
import io.vertx.stack.builder.utils.Resolver;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
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

  public void build() {
    Objects.requireNonNull(stack, "Stack not defined");
    createOutputDirectory();
    downloadBaseStack();
    resolveDependencies();
    copyAdditionalFiles();
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
    File lib = new File(stack.getDirectory(), "lib");
    for (StackDependency dependency : stack.getDependencies()) {
      List<Artifact> artifacts = resolver.resolve(dependency);
      if (artifacts.isEmpty()) {
        throw new IllegalStateException("Cannot resolve " + dependency.getGACV());
      }
      for (Artifact artifact : artifacts) {
        try {
          File file = Artifacts.getArtifactFile(lib, artifact);
          if (file == null) {
            LOGGER.info("Copying " + artifact + " to " + lib.getAbsolutePath());
            FileUtils.copyFileToDirectory(artifact.getFile(), lib);
            continue;
          }

          // Conflict detected

          // 1) Easy case, it's the same file name
          if (artifact.getFile().getName().equals(file.getName())) {
            // Ok same file name
            // If it's a snapshot, overwrite if newer
            if (artifact.isSnapshot()
                && artifact.getFile().lastModified() > file.lastModified()) {
              LOGGER.info("Overwriting " + artifact);
              FileUtils.copyFileToDirectory(artifact.getFile(), lib);
              continue;
            } else {
              LOGGER.info("Skipping " + artifact + " - already present or newer");
              continue;
            }
          }

          // 2) release case => error
          if (!artifact.isSnapshot()) {
            LOGGER.severe("Conflict detected between " + artifact
                + " and the existing file " + file.getName());
            throw new IllegalStateException("Cannot build the stack: " +
                "Conflict detected between " + artifact +
                " and the existing file " + file.getName());
          }

          // 3) artifact is a snapshot

          String version = Artifacts.getSnapshotVersion(artifact, file.getName());
          if (version != null && version.equals(artifact.getVersion())) {
            // Both are snapshot.
            // Overwrite if newer.
            if (artifact.getFile().lastModified() > file.lastModified()) {
              LOGGER.info("Overwriting file for " + artifact);
              // Need to delete the existing file as the name might be different
              // (snapshot timestamp)
              FileUtils.deleteQuietly(file);
              FileUtils.copyFileToDirectory(artifact.getFile(), lib);
            } else {
              LOGGER.info("Skipping " + artifact + " - existing file is newer");
            }
          } else {
            LOGGER.severe("Conflict detected between " + artifact
                + " and the existing file " + file.getName());
            throw new IllegalStateException("Cannot build the stack: " +
                "Conflict detected between " + artifact +
                " and the existing file " + file.getName());
          }
        } catch (IOException e) {
          LOGGER.severe("Cannot copy file " + artifact.getFile().getAbsolutePath()
              + " to " + lib.getAbsolutePath() + " : " + e.getMessage());
          throw new IllegalStateException("Error while copying a dependency", e);
        }
      }
    }
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
