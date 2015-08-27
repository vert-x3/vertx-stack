package io.vertx.stack.builder.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Class responsible to retrieve the default vert.x stack version.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StackVersion {

  public static final String VERSION;

  static {
    try (InputStream is = StackVersion.class.getClassLoader().getResourceAsStream("vertx-stack-version.txt")) {
      if (is == null) {
        throw new IllegalStateException("Cannot find vertx-stack-version.txt on classpath");
      }
      try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
        VERSION = (scanner.hasNext() ? scanner.next() : "");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

}
