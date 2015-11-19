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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FileUtils {

  public static void write(File file, String content) {
    try {
      Files.write(file.toPath(), content.getBytes());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void deleteQuietly(File file) {
    try {
      Files.deleteIfExists(file.toPath());
    } catch (Exception e) {
      // Ignore it.
    }
  }

  public static void copyFile(File source, File target) {
    Path s = source.toPath();
    Path t = target.toPath();
    try {
      Files.copy(s, t, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getExtension(File file) {
    int index = file.getName().lastIndexOf(".");
    if (index == -1) {
      return "";
    } else {
      return file.getName().substring(index + 1);
    }
  }

  public static void delete(File file) {
    if (file.isFile()) {
      deleteQuietly(file);
    } else if (file.isDirectory()) {
      Path directory = file.toPath();
      try {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
