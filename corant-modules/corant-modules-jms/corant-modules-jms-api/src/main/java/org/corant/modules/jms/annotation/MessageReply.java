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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import org.corant.modules.jms.JMSNames;
import org.corant.modules.jms.marshaller.MessageMarshaller;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:49:53
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface MessageReply {
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
   * The destination name
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   */
  String destination();

  /**
   * Specify a marshal scheme to marshaling the POJO into {@link Message} object, when the message
   * object sent is a POJO, default is "STD_JAVA".
   *
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   *
   * @return the marshaler name
   * @see MessageMarshaller
   */
  String marshaller() default JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA;

  /**
   * Marks whether a queue or topic.
   * <p>
   * Value of true represents Topic, Value of false represents Queue, default is false.
   * <p>
   * <b>Note:</b> The final value type is <b>boolean</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to boolean type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to boolean value.
   *
   */
  String multicast() default "false";

}
