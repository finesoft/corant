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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Conversions.toObject;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import org.corant.modules.jms.shared.context.SerialSchema;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:19:13
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface MessageSerialization {

  SerialSchema schema();

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 下午5:29:27
   *
   */
  class MessageSerializationLiteral extends AnnotationLiteral<MessageSerialization>
      implements MessageSerialization {
    private static final long serialVersionUID = -4241417907420530257L;

    private final SerialSchema schame;

    protected MessageSerializationLiteral(SerialSchema schame) {
      this.schame = schame;
    }

    public static MessageSerializationLiteral of(Object obj) {
      if (obj instanceof MessageSerialization) {
        return new MessageSerializationLiteral(((MessageSerialization) obj).schema());
      } else {
        return new MessageSerializationLiteral(toObject(obj, SerialSchema.class));
      }
    }

    @Override
    public SerialSchema schema() {
      return schame;
    }

  }
}
