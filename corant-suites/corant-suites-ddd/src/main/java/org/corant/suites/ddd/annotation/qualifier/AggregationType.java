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
package org.corant.suites.ddd.annotation.qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import org.corant.suites.ddd.model.Aggregation;

/**
 * @author bingo 下午9:05:13
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface AggregationType {

  Class<? extends Aggregation> value();

  public static class AggregationTypeLiteral extends AnnotationLiteral<AggregationType>
      implements AggregationType {

    private static final long serialVersionUID = -5552841006073177750L;

    private final Class<? extends Aggregation> value;

    private AggregationTypeLiteral(Class<? extends Aggregation> value) {
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static AggregationTypeLiteral[] from(Class<? extends Aggregation>... values) {
      return Arrays.stream(values).map(AggregationTypeLiteral::of)
          .toArray(AggregationTypeLiteral[]::new);
    }

    public static AggregationTypeLiteral of(Class<? extends Aggregation> value) {
      return new AggregationTypeLiteral(value);
    }

    @Override
    public Class<? extends Aggregation> value() {
      return value;
    }
  }
}
