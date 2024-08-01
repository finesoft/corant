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
package org.corant.modules.ddd.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Annotations.calculateMembersHashCode;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Objects.forceCast;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.corant.modules.ddd.Aggregate;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 下午9:05:13
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface AggregateType {

  Class<? extends Aggregate> value();

  class AggregateTypeLiteral extends AnnotationLiteral<AggregateType> implements AggregateType {

    private static final long serialVersionUID = -5552841006073177750L;

    private Class<? extends Aggregate> value;
    private transient volatile Integer hashCode;

    private AggregateTypeLiteral(Class<? extends Aggregate> value) {
      this.value = forceCast(getUserClass(value));
    }

    @SuppressWarnings("unchecked")
    public static AggregateTypeLiteral[] from(Class<? extends Aggregate>... values) {
      return Arrays.stream(values).map(AggregateTypeLiteral::of)
          .toArray(AggregateTypeLiteral[]::new);
    }

    public static AggregateTypeLiteral of(Aggregate aggregate) {
      return new AggregateTypeLiteral(aggregate.getClass());
    }

    public static AggregateTypeLiteral of(Class<? extends Aggregate> value) {
      return new AggregateTypeLiteral(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !AggregateType.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      AggregateType other = (AggregateType) obj;
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
    public Class<? extends Aggregate> value() {
      return value;
    }
  }
}
