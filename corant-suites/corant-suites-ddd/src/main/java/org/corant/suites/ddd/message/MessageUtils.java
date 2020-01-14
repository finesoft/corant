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

import static org.corant.shared.util.ObjectUtils.isEquals;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午7:28:16
 *
 */
public class MessageUtils {

  static final Logger logger = Logger.getLogger(MessageUtils.class.getName());

  public static boolean isCorrelated(Message m, Message o) {
    return m instanceof MergableMessage && o instanceof MergableMessage
        && isEquals(m.getClass(), o.getClass())
        && isEquals(m.getMetadata().getSource(), o.getMetadata().getSource());
  }

  public static void mergeToQueue(Queue<Message> queue, Message newMsg) {
    if (newMsg instanceof MergableMessage) {
      MergableMessage oldMgbMsg = null;
      for (Message queMsg : queue) {
        if (isCorrelated(queMsg, newMsg)) {
          oldMgbMsg = (MergableMessage) queMsg;
          break;
        }
      }
      final MergableMessage order = oldMgbMsg;
      if (order == null || !((MergableMessage) newMsg).canMerge(order)) {
        logger.fine(() -> String.format("Enqueue message %s", newMsg.getMetadata()));
        queue.add(newMsg);
      } else {
        logger.fine(() -> String.format("Remove message %s from queue.", order.getMetadata()));
        queue.remove(order);
        if (((MergableMessage) newMsg).merge(order).isValid()) {
          logger.fine(() -> String.format("Merge message %s to %s and enqueue it.",
              order.getMetadata(), newMsg.getMetadata()));
          queue.add(newMsg);
        }
      }
    } else if (newMsg != null) {
      logger.fine(() -> String.format("Enqueue message %s", newMsg.getMetadata()));
      queue.add(newMsg);
    }
  }
}
