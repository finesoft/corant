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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * corant-shared
 *
 * @author bingo 上午11:30:02
 *
 */
public class ThreadLocals {

  /**
   * corant-shared
   *
   * @author bingo 上午11:30:23
   *
   */
  public static class ThreadLocalStack<T> {

    private final ThreadLocal<Deque<T>> local = new ThreadLocal<>();

    public void clear() {
      local.remove();
    }

    public boolean isEmpty() {
      Deque<T> stack = stack(false);
      return stack == null || stack.isEmpty();
    }

    public T peek() {
      Deque<T> stack = local.get();
      if (stack == null || stack.isEmpty()) {
        return null;
      }
      return stack.peek();
    }

    public T pop() {
      Deque<T> stack = local.get();
      if (stack == null || stack.isEmpty()) {
        return null;
      }
      return stack.pop();
    }

    public void push(T obj) {
      stack(true).push(obj);
    }

    public int size() {
      Deque<T> stack = stack(false);
      return stack == null ? 0 : stack.size();
    }

    private Deque<T> stack(boolean create) {
      Deque<T> stack = local.get();
      if (stack == null && create) {
        stack = new ArrayDeque<>();
        local.set(stack);
      }
      return stack;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午11:31:21
   *
   */
  public static class WeakThreadLocal<T> implements Supplier<T> {
    private final ThreadLocal<WeakReference<T>> cache;
    private final Supplier<T> supplier;

    public WeakThreadLocal(Supplier<T> supplier) {
      this.cache = new ThreadLocal<>();
      this.supplier = shouldNotNull(supplier, "The value supplier can't null!");
    }

    public static <T> WeakThreadLocal<T> withSupplier(Supplier<T> supplier) {
      return new WeakThreadLocal<>(supplier);
    }

    @Override
    public T get() {
      T value = null;
      WeakReference<T> ref = cache.get();
      if (ref != null) {
        value = ref.get();
      }
      if (value == null) {
        value = supplier.get();
        cache.set(new WeakReference<>(value));
      }
      return value;
    }
  }
}
