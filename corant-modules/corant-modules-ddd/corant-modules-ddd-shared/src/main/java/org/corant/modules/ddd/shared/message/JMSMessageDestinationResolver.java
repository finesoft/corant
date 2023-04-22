/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.ddd.shared.message;

import java.util.function.BiFunction;
import jakarta.jms.JMSContext;
import org.corant.modules.ddd.Message;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The message pre-dispatch hander use to configure the message properties purpose for message
 * consumer to select messages.
 *
 * @author bingo 下午5:57:40
 *
 */
@FunctionalInterface
public interface JMSMessageDestinationResolver
    extends BiFunction<JMSContext, Message, jakarta.jms.Destination>, Sortable {

  default boolean supports(Message message) {
    return false;
  }
}
