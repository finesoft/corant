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
package org.corant.suites.ddd.model;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.unitwork.UnitOfWork;

/**
 * corant-suites-ddd
 *
 * <p>
 * The aggregate assistant, use for help the aggregate to emit events or maintain the messages. The
 * aggregate should focus on core domain logic, the other work can use this to handle.
 * </p>
 *
 * @author bingo 下午3:11:06
 *
 */
public interface AggregateAssistant {

  /**
   * Clear the message queue.
   */
  void clearMessages();

  /**
   * The current unit of work or null
   *
   * @return currentUnitOfWork
   */
  Optional<? extends UnitOfWork> currentUnitOfWork();

  /**
   * Obtain the message queue, if flush is true then clear queue.
   */
  List<Message> dequeueMessages(boolean flush);

  /**
   * enqueue aggregate message to queue
   */
  void enqueueMessages(Message... messages);

  /**
   * fire aggregate asynchronous event
   */
  <U extends Event> CompletionStage<U> fireAsyncEvent(U event, Annotation... qualifiers);

  /**
   * fire aggregate event
   */
  void fireEvent(Event event, Annotation... qualifiers);

  /**
   * The aggregate which it serve
   */
  Aggregate getAggregate();

}
