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
public @interface EsTokenCount {

  /**
   * The analyzer which should be used to analyze the string value. Required. For best performance,
   * use an analyzer without token filters.
   *
   * @return analyzer
   */
  String analyzer() default "standard";

  /**
   * Mapping field-level query time boosting. Accepts a floating point number, defaults to 1.0.
   *
   * @return boost
   */
  float boost() default 1.0f;

  /**
   *
   * Should the field be stored on disk in a column-stride fashion, so that it can later be used for
   * sorting, aggregations, or scripting? Accepts true (default) or false.
   *
   * @return doc_values
   */
  boolean doc_values() default true;

  /**
   *
   * Indicates if position increments should be counted. Set to false if you don’t want to count
   * tokens removed by analyzer filters (like stop). Defaults to true.
   *
   * @return enable_position_increments
   */
  boolean enable_position_increments() default true;

  /**
   *
   * Should the field be searchable? Accepts true (default) and false.
   *
   * @return index
   */
  boolean index() default true;

  /**
   * Accepts a numeric value of the same type as the field which is substituted for any explicit
   * null values. Defaults to null, which means the field is treated as missing.
   *
   * @return null_value
   */
  String null_value() default "";

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return store
   */
  boolean store() default false;
}
