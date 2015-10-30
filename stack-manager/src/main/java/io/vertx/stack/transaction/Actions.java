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

package io.vertx.stack.transaction;

import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Common {@link Action} implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Actions {

  private static final Logger logger = Logger.getLogger("Stack Resolver");

  /**
   * Action to copy an artifact to a directory.
   *
   * @param artifact  the artifact
   * @param directory the directory
   * @return the created {@link Action}
   */
  public static Action copy(Artifact artifact, File directory) {
    return () -> {
      Path source = artifact.getFile().toPath();
      Path output = directory.toPath().resolve(source.getFileName());
      logger.info("Copying " + source.getFileName());
      Files.copy(source, output);
    };
  }

  /**
   * Action denoting that nothing needs to be done for the given artifact.
   *
   * @param artifact the artifact
   * @return the created {@link Action}
   */
  public static Action skip(Artifact artifact) {
    return () -> logger.info("Skipping " + artifact.toString());
  }

  /**
   * Action removing a file.
   *
   * @param file the file to be removed
   * @return the created {@link Action}
   */
  public static Action remove(File file) {
    return () -> {
      if (file.isFile()) {
        logger.info("Deleting " + file.getName());
        file.delete();
      }
    };
  }

}
