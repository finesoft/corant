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
package org.corant.suites.jms.shared.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.jms.JMSContext;
import javax.jms.Session;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:41:54
 *
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageReceive {

  /**
   *
   * @see Session#AUTO_ACKNOWLEDGE
   * @see Session#CLIENT_ACKNOWLEDGE
   * @see Session#DUPS_OK_ACKNOWLEDGE
   * @see Session#SESSION_TRANSACTED
   *
   * @return acknowledge
   */
  int acknowledge() default Session.CLIENT_ACKNOWLEDGE;

  /**
   * The breaked duration, if exceeds then start try mode.
   *
   * @return breakedDuration
   */
  String breakedDuration() default "PT5M";

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
   * @see JMSContext#setClientID(String)
   * @return clientId
   */
  String clientId() default "";

  /**
   * The connection factory id, used to represent a JMS service or cluster, usually set up through a
   * configuration file.
   *
   * @return connectionFactoryId
   */
  String connectionFactoryId() default "";

  /**
   * The destination name
   *
   * @see javax.jms.Destination
   * @return destinations
   */
  String[] destinations() default {};

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
  long loopIntervalMs() default 500L;

  /**
   * Marks whether a queue or topic.
   *
   * Value of true represents Topic, Value of false represents Queue, default is false.
   *
   * @return multicast
   */
  boolean multicast() default false;

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
   * @see javax.jms.MessageConsumer#receive(long)
   * @return receiveTimeout
   */
  long receiveTimeout() default 1000L;

  /**
   * @see Session#createConsumer(javax.jms.Destination, String)
   * @return selector
   */
  String selector() default "";

  /**
   * TODO
   *
   * @return subscriptionDurable
   */
  boolean subscriptionDurable() default false;

  /**
   * the try threshold, used to recover from break mode and enter try mode. In try mode, if any
   * error occurs, go straight into break mode.
   *
   * @return tryThreshold
   */
  int tryThreshold() default 4;

  /**
   * TODO
   *
   * @return type
   */
  Class<?> type() default String.class;
}
