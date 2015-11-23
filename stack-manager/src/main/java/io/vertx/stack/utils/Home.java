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

/**
 * Helper method to find vert.x home.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Home {

  /**
   * Try to find the home of vert.x using the `VERTX_HOME` env variable or the `vertx.home` system property.
   *
   * @return the found directory, {@code null} if not set.
   */
  public static File getVertxHome() {
    // System property ?
    String home = System.getProperty("vertx.home");
    if (home != null) {
      return new File(home);
    }
    // Environment variable
    home = System.getenv("VERTX_HOME");
    if (home != null) {
      return new File(home);
    }
    return null;
  }
}
