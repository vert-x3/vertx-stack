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

import io.vertx.stack.resolver.ResolverOptions;

import java.io.File;
import java.util.List;

/**
 * The options configuring the stack resolution.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackResolutionOptions extends ResolverOptions {

  private boolean failOnConflicts;

  private boolean cacheDisabled;

  private boolean cacheDisabledForSnapshots;

  private File cacheFile;

  /**
   * @return whether or not the resolution fails on conflicts or just prints a warning.
   */
  public boolean isFailOnConflicts() {
    return failOnConflicts;
  }

  /**
   * Sets whether or not the resolution should fail on conflicts.
   *
   * @param failOnConflicts whether or not the resolution fails on conflicts.
   * @return the current {@link StackResolutionOptions} instance
   */
  public StackResolutionOptions setFailOnConflicts(boolean failOnConflicts) {
    this.failOnConflicts = failOnConflicts;
    return this;
  }

  @Override
  public StackResolutionOptions setHttpProxy(String httpProxy) {
    super.setHttpProxy(httpProxy);
    return this;
  }

  @Override
  public StackResolutionOptions setHttpsProxy(String httpsProxy) {
    super.setHttpsProxy(httpsProxy);
    return this;
  }

  @Override
  public StackResolutionOptions setLocalRepository(String localRepository) {
    super.setLocalRepository(localRepository);
    return this;
  }

  @Override
  public StackResolutionOptions setRemoteRepositories(List<String> remoteRepositories) {
    super.setRemoteRepositories(remoteRepositories);
    return this;
  }

  /**
   * @return whether or not the cache is disabled.
   */
  public boolean isCacheDisabled() {
    return cacheDisabled;
  }

  /**
   * Sets whether or not the cache is disabled.
   *
   * @param cacheDisabled {@code true} to disable the cache, {@code false} to enable it (default)
   * @return the current {@link StackResolutionOptions} instance
   */
  public StackResolutionOptions setCacheDisabled(boolean cacheDisabled) {
    this.cacheDisabled = cacheDisabled;
    return this;
  }

  /**
   * @return whether or not the cache is disabled for snapshots
   */
  public boolean isCacheDisabledForSnapshots() {
    return cacheDisabledForSnapshots;
  }

  /**
   * Sets whether or not the cache is disabled for snapshot.
   *
   * @param cacheDisabledForSnapshots {@code true} to disable the cache for snapshot, {@code false} to enable it
   *                                  (default)
   * @return the current {@link StackResolutionOptions} instance
   */
  public StackResolutionOptions setCacheDisabledForSnapshots(boolean cacheDisabledForSnapshots) {
    this.cacheDisabledForSnapshots = cacheDisabledForSnapshots;
    return this;
  }

  /**
   * @return the location of the cache file is set.
   */
  public File getCacheFile() {
    return cacheFile;
  }

  /**
   * Sets the cache file location (json file).
   *
   * @param cacheFile the cache file
   * @return the current {@link StackResolutionOptions} instance
   */
  public StackResolutionOptions setCacheFile(File cacheFile) {
    this.cacheFile = cacheFile;
    return this;
  }
}
