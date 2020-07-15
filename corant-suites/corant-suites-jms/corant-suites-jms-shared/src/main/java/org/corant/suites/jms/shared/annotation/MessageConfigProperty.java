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

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:46:33
 *
 */
@Target({})
@Retention(RUNTIME)
public @interface MessageConfigProperty {

  /** The name of the property */
  String name();

  /** The value of the property */
  String value();

  public static class MessageConfigPropertyLiteral extends AnnotationLiteral<MessageConfigProperty>
      implements MessageConfigProperty {

    private static final long serialVersionUID = 7660764550683179095L;

    final String name;
    final String value;

    private MessageConfigPropertyLiteral(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public static MessageConfigPropertyLiteral[] from(MessageConfigProperty... properties) {
      return streamOf(properties).map(MessageConfigPropertyLiteral::of)
          .toArray(MessageConfigPropertyLiteral[]::new);
    }

    public static MessageConfigPropertyLiteral of(MessageConfigProperty p) {
      return new MessageConfigPropertyLiteral(p.name(), p.value());
    }

    public static MessageConfigPropertyLiteral of(String name, String value) {
      return new MessageConfigPropertyLiteral(name, value);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String value() {
      return value;
    }

  }
}
