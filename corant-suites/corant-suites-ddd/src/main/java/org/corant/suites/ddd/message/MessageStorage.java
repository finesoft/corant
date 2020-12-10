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

import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午6:19:12
 *
 */
@InfrastructureServices
public interface MessageStorage extends UnaryOperator<Message> {

  MessageStorage DUMMY_INST = new MessageStorage() {
    final transient Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public Message apply(Message t) {
      logger.fine(
          () -> "The message stroage is an empty implementation that does not really implement apply");
      return t;
    }
  };

  static MessageStorage empty() {
    return DUMMY_INST;
  }

  default void prepare() {}
}
