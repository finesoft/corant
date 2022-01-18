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

import static org.corant.config.Configs.assemblyStringConfigProperty;
import static org.corant.shared.util.Assertions.shouldNotEquals;
import static org.corant.shared.util.Conversions.toDouble;
import static org.corant.shared.util.Conversions.toDuration;
import static org.corant.shared.util.Conversions.toEnum;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.context.concurrent.annotation.Asynchronous;
import org.corant.shared.retry.BackoffStrategy;
import org.corant.shared.retry.BackoffStrategy.BackoffAlgorithm;
import org.corant.shared.retry.BackoffStrategy.BackoffStrategyBuilder;
import org.corant.shared.retry.RetryStrategy;
import org.corant.shared.retry.RetryStrategy.MaxAttemptsRetryStrategy;
import org.corant.shared.retry.RetryStrategy.ThrowableClassifierRetryStrategy;
import org.corant.shared.retry.RetryStrategy.TimeoutRetryStrategy;
import org.corant.shared.util.Classes;

/**
 * corant-context
 *
 * @author bingo 上午10:46:41
 *
 */
public class AsynchronousConfig {

  final BackoffStrategy backoffStrategy;

  final RetryStrategy retryStrategy;

  final boolean retry;

  @SuppressWarnings("unchecked")
  public AsynchronousConfig(Asynchronous ann) {
    int retryAttempts = isNotBlank(ann.maxAttempts())
        ? toInteger(assemblyStringConfigProperty(ann.maxAttempts())).intValue() + 1
        : 0;
    if (retryAttempts > 0) {
      BackoffAlgorithm backoffAlgo =
          toEnum(assemblyStringConfigProperty(ann.backoffStrategy()), BackoffAlgorithm.class);
      shouldNotEquals(backoffAlgo, BackoffAlgorithm.EXPO_DECORR,
          "Can't support EXPO_DECORR backoff algorithm on asynchronous method!");
      backoffStrategy = new BackoffStrategyBuilder().algorithm(backoffAlgo)
          .baseDuration(toDuration(assemblyStringConfigProperty(ann.baseBackoffDuration())))
          .maxDuration(toDuration(assemblyStringConfigProperty(ann.maxBackoffDuration())))
          .factor(toDouble(assemblyStringConfigProperty(ann.backoffFactor()))).build();
      RetryStrategy root = new MaxAttemptsRetryStrategy(retryAttempts);
      final Set<Class<? extends Throwable>> abortOn = new LinkedHashSet<>();
      final Set<Class<? extends Throwable>> retryOn = new LinkedHashSet<>();
      for (String abortOnCls : ann.abortOn()) {
        if (isNotBlank(abortOnCls)) {
          abortOn.add((Class<? extends Throwable>) Classes
              .asClass(assemblyStringConfigProperty(abortOnCls)));
        }
      }
      for (String retryOnCls : ann.retryOn()) {
        if (isNotBlank(retryOnCls)) {
          retryOn.add((Class<? extends Throwable>) Classes
              .asClass(assemblyStringConfigProperty(retryOnCls)));
        }
      }
      root = root.and(new ThrowableClassifierRetryStrategy(retryOn, abortOn));
      if (isNotBlank(ann.timeout())) {
        root = root.and(new TimeoutRetryStrategy()
            .timeout(toDuration(assemblyStringConfigProperty(ann.timeout()))));
      }
      retryStrategy = root;
      retry = true;
    } else {
      backoffStrategy = BackoffStrategy.NON_BACKOFF_STRATEGY;
      retryStrategy = RetryStrategy.NON_RETRY_STRATEGY;
      retry = false;
    }
  }

  public BackoffStrategy getBackoffStrategy() {
    return backoffStrategy;
  }

  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  public boolean isRetry() {
    return retry;
  }

}
