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
public @interface EsNumeric {

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
   * Should the field be stored on disk in a column-stride fashion, so that it can later be used for
   * sorting, aggregations, or scripting? Accepts true (default) or false.
   *
   * @return
   */
  boolean doc_values() default true;

  /**
   * If true, malformed numbers are ignored. If false (default), malformed numbers throw an
   * exception and reject the whole document.
   *
   * @return
   */
  boolean ignore_malformed() default false;

  /**
   * Should the field be searchable? Accepts true (default) and false.
   *
   * @return
   */
  boolean index() default true;

  /**
   * Accepts a numeric value of the same type as the field which is substituted for any explicit
   * null values. Defaults to null, which means the field is treated as missing.
   *
   * @return
   */
  String null_value() default "";

  /**
   *
   * A floating point that is backed by a long and a fixed scaling factor. The scaling factor to use
   * when encoding values. Values will be multiplied by this factor at index time and rounded to the
   * closest long value. For instance, a scaled_float with a scaling_factor of 10 would internally
   * store 2.34 as 23 and all search-time operations (queries, aggregations, sorting) will behave as
   * if the document had a value of 2.3. High values of scaling_factor improve accuracy but also
   * increase space requirements. This parameter is required.
   *
   * @return
   */
  short scaling_factor() default 1;

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return
   */
  boolean store() default false;

  /**
   * Actual type
   *
   * @return
   */
  EsNumericType type();

  enum EsNumericType {

    /**
     * A signed 64-bit integer with a minimum value of -2^63 and a maximum value of 2^63-1.
     */
    LONG("long"),
    /**
     * A signed 32-bit integer with a minimum value of -2^31 and a maximum value of 2^31-1.
     */
    INTEGER("integer"),
    /**
     * A signed 16-bit integer with a minimum value of -32,768 and a maximum value of 32,767.
     */
    SHORT("short"),
    /**
     * A signed 8-bit integer with a minimum value of -128 and a maximum value of 127.
     */
    BYTE("byte"),
    /**
     * A double-precision 64-bit IEEE 754 floating point.
     */
    DOUBLE("double"),
    /**
     * A single-precision 32-bit IEEE 754 floating point.
     */
    FLOAT("float"),
    /**
     * A half-precision 16-bit IEEE 754 floating point.
     */
    HALF_FLOAT("half_float"),
    /**
     * A floating point that is backed by a long and a fixed scaling factor.
     */
    SCALED_FLOAT("scaled_float");

    private final String value;

    EsNumericType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
