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
package org.corant.context.concurrent;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

/**
 * corant-context
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, if there is any infringement,
 * please inform me(finesoft@gmail.com) </b>
 * <p>
 * Asynchronously processed wrapper of an injectable reference. Can be used to obtain an injectable
 * reference of a bean whose creation involves potentially blocking operations:
 *
 * <pre>
 * &#64;ApplicationScoped
 * class Hello {
 *
 *   &#64;Inject
 *   AsynchronousReference&lt;ServiceWithBlockingInit&gt; service;
 *
 *   CompletionStage&lt;String&gt; hello() {
 *     return service.thenApply((s) -> "Hello" + s.getName() + "!");
 *   }
 * }
 * </pre>
 *
 *
 * <p>
 * No method in this interface waits for completion. This interface also implements
 * {@link CompletionStage} with an injectable reference as the result.
 * </p>
 */
public interface AsynchronousReference<T> extends CompletionStage<T> {

  /**
   * Gets the cause in case of a failure occurs during processing.
   *
   * @return the cause or {@code null} if processed sucessfully
   */
  Throwable cause();

  /**
   * Gets the reference.
   *
   * @return the reference, might be {@code null}
   */
  T get();

  /**
   * If {@link #isDone()} returns <code>true</code>, invoke the specified consumer with the
   * reference (may be {@code null}) and the exception (or {@code null} if processed successfully).
   *
   * @param consumer
   */
  default void ifDone(BiConsumer<? super T, ? super Throwable> consumer) {
    if (isDone()) {
      consumer.accept(get(), cause());
    }
  }

  /**
   *
   * @return {@code true} if an injectable reference was obtained, {@code false} otherwise
   */
  boolean isDone();

  /**
   * Gets the reference or the default value.
   *
   * @param defaultValue
   * @return the reference or the default value if the reference is {@code null}
   */
  default T orElse(T defaultValue) {
    T value = get();
    return value != null ? value : defaultValue;
  }
}
