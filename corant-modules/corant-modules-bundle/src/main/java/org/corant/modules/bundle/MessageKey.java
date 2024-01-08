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
package org.corant.modules.bundle;

import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午8:44:38
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface MessageKey {

  @Nonbinding
  String locale() default "zh_CN";

  @Nonbinding
  String[] parameters() default {};

  @Nonbinding
  String value() default EMPTY;

  class MessageCodesLiteral extends AnnotationLiteral<MessageKey> implements MessageKey {

    private static final long serialVersionUID = -5766940942537035511L;

    final Locale locale;
    final String key;
    final String[] parameters;

    public MessageCodesLiteral(Locale locale, String key, String... parameters) {
      this.locale = locale;
      this.key = key;
      this.parameters = parameters;
    }

    @Override
    public String locale() {
      return locale == null ? "zh_CN" : locale.toString();
    }

    @Override
    public String[] parameters() {
      return parameters;
    }

    @Override
    public String value() {
      return defaultString(key, EMPTY);
    }

  }

}
