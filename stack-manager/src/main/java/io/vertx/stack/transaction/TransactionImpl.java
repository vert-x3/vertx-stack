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

package io.vertx.stack.transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link Transaction}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class TransactionImpl implements Transaction {

  private List<Action> actions = new ArrayList<>();

  @Override
  public Transaction append(Action action) {
    actions.add(action);
    return this;
  }

  @Override
  public void apply() throws Exception {
    for (Action action : actions) {
      action.execute();
    }
  }
}
