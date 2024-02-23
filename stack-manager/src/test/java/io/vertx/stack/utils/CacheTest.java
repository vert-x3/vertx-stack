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

package io.vertx.stack.utils;

import io.vertx.stack.model.Artifact;
import io.vertx.stack.resolver.ResolutionOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CacheTest {

  private Cache cache;
  private File cacheFile;

  static File TEMP_FILE;

  static {
    try {
      TEMP_FILE = File.createTempFile("acme", ".jar");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Before
  public void setUp() {
    cacheFile = new File("target/test-cache/cache.json");
    if (cacheFile.isFile()) {
      cacheFile.delete();
    }

    cache = new Cache(false, false, cacheFile);

  }

  @Test
  public void testCachingOfReleaseAndUpdate() {
    String gacv = "org.acme:acme:jar:1.0";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0").setFile(TEMP_FILE);
    Artifact artifact2 = Artifact.artifact("org.acme:acme-dep:jar:1.0").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    cache.put(gacv, options, Arrays.asList(artifact, artifact2));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(2);
  }

  @Test
  public void testCachingOfSnapshotAndUpdate() {
    String gacv = "org.acme:acme:jar:1.0-SNAPSHOT";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0-SNAPSHOT").setFile(TEMP_FILE);
    Artifact artifact2 = Artifact.artifact("org.acme:acme-dep:jar:1.0-SNAPSHOT").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    cache.put(gacv, options, Arrays.asList(artifact, artifact2));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(2);
  }


  @Test
  public void testDisabledCache() throws IOException {
    String gacv = "org.acme:acme:jar:1.0";
    cache = new Cache(true, false, cacheFile);
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    File file = File.createTempFile("acme", ".jar");
    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0").setFile(file);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).isNull();
  }

  @Test
  public void testCacheDisabledForSnapshots() {
    String gacv = "org.acme:acme:jar:1.0-SNAPSHOT";
    cache = new Cache(false, true, cacheFile);
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0-SNAPSHOT").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).isNull();
  }

  @Test
  public void testWithInvalidArtifact() {
    String gacv = "org.acme:acme:jar:1.0";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0").setFile(new File("does not exist.jar"));

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).isNull();
  }

  @Test
  public void testWithEmptyResolution() {
    String gacv = "org.acme:acme:jar:1.0";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();
    cache.put(gacv, options, Collections.emptyList());
    list = cache.get(gacv, options);
    assertThat(list).isNull();
  }

  @Test
  public void testCacheReloading() {
    String gacv = "org.acme:acme:jar:1.0";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);
    cache.writeCacheOnFile();

    cache = new Cache(false, false, cacheFile);

    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);
  }

  @Test
  public void testSnapshotEviction() {
    String gacv = "org.acme:acme:jar:1.0-SNAPSHOT";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0-SNAPSHOT").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    Optional<Cache.CacheEntry> entry = cache.find(gacv, options);
    entry.get().setInsertionTime(System.currentTimeMillis() - 25 * 60 * 60 * 1000);

    list = cache.get(gacv, options);
    assertThat(list).isNull();
  }

  @Test
  public void testNonSnapshotEviction() {
    String gacv = "org.acme:acme:jar:1.0-SNAPSHOT";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0-SNAPSHOT").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    Optional<Cache.CacheEntry> entry = cache.find(gacv, options);
    entry.get().setInsertionTime(System.currentTimeMillis() - 22 * 60 * 60 * 1000);

    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);
  }

  @Test
  public void testCachingUsingDifferentResolutionOption() {
    String gacv = "org.acme:acme:jar:1.0";
    ResolutionOptions options = new ResolutionOptions();
    List<Artifact> list = cache.get(gacv, options);
    assertThat(list).isNull();

    Artifact artifact = Artifact.artifact("org.acme:acme:jar:1.0").setFile(TEMP_FILE);

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);

    options = new ResolutionOptions().setWithTransitive(false);
    list = cache.get(gacv, options);
    assertThat(list).isNull();

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);
    assertThat(cache.size()).isEqualTo(2);

    options = new ResolutionOptions().setWithTransitive(true).addExclusion("org.acme:transitive");
    list = cache.get(gacv, options);
    assertThat(list).isNull();

    cache.put(gacv, options, Collections.singletonList(artifact));
    list = cache.get(gacv, options);
    assertThat(list).hasSize(1);
    assertThat(cache.size()).isEqualTo(3);

  }


}
