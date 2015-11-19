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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class LocalRepoBuilder {

  private final static Logger LOGGER = Logger.getLogger(LocalRepoBuilder.class.getName());

  private final File output;
  private List<LocalArtifact> artifacts = new ArrayList<>();

  public LocalRepoBuilder(File output) {
    this.output = output;
  }

  public LocalRepoBuilder addArtifact(LocalArtifact artifact) {
    this.artifacts.add(artifact);
    return this;
  }

  public void build() {
    build(true);
  }

  public void build(boolean clean) {
    if (output.isDirectory() && clean) {
      LOGGER.info("Deleting " + output.getAbsolutePath());
      FileUtils.deleteQuietly(output);
      output.mkdirs();
    }

    // Install all artifacts
    artifacts.stream().forEach(artifact -> {
      File directory = artifact.getArtifactDirectory(output);
      directory.mkdirs();

      // Copy pom
      File pom = artifact.getPomFile(output);
      FileUtils.write(pom, artifact.toPom());

      // Copy files
      for (Map.Entry<String, File> entry : artifact.getFiles().entrySet()) {
        if (entry.getKey().equals(LocalArtifact.MAIN_ARTIFACT_CLASSIFIER)) {
          FileUtils.copyFile(entry.getValue(), artifact.getFile(output));
        } else {
          FileUtils.copyFile(entry.getValue(), artifact.getFile(entry.getKey(), output));
        }
      }

      LOGGER.info("The artifact " + artifact.gav() + " has been installed in the local repository");
    });
  }


}
