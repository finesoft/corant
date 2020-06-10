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

import static java.lang.invoke.MethodType.methodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
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
  private static final MethodHandle INVOKE_CLEANER;
  private static final MethodHandle GET_CLEANER;
  private static final MethodHandle CLEAN;

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
    try {
      MethodHandle invokeCleaner = null;
      MethodHandle getCleaner = null;
      MethodHandle clean = null;
      final MethodHandles.Lookup lookup = MethodHandles.lookup();
      try {
        invokeCleaner = lookup.findVirtual(UNSAFE.getClass(), "invokeCleaner",
            methodType(void.class, ByteBuffer.class));
      } catch (NoSuchMethodException ex) {
        // for JDK 8
        final Class<?> directBuffer = Class.forName("sun.nio.ch.DirectBuffer");
        final Class<?> cleaner = Class.forName("sun.misc.Cleaner");
        getCleaner = lookup.findVirtual(directBuffer, "cleaner", methodType(cleaner));
        clean = lookup.findVirtual(cleaner, "clean", methodType(void.class));
      }
      INVOKE_CLEANER = invokeCleaner;
      GET_CLEANER = getCleaner;
      CLEAN = clean;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void free(final ByteBuffer buffer) {
    if (null != buffer && buffer.isDirect()) {
      try {
        if (null != INVOKE_CLEANER) {
          // for JDK 9+
          INVOKE_CLEANER.invokeExact(UNSAFE, buffer);
        } else {
          // for JDK 8
          final Object cleaner = GET_CLEANER.invoke(buffer);
          if (null != cleaner) {
            CLEAN.invoke(cleaner);
          }
        }
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  public static Unsafe get() {
    return UNSAFE;
  }
}
