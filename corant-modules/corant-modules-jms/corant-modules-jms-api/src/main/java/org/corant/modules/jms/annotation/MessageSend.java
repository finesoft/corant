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
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.jms.JMSProducer;
import javax.jms.Message;
import org.corant.modules.jms.JMSNames;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:49:53
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface MessageSend {

  /**
   * Sets the minimum length of time in milliseconds that must elapse after a message is sent before
   * the Jakarta Messaging provider may deliver the message to a consumer.
   *
   * For transacted sends, this time starts when the client sends the message, not when the
   * transaction is committed.
   *
   * deliveryDelay is set to -1 by default, means not setting
   *
   * @return deliveryDelay
   * @see JMSProducer#setDeliveryDelay(long)
   */
  String deliveryDelay() default "-1";

  /**
   * This delivery mode instructs the Jakarta Messaging provider to log the message to stable
   * storage as part of the client's send operation, delivery mode is set to PERSISTENT by default.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>integer</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to integer type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to integer value.
   *
   * @see JMSProducer#setDeliveryMode(int)
   */
  String deliveryMode() default "2";// DeliveryMode.PERSISTENT;

  /**
   * The message destination
   *
   * @return destination
   */
  MessageDestination destination();

  /**
   * In the Jakarta EE web or EJB container, when there is no active JTA transaction in progress:
   * The argument acknowledgeMode must be set to either of JMSContext.AUTO_ACKNOWLEDGE or
   * JMSContext.DUPS_OK_ACKNOWLEDGE. The session will be non-transacted and messages received by
   * this session will be acknowledged automatically according to the value of acknowledgeMode. For
   * a definition of the meaning of these acknowledgement modes see the links below. The values
   * JMSContext.SESSION_TRANSACTED and JMSContext.CLIENT_ACKNOWLEDGE may not be used.
   *
   * <p>
   * Note: The final value type is <b>boolean</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to boolean type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to boolean value.
   *
   * @return sessionMode
   */
  String dupsOkAck() default "false";

  /**
   * Specify a marshal scheme to marshaling the POJO into {@link Message} object, when the message
   * object sent is a POJO, default is "STD_JAVA".
   *
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   *
   * @return the marshaller name
   */
  String marshaller() default JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA;

  /**
   * Specifies that messages sent using the JMSProducer will have the specified property set to the
   * specified Java object value.
   *
   * <p>
   * Note that this method works only for the objectified primitive object types (Integer, Double,
   * Long ...) and String objects.
   *
   * @return properties
   *
   * @see JMSProducer#setProperty(String, Object)
   */
  MessageProperty[] properties() default {};

  /**
   * Specifies the time to live of messages that are sent using the JMSProducer. This is used to
   * determine the expiration time of a message.
   *
   * The expiration time of a message is the sum of the message's time to live and the time it is
   * sent. For transacted sends, this is the time the client sends the message, not the time the
   * transaction is committed.
   *
   * Time to live is set to zero by default, which means a message never expires.
   *
   * <p>
   * <b>Note:</b> The final value type is <b>long</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to long type. If the value of this property uses the <b>"${...}"</b> expression, the
   * specific value can be obtained from the system property or configuration, and then convert it
   * to long value.
   *
   * @return timeToLive
   * @see JMSProducer#setTimeToLive(long)
   */
  String timeToLive() default "-1";

}
