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
import org.corant.shared.retry.BackoffStrategy.BackoffAlgorithm;

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
   * Returns the message session acknowledgment mode. Default is 2, Session#CLIENT_ACKNOWLEDGE.
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @see Session#AUTO_ACKNOWLEDGE
   * @see Session#CLIENT_ACKNOWLEDGE
   * @see Session#DUPS_OK_ACKNOWLEDGE
   * @see Session#SESSION_TRANSACTED
   *
   * @return acknowledge
   */
  String acknowledge() default "2";

  /**
   * Returns the Back-off Algorithm, default is {@link BackoffAlgorithm#FIXED}
   * <p>
   * <b>Note:</b> The final value type is <b>{@link BackoffAlgorithm}</b> type; in order to support
   * configurability, the string is used as the value type of annotation property, and the value
   * will eventually be converted to {@link BackoffAlgorithm} type. If the value of this property
   * uses the <b>"${...}"</b> expression, the specific value can be obtained from the system
   * property or configuration, and then convert it to {@link BackoffAlgorithm} value.
   *
   * @see BackoffAlgorithm
   */
  String brokenBackoffAlgo() default "FIXED";

  /**
   * The broken back-off factor, for Exponential back-off + jitter algorithm to compute the delay.
   * The value of back-off factor must greater then 1.0, default is 2.0.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>double</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to double type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to double value.
   *
   * @return brokenBackoff
   */
  String brokenBackoffFactor() default "2.0";

  /**
   * The broken duration, if exceeds then start try mode.
   *
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   *
   * @return brokenDuration
   */
  String brokenDuration() default "PT5M";

  /**
   * Marks whether a connection or session is cached. Default is 3.
   *
   * <pre>
   * Value of 0 represents Not cache
   * Value of 1 represents cache connection
   * Value of 2 represents cache session
   * Value of 3 represents cache message consumer
   * </pre>
   *
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @return cacheLevel
   */
  String cacheLevel() default "3";

  /**
   * The failure threshold, if exceeds then start break mode.
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @return failureThreshold
   */
  String failureThreshold() default "16";

  /**
   * When the task breaks itself, the scheduler can still schedule the task, but the task execution
   * only sleeps for a short time and then returns directly. The sleep short time is loopIntervalMs.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>long</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to long type. If the value of this property uses the <b>"${...}"</b> expression, the
   * specific value can be obtained from the system property or configuration, and then convert it
   * to long value.
   *
   * @return loopIntervalMs
   */
  String loopIntervalMs() default "1000";

  /**
   * The max broken duration, if exceeds then start try mode.
   *
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   *
   * @return maxBrokenDuration
   */
  String maxBrokenDuration() default "PT1H";

  /**
   * The number of messages received per execution, using the same message consumer. Each message
   * receipt has its own message acknowledgement, which means that in JTA XA, each message received
   * and processed is a separate transaction; in SESSION_TRANSACTED, each message received and
   * processed is committed independently; in CLIENT_ACKNOWLEDGE, each receive processing is
   * acknowledged independently. Default value is 1.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @return receiveThreshold
   */
  String receiveThreshold() default "1";

  /**
   * Internal MessageConsumer receive timeout in milliseconds. Default is 1000.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>long</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to long type. If the value of this property uses the <b>"${...}"</b> expression, the
   * specific value can be obtained from the system property or configuration, and then convert it
   * to long value.
   *
   * @return receiveTimeout
   */
  String receiveTimeout() default "1000";

  /**
   * The message reply config
   */
  MessageReply[] reply() default {};

  /**
   * Returns the message selector.
   *
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   *
   * @see Session#createConsumer(javax.jms.Destination, String)
   * @return selector
   */
  String selector() default EMPTY;

  /**
   * The pairs of destination name and selector for consuming multiple destinations.
   */
  String[] specifiedSelectors() default {};

  /**
   * the try threshold, used to recover from break mode and enter try mode. In try mode, if any
   * error occurs, go straight into break mode.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @return tryThreshold
   */
  String tryThreshold() default "2";

  /**
   * The value of the timeout in seconds. If the value is zero, the transaction service restores the
   * default value. If the value is negative a SystemException is thrown. Only works if
   * {@link #xa()} = true, default is 0.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @return txTimeout
   */
  String txTimeout() default "0";

  /**
   * Whether to enable XA to receive messages, if true the Connection factory must support
   * XAConnection and the {@link #acknowledge()} return will be ignored. Default is true.
   *
   * <p>
   * Note: The final value type is <b>boolean</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to boolean type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to boolean value.
   *
   * @return xa
   */
  String xa() default "true";
}
