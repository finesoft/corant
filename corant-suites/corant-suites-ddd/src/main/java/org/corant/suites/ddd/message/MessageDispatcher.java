/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.ddd.message;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午6:17:36
 *
 */
@FunctionalInterface
public interface MessageDispatcher extends Consumer<Message[]> {

  MessageDispatcher DUMMY_INST = new MessageDispatcher() {
    transient Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public void accept(Message[] t) {
      logger.fine(
          () -> "The message dispatch is an empty implementation that does not really implement accept");
    }
  };

  static MessageDispatcher empty() {
    return DUMMY_INST;
  }

  default void prepare() {}

}
