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

import java.util.Map;

/**
 * Utility class to handle Maven-like filtering.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Filtering {

  private Filtering() {
    // Avoid direct instantiation
  }

  /**
   * Filters the given input using the given set of variables.
   *
   * @param input     the input
   * @param variables the variables
   * @return the modified input
   */
  public static String filter(String input, Map<String, String> variables) {
    if (variables.isEmpty()) {
      return input;
    }

    if (input == null || input.isEmpty()) {
      return input;
    }

    String current = input;
    String last = input;
    while (current.contains("${") && current.contains("}")) {

      // Replace all variables
      for (Map.Entry<String, String> entry : variables.entrySet()) {
        current = current.replace("${" + entry.getKey() + "}", entry.getValue());
      }

      // If we didn't change anything, just leave.
      if (last.equals(current)) {
        return current;
      }

      last = current;
    }

    return current;
  }

}
