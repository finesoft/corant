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
package org.corant.suites.query.jpql.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * corant-suites-query-sql
 *
 * <P>
 * JPQL name query service qualifier.
 *
 * <pre>
 *  The property value is persistence unit name, usually with <b>corant-suites-jpa</b>.
 * </pre>
 *
 * @author bingo 下午6:05:00
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface JpqlQuery {

  /**
   * The persistence unit name
   *
   * @return value
   */
  @Nonbinding
  String value() default "";

  /**
   * corant-suites-query-jpql
   *
   * @author bingo 上午11:41:54
   *
   */
  public static final class JpqlQueryLiteral extends AnnotationLiteral<JpqlQuery>
      implements JpqlQuery {

    public static final JpqlQuery INSTANCE = of("");

    private static final long serialVersionUID = 1L;

    private final String value;

    private JpqlQueryLiteral(String value) {
      this.value = value;
    }

    public static JpqlQueryLiteral of(String value) {
      return new JpqlQueryLiteral(defaultString(value));
    }

    @Override
    public String value() {
      return value;
    }

  }
}
