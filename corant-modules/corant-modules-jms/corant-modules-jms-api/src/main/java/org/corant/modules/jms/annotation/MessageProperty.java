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

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:46:33
 *
 */
@Target({})
@Retention(RUNTIME)
public @interface MessageProperty {

  MessageProperty[] EMPTY_ARRAY = {};

  /** The name of the property */
  String name();

  Class<?> type() default String.class;

  /**
   * The value of the property
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   */
  String value();

  class MessagePropertyLiteral extends AnnotationLiteral<MessageProperty>
      implements MessageProperty {

    private static final long serialVersionUID = 7660764550683179095L;

    final String name;
    final String value;
    final Class<?> type;

    private MessagePropertyLiteral(String name, String value, Class<?> type) {
      this.name = name;
      this.value = value;
      this.type = type;
    }

    public static MessagePropertyLiteral[] from(MessageProperty... properties) {
      return streamOf(properties).map(MessagePropertyLiteral::of)
          .toArray(MessagePropertyLiteral[]::new);
    }

    public static MessagePropertyLiteral of(MessageProperty p) {
      return new MessagePropertyLiteral(p.name(), p.value(), p.type());
    }

    public static MessagePropertyLiteral of(String name, String value, Class<?> type) {
      return new MessagePropertyLiteral(name, value, type);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Class<?> type() {
      return type;
    }

    @Override
    public String value() {
      return value;
    }

  }
}
