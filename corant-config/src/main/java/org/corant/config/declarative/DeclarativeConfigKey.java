/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.config.declarative;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * corant-config
 *
 * @author bingo 下午12:24:47
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface DeclarativeConfigKey {

  String UNCONFIGURED_KEY = "";

  @Nonbinding
  String value() default UNCONFIGURED_KEY;

  class DeclarativeConfigKeyLiteral extends AnnotationLiteral<DeclarativeConfigKey>
      implements DeclarativeConfigKey {

    private static final long serialVersionUID = 6321463899932625786L;

    public static final DeclarativeConfigKeyLiteral UNCONFIGURED =
        DeclarativeConfigKeyLiteral.of(UNCONFIGURED_KEY);

    private final String value;

    private DeclarativeConfigKeyLiteral(String value) {
      this.value = value;
    }

    public static DeclarativeConfigKeyLiteral of(String value) {
      return new DeclarativeConfigKeyLiteral(value);
    }

    @Override
    public String value() {
      return value;
    }

  }
}
