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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.stack.resolver.ResolutionOptions;
import org.apache.maven.model.Exclusion;

/**
 * Represents a dependency. The stack describes a set of dependencies resolved as Maven dependencies. Each dependency
 * can be included or excluded and can instruct whether the resolution need to bring the transitive dependencies too.
 * <p/>
 * Be aware the dependencies cannot be set as "optional', as optionality do not make sense when we deploy the artifact.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Dependency extends org.apache.maven.model.Dependency {

  private boolean included = true;

  private boolean transitive = true;

  /**
   * Creates a new {@link Dependency}.
   *
   * @param groupId    the groupId
   * @param artifactId the artifactId
   * @param version    the version
   */
  public Dependency(String groupId, String artifactId, String version) {
    this();
    setGroupId(groupId);
    setArtifactId(artifactId);
    setVersion(version);
  }

  /**
   * Creates a new {@link Dependency}.
   *
   * @param groupId    the groupId
   * @param artifactId the artifactId
   * @param version    the version
   * @param type       the type / extension / packaging
   */
  public Dependency(String groupId, String artifactId, String version, String type) {
    this(groupId, artifactId, version);
    setType(type);
  }

  /**
   * Creates a new {@link Dependency}.
   * <p/>
   * The type is set to "jar" by default.
   */
  public Dependency() {
    setType("jar");
  }

  /**
   * @return {@code true} if the dependency is included, {@code false} otherwise.
   */
  public boolean isIncluded() {
    return included;
  }

  /**
   * Sets whether or not the dependency is included. Included by default.
   *
   * @return the current {@link Dependency} instance.
   */
  public Dependency setIncluded(boolean included) {
    this.included = included;
    return this;
  }

  /**
   * @return {@code true} if the dependency must be resolved with its transitive dependencies.
   */
  public boolean isTransitive() {
    return transitive;
  }

  /**
   * Sets whether or not the dependency resolution also resolves the transitive dependencies. Transitive dependencies
   * are resolved by default.
   *
   * @return the current {@link Dependency} instance.
   */
  public Dependency setTransitive(boolean transitive) {
    this.transitive = transitive;
    return this;
  }

  /**
   * Creates the {@link ResolutionOptions} object for the dependency.
   *
   * @return the {@link ResolutionOptions} instructing the dependency resolution.
   */
  public ResolutionOptions getResolutionOptions() {
    ResolutionOptions options = new ResolutionOptions();
    options.setWithTransitive(transitive);
    for (Exclusion exclusion : getExclusions()) {
      options.addExclusion(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
    }

    return options;
  }

  /**
   * @return the Maven GACV string.
   */
  public String getGACV() {
    return getManagementKey() + ":" + getVersion();
  }

  /**
   * Not supported property, as the concept of 'optional' does not make sense when building a stack.
   *
   * @param optional ignored
   */
  @Override
  @JsonIgnore
  public void setOptional(boolean optional) {
    throw new UnsupportedOperationException("You cannot add an optional dependency to a stack - optional does not " +
        "make sense in this case");
  }
}
