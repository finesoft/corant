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
package org.corant.shared.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.misc.Unsafe;

/**
 *
 * @author bingo 下午7:00:37
 *
 */
@SuppressWarnings("restriction")
public class UnsafeAccessors {

  private static Unsafe UNSAFE = null;

  static {
    UNSAFE = AccessController.doPrivileged((PrivilegedAction<Unsafe>) () -> {
      try {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
  }

  public static Unsafe get() {
    return UNSAFE;
  }
}
