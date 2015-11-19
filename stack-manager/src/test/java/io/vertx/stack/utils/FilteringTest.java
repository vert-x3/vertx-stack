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

import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FilteringTest {

  @Test
  public void testFilteringWithoutVariables() {
    String out = Filtering.filter("hello", Collections.emptyMap());
    assertThat(out).isEqualTo("hello");
  }

  @Test
  public void testFilteringWithoutPlaceholders() {
    String out = Filtering.filter("hello", new FluentMap<>().add("key", "value"));
    assertThat(out).isEqualTo("hello");
  }

  @Test
  public void testFilteringWithNonMatchingVariables() {
    String out = Filtering.filter("hello ${nope}", new FluentMap<>().add("key", "value"));
    assertThat(out).isEqualTo("hello ${nope}");
  }

  @Test
  public void testFilteringWithReplacement() {
    String out = Filtering.filter("hello ${world}", new FluentMap<>().add("world", "vert.x"));
    assertThat(out).isEqualTo("hello vert.x");
  }

  @Test
  public void testFilteringWithPartialReplacement() {
    String out = Filtering.filter("hello ${world} ${period}", new FluentMap<>().add("world", "vert.x"));
    assertThat(out).isEqualTo("hello vert.x ${period}");
  }

  @Test
  public void testFilteringWithNestedReplacement() {
    String out = Filtering.filter("hello ${world} ${period}", new FluentMap<>()
        .add("name", "vert.x")
        .add("world", "${name}"));
    assertThat(out).isEqualTo("hello vert.x ${period}");
  }

  @Test
  public void testFilteringWithUnfinishedVariables() {
    String out = Filtering.filter("hello ${world} ${per ", new FluentMap<>()
        .add("name", "vert.x")
        .add("world", "${name}"));
    assertThat(out).isEqualTo("hello vert.x ${per ");
  }

}