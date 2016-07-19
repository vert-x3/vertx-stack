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

package io.vertx.stack.docker;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.assertEquals;

/**
 * This test checks that the docker container use the expected Vert.x version.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VersionIT {

  private static final String versionExpected = System.getProperty("vertx.version");

  @Test
  public void testVersionOfRegularContainer() {
    String containerUrl = System.getProperty("vertx.url");

    RestAssured.baseURI = containerUrl;
    RestAssured.defaultParser = Parser.JSON;
    System.out.println("Checking URL: " + containerUrl);

    String version = get("/version").asString();
    System.out.println(version);
    assertEquals(versionExpected, version);
  }

  @Test
  public void testVersionOfAlpineContainer() {
    String containerUrl = System.getProperty("vertx-alpine.url");

    RestAssured.baseURI = containerUrl;
    RestAssured.defaultParser = Parser.JSON;
    System.out.println("Checking URL (Alpine): " + containerUrl);

    String version = get("/version").asString();
    System.out.println(version);
    assertEquals(versionExpected, version);
  }

  @Test
  public void testVersionOfRegularExecContainer() {
    String containerUrl = System.getProperty("vertx-exec.url");

    RestAssured.baseURI = containerUrl;
    RestAssured.defaultParser = Parser.JSON;
    System.out.println("Checking URL: " + containerUrl);

    String version = get("/version").asString();
    System.out.println(version);
    assertEquals(versionExpected, version);
  }

  @Test
  public void testVersionOfAlpineExecContainer() {
    String containerUrl = System.getProperty("vertx-exec-alpine.url");

    RestAssured.baseURI = containerUrl;
    RestAssured.defaultParser = Parser.JSON;
    System.out.println("Checking URL: " + containerUrl);

    String version = get("/version").asString();
    System.out.println(version);
    assertEquals(versionExpected, version);
  }

}
