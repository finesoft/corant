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
package org.corant.shared.service;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * corant-context
 *
 * <p>
 * Annotation that may be placed upon an SPI implementation class, use to conditionally veto it if a
 * system property or system environment variable not meet the condition.
 *
 * @author bingo 下午3:08:47
 *
 */
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
public @interface RequiredConfiguration {

  String DEFAULT_NULL_VALUE = "$default_null$";

  /**
   * The name of system property or system environment variable which value will be used to
   * predication.
   *
   * @return key
   */
  String key();

  /**
   * The predication
   *
   * @return predicate
   */
  ValuePredicate predicate() default ValuePredicate.NO_NULL;

  /**
   * The type of the value corresponding to the key
   *
   * @return type
   */
  Class<?> type() default String.class;

  /**
   * The reference value used for predication
   *
   * @return value
   */
  String value() default DEFAULT_NULL_VALUE;

  enum ValuePredicate {
    NULL, BLANK, EMPTY, NO_EMPTY, NO_NULL, NO_BLANK, EQ, NO_EQ, GTE, GT, LT, LTE, INC, EXC, REGEX
  }
}
