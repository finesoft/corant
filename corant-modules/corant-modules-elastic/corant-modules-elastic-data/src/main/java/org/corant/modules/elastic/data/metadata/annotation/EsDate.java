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
 * JSON doesn’t have a date datatype, so dates in Elasticsearch can either be:
 * <li>strings containing formatted dates, e.g. "2015-01-01" or "2015/01/01 12:10:30".</li>
 * <li>a long number representing milliseconds-since-the-epoch.</li>
 * <li>an integer representing seconds-since-the-epoch.</li>
 *
 * Internally, dates are converted to UTC (if the time-zone is specified) and stored as a long
 * number representing milliseconds-since-the-epoch.
 *
 * Queries on dates are internally converted to range queries on this long representation, and the
 * result of aggregations and stored fields is converted back to a string depending on the date
 * format that is associated with the field.
 *
 * Dates will always be rendered as strings, even if they were initially supplied as a long in the
 * JSON document.
 *
 * @author bingo 上午11:42:57
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsDate {

  /**
   * Mapping field-level query time boosting. Accepts a floating point number, defaults to 1.0.
   *
   * @return
   */
  float boost() default 1.0f;

  /**
   * Should the field be stored on disk in a column-stride fashion, so that it can later be used for
   * sorting, aggregations, or scripting? Accepts true (default) or false.
   *
   * @return
   */
  boolean doc_values() default true;

  /**
   * The date format(s) that can be parsed. Defaults to strict_date_optional_time||epoch_millis.
   *
   * @return
   */
  String format() default "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis||yyyy-MM-dd'T'HH:mm:ss.SSSz||yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss";

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
   * The locale to use when parsing dates since months do not have the same names and/or
   * abbreviations in all languages.
   *
   * @return locale
   */
  String locale() default "";

  /**
   * Accepts a date value in one of the configured format's as the field which is substituted for
   * any explicit null values. Defaults to null, which means the field is treated as missing.
   *
   * @return null_value
   */
  String null_value() default "";

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   */
  boolean store() default false;
}
