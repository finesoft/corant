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
 * @author bingo 上午11:47:07
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsKeyword {

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
   * Should global ordinals be loaded eagerly on refresh? Accepts true or false (default). Enabling
   * this is a good idea on fields that are frequently used for terms aggregations.
   *
   * @return
   */
  boolean eager_global_ordinals() default false;

  /**
   * Multi-fields allow the same string value to be indexed in multiple ways for different purposes,
   * such as one field for search and a multi-field for sorting and aggregations.
   *
   * @return
   */
  EsMultiFields fields() default @EsMultiFields(entries = {});

  /**
   * Do not index any string longer than this value. Defaults to 2147483647 so that all values would
   * be accepted. Please however note that default dynamic mapping rules create a sub keyword field
   * that overrides this default by setting ignore_above: 256.
   *
   * @return
   */
  short ignore_above() default 256;

  /**
   * Should the field be searchable? Accepts true (default) or false.
   *
   * @return
   */
  boolean index() default true;

  /**
   * What information should be stored in the index, for scoring purposes. Defaults to docs but can
   * also be set to freqs to take term frequency into account when computing scores.
   *
   * @return
   */
  EsIndexOption index_options() default EsIndexOption.DOCS;

  /**
   * [experimental] This functionality is experimental and may be changed or removed completely in a
   * future release. Elastic will take a best effort approach to fix any issues, but experimental
   * features are not subject to the support SLA of official GA features. How to pre-process the
   * keyword prior to indexing. Defaults to null, meaning the keyword is kept as-is.
   *
   * @return
   */
  String normalizer() default EMPTY;

  /**
   * Whether field-length should be taken into account when scoring queries. Accepts true or false
   * (default).
   *
   * @return
   */
  boolean norms() default false;

  /**
   * Accepts a string value which is substituted for any explicit null values. Defaults to null,
   * which means the field is treated as missing.
   *
   * @return
   */
  String null_value() default EMPTY;

  /**
   *
   * Which scoring algorithm or similarity should be used. Defaults to BM25.
   *
   * @return
   */
  String similarity() default "BM25";

  /**
   * Whether full text queries should split the input on whitespace when building a query for this
   * field. Accepts true or false (default).
   */
  boolean split_queries_on_whitespace() default false;

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return
   */
  boolean store() default false;
}
