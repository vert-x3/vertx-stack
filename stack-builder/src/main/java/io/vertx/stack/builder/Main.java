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
