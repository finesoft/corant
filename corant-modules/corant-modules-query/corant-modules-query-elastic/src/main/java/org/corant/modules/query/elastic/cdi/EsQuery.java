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
import static org.corant.shared.util.Annotations.calculateMembersHashCode;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import org.corant.shared.ubiquity.Tuple.Pair;

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
  String value() default EMPTY;

  /**
   * corant-modules-query-elastic
   *
   * @author bingo 上午11:42:58
   *
   */
  final class EsQueryLiteral extends AnnotationLiteral<EsQuery> implements EsQuery {

    public static final EsQuery INSTANCE = of(EMPTY);

    private static final long serialVersionUID = 1L;

    private String value;
    private transient volatile Integer hashCode;

    private EsQueryLiteral(String value) {
      this.value = defaultString(value);
    }

    public static EsQueryLiteral of(String value) {
      return new EsQueryLiteral(defaultString(value));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !EsQuery.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      EsQuery other = (EsQuery) obj;
      return value.equals(other.value());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("value", value));
      }
      return hashCode;
    }

    @Override
    public String value() {
      return value;
    }
  }
}
