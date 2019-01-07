/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.message;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.suites.ddd.annotation.qualifier.MQ;
import org.corant.suites.ddd.annotation.stereotype.Messages;

/**
 * @author bingo 下午1:50:27
 *
 */
public abstract class MessageUtils {

  public static String extractMessageQueue(Class<?> cls) {
    MQ mt = cls.getAnnotation(MQ.class);
    if (mt != null) {
      return mt.value();
    }
    return null;
  }

  public static Set<String> extractMessageQueues(Class<?> cls) {
    Set<String> queues = new LinkedHashSet<>();
    Messages mt = cls.getAnnotation(Messages.class);
    if (mt != null) {
      Arrays.stream(mt.queues()).forEach(queues::add);
    }
    return queues;
  }

}
