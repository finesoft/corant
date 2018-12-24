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
package org.corant.asosat.ddd.util;

import java.lang.annotation.Annotation;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

/**
 * @author bingo 下午7:06:13
 *
 */
public class DefaultContext {

  static CDI<Object> CTX;
  static {
    synchronized (DefaultContext.class) {
      try {
        CTX = CDI.current();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static <U> U bean(Class<U> subtype, Annotation... qualifiers) {
    if (CTX != null) {
      Instance<U> inst = CTX.select(subtype, qualifiers);
      if (inst.isResolvable()) {
        return inst.get();
      }
    }
    return null;
  }

  public static CDI<Object> current() {
    return CTX;
  }

  public static Event<Object> event() {
    return CTX.getBeanManager().getEvent();
  }

  public static void fireAsyncEvent(org.corant.asosat.ddd.event.Event event,
      Annotation... qualifiers) {
    if (event != null) {
      if (qualifiers.length > 0) {
        event().select(qualifiers).fireAsync(event);
      } else {
        event().fireAsync(event);
      }
    }
  }

  public static void fireEvent(org.corant.asosat.ddd.event.Event event, Annotation... qualifiers) {
    if (event != null) {
      if (qualifiers.length > 0) {
        event().select(qualifiers).fire(event);
      } else {
        event().fire(event);
      }
    }
  }

}
