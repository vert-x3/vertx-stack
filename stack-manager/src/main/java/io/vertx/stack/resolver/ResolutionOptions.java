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

package io.vertx.stack.resolver;

import java.util.*;

/**
 * Options configuring the resolution of a single dependency.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ResolutionOptions {

  private boolean withTransitive = true;
  private List<String> exclusions = new ArrayList<>();

  /**
   * @return whether or not the resolution should also resolve the transitive dependencies.
   */
  public boolean isWithTransitive() {
    return withTransitive;
  }

  /**
   * Sets whether or not the resolution of the dependency should include the transitive dependencies.
   *
   * @param withTransitive whether or not the resolution should also resolve the transitive dependencies, {@code
   *                       true} by default.
   * @return the current {@link ResolutionOptions} instance
   */
  public ResolutionOptions setWithTransitive(boolean withTransitive) {
    this.withTransitive = withTransitive;
    return this;
  }

  /**
   * Adds an exclusion. The excluded dependencies and its children would not be resolved. The exclusion is given
   * under the following form: {@code groupId:artifactId}.
   *
   * @param exclusion the exclusion to add
   * @return the current {@link ResolutionOptions} instance
   */
  public ResolutionOptions addExclusion(String exclusion) {
    exclusions.add(exclusion);
    return this;
  }

  /**
   * Removes an exclusion.
   *
   * @param exclusion the exlusion to remove
   * @return the current {@link ResolutionOptions} instance
   * @see #addExclusion(String)
   */
  public ResolutionOptions removeExclusion(String exclusion) {
    exclusions.remove(exclusion);
    return this;
  }

  /**
   * @return the list of exclusions, empty if none.
   */
  public List<String> getExclusions() {
    return exclusions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResolutionOptions that = (ResolutionOptions) o;

    if (isWithTransitive() != that.isWithTransitive()) return false;

    Set<String> set1 = new HashSet<>();
    set1.addAll(that.getExclusions());
    Set<String> set2 = new HashSet<>();
    set2.addAll(getExclusions());
    return set1.equals(set2);
  }

  @Override
  public int hashCode() {
    int result = (isWithTransitive() ? 1 : 0);
    result = 31 * result + getExclusions().hashCode();
    return result;
  }
}
