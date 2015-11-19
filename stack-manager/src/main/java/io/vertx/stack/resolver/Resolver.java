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

package io.vertx.stack.resolver;

import org.eclipse.aether.artifact.Artifact;

import java.util.List;

/**
 * Interface implemented by resolver. Resolvers are responsible for the resolution of the dependencies.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public interface Resolver {

  /**
   * Resolves a dependency.
   *
   * @param dependency the dependency, using the GACV form.
   * @param options    the resolution options
   * @return the list of resolved artifacts. The list contains a single element if the transitive dependencies are
   * not resolved. The first artifact of the list if the artifact for the given dependency.
   */
  List<Artifact> resolve(String dependency, ResolutionOptions options);

  /**
   * Creates a {@link Resolver} using the default implementation and default options.
   *
   * @return the created {@link Resolver}.
   */
  static Resolver create() {
    return new ResolverImpl(new ResolverOptions());
  }

  /**
   * Creates a {@link Resolver} using the default implementation and the given options.
   *
   * @param options the resolver options
   * @return the created {@link Resolver}.
   */
  static Resolver create(ResolverOptions options) {
    return new ResolverImpl(options);
  }
}
