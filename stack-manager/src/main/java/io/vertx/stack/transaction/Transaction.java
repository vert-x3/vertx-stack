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

/**
 * Represents a transaction. Be aware that it's not ACID transaction. It just postpones the execution of action until
 * all the stack is resolved. It avoids starting modifying the stack when a conflict is detected.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public interface Transaction {

  /**
   * Creates an instance of {@link Transaction} using the default implementation.
   *
   * @return the created {@link Transaction}.
   */
  static Transaction create() {
    return new TransactionImpl();
  }

  /**
   * Appends an action to the transaction.
   *
   * @param action the action
   * @return the current {@link Transaction} instance
   */
  Transaction append(Action action);

  /**
   * Executes the action contained in the transaction. The execution are executed in the insertion order.
   *
   * @throws Exception if an action failed.
   */
  void apply() throws Exception;
}
