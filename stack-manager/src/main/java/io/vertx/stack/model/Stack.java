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

package io.vertx.stack.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.stack.utils.Filtering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents a stack. A stack is composed by a set of dependencies and variables.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Stack {

  private List<Dependency> dependencies = new ArrayList<>();

  private Map<String, String> variables = new LinkedHashMap<>();

  /**
   * Adds a new variables or updates the value of an existing one.
   *
   * @param key   the variable name
   * @param value the value
   * @return the current {@link Stack} instance
   */
  public Stack addVariable(String key, String value) {
    variables.put(key, value);
    return this;
  }

  /**
   * Removes a variable.
   *
   * @param key the variable name
   * @return the current {@link Stack} instance
   */
  public Stack removeVariable(String key) {
    variables.remove(key);
    return this;
  }

  /**
   * Adds a dependency.
   *
   * @param dependency the dependency
   * @return the current {@link Stack} instance
   */
  public Stack addDependency(Dependency dependency) {
    dependencies.add(dependency);
    return this;
  }

  /**
   * Removes a dependency.
   *
   * @param dependency the dependency
   * @return the current {@link Stack} instance
   */
  public Stack removeDependency(Dependency dependency) {
    dependencies.remove(dependency);
    return this;
  }

  /**
   * Gets the variables.
   *
   * @return the variables, empty if none.
   */
  public Map<String, String> getVariables() {
    return variables;
  }

  /**
   * Apply filtering on the set of dependencies.
   */
  public void applyFiltering() {
    // Compute the final set of properties.
    // First the properties, Then the system properties, so you can override a value using -D.
    Map<String, String> properties = new LinkedHashMap<>();
    properties.putAll(variables);
    properties.putAll((Map) System.getProperties());

    dependencies.stream().forEach(dependency -> {
      dependency.setGroupId(Filtering.filter(dependency.getGroupId(), properties));
      dependency.setArtifactId(Filtering.filter(dependency.getArtifactId(), properties));
      dependency.setVersion(Filtering.filter(dependency.getVersion(), properties));
      dependency.setClassifier(Filtering.filter(dependency.getClassifier(), properties));
      dependency.setType(Filtering.filter(dependency.getType(), properties));
    });
  }

  /**
   * Gets the dependencies, empty if none.
   *
   * @return the dependencies
   */
  public Stream<Dependency> getDependencies() {
    return dependencies.stream();
  }

  /**
   * Reads a stack descriptor.
   *
   * @param descriptor the descriptor, must be a valid YAML file.
   * @return the created stack
   */
  public static Stack fromDescriptor(File descriptor) {
    ObjectMapper mapper = new ObjectMapper();
    mapper = mapper
        .enable(JsonParser.Feature.ALLOW_COMMENTS)
        .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
        .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    try {
      return mapper.readValue(descriptor, Stack.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load stack from " + descriptor.getAbsolutePath(), e);
    }
  }
}
