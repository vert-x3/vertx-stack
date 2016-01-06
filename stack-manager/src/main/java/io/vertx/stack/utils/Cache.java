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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.stack.resolver.ResolutionOptions;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A cache storing the resolution result.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Cache {
  private final static Logger LOGGER = LoggerFactory.getLogger("stack-manager-cache");

  // We don't use the MAPPEr from vert.x because it requires some tuning.
  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(
      new SimpleModule("artifact-module").addDeserializer(Artifact.class, new JsonDeserializer<Artifact>() {
    @Override
    public Artifact deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      Artifact artifact;
      if (node.get("classifier") != null) {
        artifact = new DefaultArtifact(node.get("groupId").asText(), node.get("artifactId").asText(),
            node.get("classifier").asText(), node.get("extension").asText(), node.get("version").asText());
      } else {
        artifact = new DefaultArtifact(node.get("groupId").asText(), node.get("artifactId").asText(),
            node.get("extension").asText(), node.get("version").asText());
      }
      artifact = artifact.setFile(new File(node.get("file").asText()));
      return artifact;
    }
  }));

  private final boolean disabled;
  private final boolean disabledForSnapshot;

  List<CacheEntry> cache = new ArrayList<>();
  private File cacheFile;


  public Cache(boolean disabled, boolean disabledForSnapshot, File cacheFile) {
    this.disabled = disabled;
    this.disabledForSnapshot = disabledForSnapshot;
    this.cacheFile = cacheFile;

    // Load cache.
    if (this.cacheFile == null && Home.getVertxHome() != null) {
      this.cacheFile = new File(Home.getVertxHome(), ".stack-manager-cache.json");
      LOGGER.info("Set resolver cache to " + this.cacheFile.getAbsolutePath());
    }

    if (this.cacheFile != null && !this.cacheFile.isFile()) {
      this.cacheFile.getParentFile().mkdirs();
    }

    if (!disabled && this.cacheFile != null && this.cacheFile.isFile()) {
      LOGGER.info("Loading resolver cache from " + this.cacheFile.getAbsolutePath());
      JavaType type = MAPPER.getTypeFactory().
          constructCollectionType(List.class, CacheEntry.class);
      try {
        cache.addAll(MAPPER.readValue(this.cacheFile, type));
      } catch (IOException e) {
        LOGGER.error("Cannot read the cache entries from " + this.cacheFile.getAbsolutePath() + ": " + e.getMessage());
      }
    }
  }

  public void writeCacheOnFile() {
    if (disabled) {
      return;
    }
    if (cacheFile != null) {
      try {
        MAPPER.writer().writeValue(cacheFile, cache);
      } catch (IOException e) {
        LOGGER.error("Cannot write the cache entries to " + cacheFile.getAbsolutePath() + ": " + e.getMessage());
      }
    }
  }

  public List<Artifact> get(String gacv, ResolutionOptions resolutionOptions) {
    if (disabled) {
      return null;
    }

    if (disabledForSnapshot && gacv.contains("SNAPSHOT")) {
      return null;
    }

    Optional<CacheEntry> entry = find(gacv, resolutionOptions);
    if (entry.isPresent()) {
      if (isValid(entry.get())) {
        return entry.get().getArtifacts();
      } else {
        // cleanup required
        cache.remove(entry.get());
        return null;
      }
    }
    return null;
  }

  private boolean isValid(CacheEntry entry) {
    // If valid check two aspects:
    // All artifact files must be existing and non empty
    // If the dependency is a snapshot, it must check the insertion date.
    if (entry.getArtifacts().isEmpty()) {
      return false;
    }

    if (entry.getArtifacts().stream().filter(artifact -> !artifact.getFile().isFile())
        .findAny().isPresent()) {
      return false;
    }

    if (entry.gacv.contains("SNAPSHOT")) {
      long now = System.currentTimeMillis();
      long insertion = entry.getInsertionTime();
      return now - insertion < 24 * 60 * 60 * 1000; // 24 hours of 60 minutes composed by 60 seconds.
    }
    return true;
  }


  public void put(String gacv, ResolutionOptions resolutionOptions, List<Artifact> list) {
    if (disabled) {
      return;
    }

    if (disabledForSnapshot && gacv.contains("SNAPSHOT")) {
      return;
    }

    Optional<CacheEntry> entry = find(gacv, resolutionOptions);
    if (entry.isPresent()) {
      CacheEntry cached = entry.get();
      cached.setInsertionTime(System.currentTimeMillis()).setArtifacts(list);
    } else {
      CacheEntry cached = new CacheEntry();
      cached.setArtifacts(list)
          .setGacv(gacv)
          .setOptions(resolutionOptions)
          .setInsertionTime(System.currentTimeMillis());
      cache.add(cached);
    }
  }

  public Optional<CacheEntry> find(String gacv, ResolutionOptions options) {
    return cache.stream().filter(entry -> entry.gacv.equals(gacv)
        && entry.options.equals(options)).findFirst();
  }

  public int size() {
    return cache.size();
  }

  public static class CacheEntry {
    String gacv;
    ResolutionOptions options;
    List<Artifact> artifacts = new ArrayList<>();

    long insertionTime;

    public CacheEntry() {
    }

    public List<Artifact> getArtifacts() {
      return artifacts;
    }

    public CacheEntry setArtifacts(List<Artifact> artifacts) {
      this.artifacts = artifacts;
      return this;
    }

    public String getGacv() {
      return gacv;
    }

    public CacheEntry setGacv(String gacv) {
      this.gacv = gacv;
      return this;
    }

    public long getInsertionTime() {
      return insertionTime;
    }

    public CacheEntry setInsertionTime(long insertionTime) {
      this.insertionTime = insertionTime;
      return this;
    }

    public ResolutionOptions getOptions() {
      return options;
    }

    public CacheEntry setOptions(ResolutionOptions options) {
      this.options = options;
      return this;
    }
  }
}
