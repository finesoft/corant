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
package org.corant.modules.jms.shared.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.jms.DeliveryMode;
import org.corant.modules.jms.shared.context.SerialSchema;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:49:53
 *
 */
@Repeatable(MessageSends.class)
@Retention(RUNTIME)
@Target(TYPE)
public @interface MessageSend {

  /**
   * The connection factory id, used to represent a JMS service or cluster, usually set up through a
   * configuration file.
   */
  String connectionFactoryId() default EMPTY;

  /**
   * This delivery mode instructs the Jakarta Messaging provider to log the message to stable
   * storage as part of the client's send operation
   */
  int deliveryMode() default DeliveryMode.PERSISTENT;

  /**
   * The destination name, can use '${config property name}' to retrieve the destination name from
   * microprofile config source.
   *
   * @return destination
   */
  String destination();

  /**
   * In the Jakarta EE web or EJB container, when there is no active JTA transaction in progress:
   * The argument acknowledgeMode must be set to either of JMSContext.AUTO_ACKNOWLEDGE or
   * JMSContext.DUPS_OK_ACKNOWLEDGE. The session will be non-transacted and messages received by
   * this session willbe acknowledged automatically according to the value of acknowledgeMode. For a
   * definition of the meaning ofthese acknowledgement modes see the links below. The values
   * JMSContext.SESSION_TRANSACTED and JMSContext.CLIENT_ACKNOWLEDGE may not be used.
   *
   */
  boolean dupsOkAck() default false;

  /**
   * The message propagation method, equivalent to Queue if false otherwise Topic .
   */
  boolean multicast() default false;

  /**
   * The message pay load serialization schema.
   */
  SerialSchema serialization() default SerialSchema.JSON_STRING;

}
