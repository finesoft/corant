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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.suites.jms.shared.annotation.MessageConfigProperty.MessageConfigPropertyLiteral;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:49:53
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface MessageSender {

  boolean multicast() default false;

  MessageConfigProperty[] properties() default {};

  String destination();

  public static class MessageProducerLiteral extends AnnotationLiteral<MessageSender>
      implements MessageSender {

    private static final long serialVersionUID = 7391504689355513463L;

    final MessageConfigPropertyLiteral[] properties;
    final String destination;
    final boolean multicast;

    /**
     * @param queue
     * @param properties
     */
    private MessageProducerLiteral(boolean multicast, String queue,
        MessageConfigPropertyLiteral[] properties) {
      super();
      this.multicast = multicast;
      this.destination = queue;
      this.properties = properties;
    }

    public static MessageProducerLiteral of(boolean multicast, String destination,
        MessageConfigProperty[] properties) {
      return new MessageProducerLiteral(multicast, destination,
          MessageConfigPropertyLiteral.from(properties));
    }

    public static MessageProducerLiteral of(MessageSender p) {
      return new MessageProducerLiteral(p.multicast(), p.destination(),
          MessageConfigPropertyLiteral.from(p.properties()));
    }

    @Override
    public boolean multicast() {
      return multicast;
    }

    @Override
    public MessageConfigProperty[] properties() {
      return properties;
    }

    @Override
    public String destination() {
      return destination;
    }

  }
}
