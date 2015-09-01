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

package io.vertx.stack.builder;

import java.io.File;

/**
 * Main class to launch the builder.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Main {

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
      return;
    }

    StackBuilder builder = new StackBuilder();
    if (args[0].endsWith(".xml")) {
      builder.fromXmlFile(new File(args[0])).build();
    } else if (args[0].endsWith(".json")) {
      builder.fromJsonFile(new File(args[0])).build();
    } else if (args[0].endsWith(".yaml")) {
      builder.fromYamlFile(new File(args[0])).build();
    } else {
      System.err.println("Unknown file type : " + args[0]);
      printUsage();
    }

  }

  private static void printUsage() {
    System.err.println("Usage: java -jar stack-builder-standalone.jar FILE");
    System.err.println("Supported file type: XML, JSON and YAML");
  }

}
