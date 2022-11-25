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
package org.corant.modules.elastic.data.metadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 2017年3月3日
 * @since
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsRange {
  /**
   * Mapping field-level query time boosting. Accepts a floating point number, defaults to 1.0.
   *
   * @return
   */
  float boost() default 1.0f;

  /**
   * Try to convert strings to numbers and truncate fractions for integers. Accepts true (default)
   * and false.
   *
   * @return
   */
  boolean coerce() default true;

  /**
   * Should the field be searchable? Accepts true (default) and false.
   *
   * @return
   */
  boolean index() default true;

  /**
   * Additional properties for mapping, for instance in the date range, you can assign
   * {name="format" value = "yyyy-MM-dd
   * HH:mm:ss||yyyy-MM-dd||epoch_millis||yyyy-MM-dd'T'HH:mm:ss.SSSz"} to the field.
   *
   * @return properties
   */
  EsProperty[] properties() default {};

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return
   */
  boolean store() default false;

  /**
   * Range type
   *
   * @return
   */
  RangeType type();

  enum RangeType {
    /**
     * A range of signed 32-bit integers with a minimum value of -2^31 and maximum of 2^31-1.
     */
    INTEGER_RANGE("integer_range"),
    /**
     * A range of single-precision 32-bit IEEE 754 floating point values.
     */
    FLOAT_RANGE("float_range"),
    /**
     * A range of signed 64-bit integers with a minimum value of -2^63 and maximum of 2^63-1.
     */
    LONG_RANGE("long_range"),
    /**
     * A range of double-precision 64-bit IEEE 754 floating point values.
     */
    DOUBLE_RANGE("double_range"),
    /**
     * A range of date values represented as unsigned 64-bit integer milliseconds elapsed since
     * system epoch.
     */
    DATE_RANGE("date_range");

    private final String value;

    RangeType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
