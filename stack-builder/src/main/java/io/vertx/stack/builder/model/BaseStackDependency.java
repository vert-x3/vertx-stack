package io.vertx.stack.builder.model;

import io.vertx.stack.builder.utils.StackVersion;

/**
 * Represent the inherited / from / base stack. It can be ommited if the stack is already installed.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class BaseStackDependency extends StackDependency {

  /**
   * The official full stack.
   */
  public static BaseStackDependency FULL;

  /**
   * The official min stack.
   */
  public static BaseStackDependency MIN;

  /**
   * The official base stack.
   */
  public static BaseStackDependency BASE;

  static {
    MIN = new BaseStackDependency();
    MIN.setGroupId("io.vertx");
    MIN.setArtifactId("vertx-stack-dist");
    MIN.setVersion(StackVersion.VERSION);
    MIN.setClassifier("min");
    MIN.setType("zip");
    // We strip the first directory.
    MIN.setStrip(1);
    MIN.setId("min");

    BASE = new BaseStackDependency();
    BASE.setGroupId("io.vertx");
    BASE.setArtifactId("vertx-stack-dist");
    BASE.setVersion(StackVersion.VERSION);
    BASE.setClassifier("base");
    BASE.setType("zip");
    // We strip the first directory.
    BASE.setStrip(1);
    BASE.setId("base");

    FULL = new BaseStackDependency();
    FULL.setGroupId("io.vertx");
    FULL.setArtifactId("vertx-stack-dist");
    FULL.setVersion(StackVersion.VERSION);
    FULL.setClassifier("full");
    FULL.setType("zip");
    // We strip the first directory.
    FULL.setStrip(1);
    FULL.setId("full");
  }


  private int strip;
  private String id;

  /**
   * In the case of an official stack provided by vert.x, the id of the stack. Are supported {@code full, min} and
   * {@code base}.
   *
   * @param id the id of the official stack.
   * @return the current {@link BaseStackDependency}
   */
  public BaseStackDependency setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * @return the configured id, {@code null} if none.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the number of directory to ignore while extracting the stack.
   *
   * @return the number of directory to strip.
   */
  public int getStrip() {
    return strip;
  }

  /**
   * Sets the number of directory to strip when unpacking the stack. This option lets you strip the base directory
   * if needed.
   *
   * @param strip the number of directory to strip
   * @return the current {@link BaseStackDependency}
   */
  public BaseStackDependency setStrip(int strip) {
    if (strip < 0) {
      throw new IllegalArgumentException("Invalid strip factor - it must be positive");
    }
    this.strip = strip;
    return this;
  }


}
