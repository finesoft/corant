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
package org.corant.modules.ddd.shared.unitwork;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.areEqual;
import java.time.Instant;
import java.util.Queue;
import java.util.logging.Logger;
import org.corant.modules.ddd.MergableMessage;
import org.corant.modules.ddd.Message;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午5:00:15
 *
 */
public final class WrappedMessage implements Comparable<WrappedMessage> {

  static final Logger logger = Logger.getLogger(WrappedMessage.class.getName());

  final Message delegate;
  final Instant wrappedTime;
  final Object source;

  public WrappedMessage(Message delegate) {
    this(delegate, Instant.now(), null);
  }

  public WrappedMessage(Message delegate, Instant wrappedTime, Object source) {
    this.delegate = shouldNotNull(delegate);
    this.wrappedTime = wrappedTime;
    this.source = source;
  }

  public WrappedMessage(Message delegate, Object source) {
    this(delegate, Instant.now(), source);
  }

  public static boolean isCorrelated(WrappedMessage m, WrappedMessage o) {
    return m.delegate instanceof MergableMessage && o.delegate instanceof MergableMessage
        && areEqual(m.delegate.getClass(), o.delegate.getClass())
        && areEqual(m.getSource(), o.getSource());
  }

  public static void mergeToQueue(Queue<WrappedMessage> queue, WrappedMessage newMsg) {
    if (newMsg.delegate instanceof MergableMessage) {
      WrappedMessage oldMgbMsg = null;
      for (WrappedMessage queMsg : queue) {
        if (isCorrelated(queMsg, newMsg)) {
          oldMgbMsg = queMsg;
          break;
        }
      }
      final MergableMessage order = oldMgbMsg == null ? null : (MergableMessage) oldMgbMsg.delegate;
      final MergableMessage newer = (MergableMessage) newMsg.delegate;
      if (order == null || !newer.canMerge(order)) {
        logger.fine(() -> String.format("Enqueue message %s.", newer));
        queue.add(newMsg);
      } else {
        logger.fine(() -> String.format("Remove message %s from queue.", order));
        queue.remove(oldMgbMsg);
        if (newer.merge(order).isValid()) {
          logger.fine(() -> String.format("Merge message %s to %s and enqueue it.", order, newer));
          queue.add(newMsg);
        }
      }
    } else if (newMsg != null) {
      logger.fine(() -> String.format("Enqueue message %s.", newMsg.delegate));
      queue.add(newMsg);
    }
  }

  @Override
  public int compareTo(WrappedMessage o) {
    return wrappedTime.compareTo(o.wrappedTime);
  }

  public Message getDelegate() {
    return delegate;
  }

  public Instant getRaisedTime() {
    return wrappedTime;
  }

  public Object getSource() {
    return source;
  }

}
