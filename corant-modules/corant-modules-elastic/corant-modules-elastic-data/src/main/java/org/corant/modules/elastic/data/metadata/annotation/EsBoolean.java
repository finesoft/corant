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

import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * corant-modules-elastic-data
 *
 * Boolean fields accept JSON true and false values, but can also accept strings which are
 * interpreted as either true or false: False values false, "false" True values true, "true"
 *
 * Aggregations like the terms aggregation use 1 and 0 for the key, and the strings "true" and
 * "false" for the key_as_string. Boolean fields when used in scripts, return 1 and 0
 *
 * @author bingo 上午11:42:28
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsBoolean {

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
   * Should the field be searchable? Accepts true (default) and false.
   *
   * @return
   */
  boolean index() default true;

  /**
   * Accepts any of the true or false values listed above. The value is substituted for any explicit
   * null values. Defaults to null, which means the field is treated as missing.
   *
   * @return
   */
  String null_value() default EMPTY;

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return
   */
  boolean store() default false;
}
