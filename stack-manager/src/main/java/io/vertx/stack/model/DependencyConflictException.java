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

/**
 * Thrown when a conflict is detected between two dependencies.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DependencyConflictException extends RuntimeException {

  private final String artifact;
  private final String chosenVersion;
  private final String dependency;
  private final String otherVersion;

  /**
   * Creates a {@link DependencyConflictException}.
   *
   * @param artifact      the artifact
   * @param chosenVersion the current version
   * @param dependency    the conflicting dependency
   * @param otherVersion  the other version
   */
  public DependencyConflictException(String artifact, String chosenVersion, String dependency, String otherVersion) {
    this.artifact = artifact;
    this.chosenVersion = chosenVersion;
    this.dependency = dependency;
    this.otherVersion = otherVersion;
  }

  /**
   * Returns the detail message string of this throwable.
   *
   * @return the detail message string of this {@code Throwable} instance
   * (which may be {@code null}).
   */
  @Override
  public String getMessage() {
    return "Conflict detected for artifact " + artifact + " - version " + chosenVersion + " was already selected " +
        "while " + dependency + " depends on version " + otherVersion;
  }
}
