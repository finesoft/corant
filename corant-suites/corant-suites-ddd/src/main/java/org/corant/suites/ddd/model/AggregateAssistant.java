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
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;

/**
 * corant-suites-ddd
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
  void fireAsyncEvent(Event event, Annotation... qualifiers);

  /**
   * fire aggregate event
   */
  void fireEvent(Event event, Annotation... qualifiers);

  /**
   * The aggregate which it serve
   */
  Aggregate getAggregate();

  /**
   * Obtain the message serial number
   */
  long getMessageSequenceNumber();
}
