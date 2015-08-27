package io.vertx.stack.builder.model;

import java.io.File;

/**
 * Defines a file or a directory added to the stack.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class AdditionalFile {

  /**
   * The file or directory.
   */
  private File file;

  /**
   * The target directory relative to the stack root.
   */
  private String targetDirectory;

  /**
   * @return the file or directory to include.
   */
  public File getFile() {
    return file;
  }

  /**
   * Sets the file or directory to add.
   *
   * @param file the file or directory
   * @return the current {@link AdditionalFile}
   */
  public AdditionalFile setFile(File file) {
    this.file = file;
    return this;
  }

  /**
   * @return the target directory
   */
  public String getTargetDirectory() {
    return targetDirectory;
  }

  /**
   * Sets the target directory.
   *
   * @param targetDirectory the target directory
   * @return the current {@link AdditionalFile}
   */
  public AdditionalFile setTargetDirectory(String targetDirectory) {
    this.targetDirectory = targetDirectory;
    return this;
  }
}
