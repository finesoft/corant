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
package org.corant.modules.query.elastic.cdi;

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
 * corant-modules-query-elastic
 *
 * <P>
 * Elastic name query service qualifier.
 *
 * <pre>
 *  The property value is cluster name, usually with <b>corant-modules-elastic-data</b>.
 * </pre>
 *
 * @author bingo 下午6:05:00
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface EsQuery {

  /**
   * The elastic data center name
   *
   * @return value
   */
  @Nonbinding
  String value() default "";

  /**
   * corant-modules-query-elastic
   *
   * @author bingo 上午11:42:58
   *
   */
  final class EsQueryLiteral extends AnnotationLiteral<EsQuery> implements EsQuery {

    public static final EsQuery INSTANCE = of("");

    private static final long serialVersionUID = 1L;

    private final String value;

    private EsQueryLiteral(String value) {
      this.value = value;
    }

    public static EsQueryLiteral of(String value) {
      return new EsQueryLiteral(defaultString(value));
    }

    @Override
    public String value() {
      return value;
    }
  }
}
