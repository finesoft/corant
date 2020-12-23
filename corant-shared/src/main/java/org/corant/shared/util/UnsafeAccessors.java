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
import org.corant.shared.exception.CorantRuntimeException;
import sun.misc.Unsafe;

/**
 *
 * @author bingo 下午7:00:37
 *
 */
public class UnsafeAccessors {

  private static final Unsafe UNSAFE;
  private static final MethodHandle UNSAFE_INVOKE_CLEANER;
  private static final MethodHandle DIRECT_BUFFER_CLEANER;
  private static final MethodHandle CLEANER_CLEAN;

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
      MethodHandle unsafeInvokeCleaner = null;
      MethodHandle directBufferCleaner = null;
      MethodHandle cleanerClean = null;
      final MethodHandles.Lookup lookup = MethodHandles.lookup();
      try {
        // for JDK 9+
        unsafeInvokeCleaner = lookup.findVirtual(UNSAFE.getClass(), "invokeCleaner",
            methodType(void.class, ByteBuffer.class));
      } catch (NoSuchMethodException ex) {
        // for JDK 8
        final Class<?> directBuffer = Class.forName("sun.nio.ch.DirectBuffer");
        final Class<?> cleaner = Class.forName("sun.misc.Cleaner");
        directBufferCleaner = lookup.findVirtual(directBuffer, "cleaner", methodType(cleaner));
        cleanerClean = lookup.findVirtual(cleaner, "clean", methodType(void.class));
      }
      UNSAFE_INVOKE_CLEANER = unsafeInvokeCleaner;
      DIRECT_BUFFER_CLEANER = directBufferCleaner;
      CLEANER_CLEAN = cleanerClean;
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  public static void free(final ByteBuffer buffer) {
    if (null != buffer && buffer.isDirect()) {
      try {
        if (null != UNSAFE_INVOKE_CLEANER) {
          // for JDK 9+
          UNSAFE_INVOKE_CLEANER.invokeExact(UNSAFE, buffer);
        } else {
          // for JDK 8
          final Object cleaner = DIRECT_BUFFER_CLEANER.invoke(buffer);
          if (null != cleaner) {
            CLEANER_CLEAN.invoke(cleaner);
          }
        }
      } catch (Throwable throwable) {
        throw new CorantRuntimeException(throwable);
      }
    }
  }

  public static Unsafe get() {
    return UNSAFE;
  }
}
