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

/**
 * corant-suites-ddd
 *
 * @author bingo 下午7:28:16
 *
 */
public class MessageUtils {

  public static boolean isCorrelated(Message m, Message o) {
    return m instanceof MergableMessage && o instanceof MergableMessage
        && isEquals(m.getClass(), o.getClass())
        && isEquals(m.getMetadata().getSource(), o.getMetadata().getSource());
  }

  public static void mergeToQueue(Queue<Message> queue, Message msg) {
    if (msg instanceof MergableMessage) {
      MergableMessage newMgbMsg = (MergableMessage) msg;
      MergableMessage oldMgbMsg = null;
      for (Message queMsg : queue) {
        if (isCorrelated(queMsg, newMgbMsg)) {
          oldMgbMsg = (MergableMessage) queMsg;
          break;
        }
      }
      if (oldMgbMsg == null || !newMgbMsg.canMerge(oldMgbMsg)) {
        queue.add(newMgbMsg);
      } else {
        queue.remove(oldMgbMsg);
        if (newMgbMsg.merge(oldMgbMsg).isValid()) {
          queue.add(newMgbMsg);
        }
      }
    } else if (msg != null) {
      queue.add(msg);
    }
  }
}
