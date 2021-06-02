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
package org.corant.modules.jms.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.inject.Stereotype;
import javax.jms.Session;
import org.corant.shared.util.Retry.BackoffAlgorithm;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 下午2:39:22
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD})
@Inherited
@Stereotype
public @interface MessageDriven {

  /**
   * @see Session#AUTO_ACKNOWLEDGE
   * @see Session#CLIENT_ACKNOWLEDGE
   * @see Session#DUPS_OK_ACKNOWLEDGE
   * @see Session#SESSION_TRANSACTED
   *
   * @return acknowledge
   */
  int acknowledge() default Session.CLIENT_ACKNOWLEDGE;

  /**
   * Returns the Backoff Algorithm
   */
  BackoffAlgorithm brokenBackoffAlgo() default BackoffAlgorithm.NONE;

  /**
   * The broken backoff factor, for Exponential backoff + jitter algorithm to compute the delay. The
   * value of backoff factor must greater then 1.0, default is 0 means do not enable Exponential
   * backoff + jitter algorithm.
   *
   * @return brokenBackoff
   */
  double brokenBackoffFactor() default 2.0;

  /**
   * The broken duration, if exceeds then start try mode.
   *
   * @return brokenDuration
   */
  String brokenDuration() default "PT5M";

  /**
   * Marks whether a connection or session is cached.
   *
   * <pre>
   * Value of 0 represents Not cache
   * Value of 1 represents cache connection
   * Value of 2 represents cache session
   * Value of 3 represents cache message consumer
   * </pre>
   *
   * @return cacheLevel
   */
  int cacheLevel() default 3;

  /**
   * The failure threshold, if exceeds then start break mode
   *
   * @return failureThreshold
   */
  int failureThreshold() default 16;

  /**
   * When the task breaks itself, the scheduler can still schedule the task, but the task execution
   * only sleeps for a short time and then returns directly. The sleep short time is loopIntervalMs
   *
   * @return loopIntervalMs
   */
  long loopIntervalMs() default 1000L;

  /**
   * The max broken duration, if exceeds then start try mode.
   *
   * @return maxBrokenDuration
   */
  String maxBrokenDuration() default "PT1H";

  /**
   * The number of messages received per execution, using the same message consumer. Each message
   * receipt has its own message acknowledgement, which means that in JTA XA, each message received
   * and processed is a separate transaction; in SESSION_TRANSACTED, each message received and
   * processed is committed independently; in CLIENT_ACKNOWLEDGE, each receive processing is
   * acknowledged independently.
   *
   * Default value is 1.
   *
   * @return receiveThreshold
   */
  int receiveThreshold() default 1;

  /**
   * Internal MessageConsumer receive timeout in millseconds
   *
   * @return receiveTimeout
   */
  long receiveTimeout() default 1000L;

  /**
   * The message reply config
   */
  MessageReply[] reply() default {};

  /**
   * @see Session#createConsumer(javax.jms.Destination, String)
   * @return selector
   */
  String selector() default EMPTY;

  /**
   * the try threshold, used to recover from break mode and enter try mode. In try mode, if any
   * error occurs, go straight into break mode.
   *
   * @return tryThreshold
   */
  int tryThreshold() default 2;

  /**
   * The value of the timeout in seconds. If the value is zero, the transaction service restores the
   * default value. If the value is negative a SystemException is thrown. Only works if
   * {@link #xa()} = true.
   *
   * @return txTimeout
   */
  int txTimeout() default 0;

  /**
   * Whether to enable XA to receive messages, if true the Connection factory must support
   * XAConnection and the {@link #acknowledge()} return will be ignored. Default is true.
   *
   * @return xa
   */
  boolean xa() default true;
}
