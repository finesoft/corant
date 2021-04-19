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
 * @author bingo 2017年3月3日
 * @since
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsText {

  /**
   * The analyzer which should be used for analyzed string fields, both at index-time and at
   * search-time (unless overridden by the search_analyzer). Defaults to the default index analyzer,
   * or the standard analyzer.
   *
   * @return
   */
  String analyzer() default "standard";

  /**
   * Mapping field-level query time boosting. Accepts a floating point number, defaults to 1.0.
   *
   * @return
   */
  float boost() default 1.0f;

  /**
   * Should global ordinals be loaded eagerly on refresh? Accepts true or false (default). Enabling
   * this is a good idea on fields that are frequently used for terms aggregations.
   *
   * @return
   */
  boolean eager_global_ordinals() default false;

  /**
   * Can the field use in-memory fielddata for sorting, aggregations, or scripting? Accepts true or
   * false (default).
   *
   * @return
   */
  boolean fielddata() default false;

  /**
   * Expert settings which allow to decide which values to load in memory when fielddata is enabled.
   * By default all values are loaded.
   *
   * @return
   */
  EsFielddataFrequencyFilter fielddata_frequency_filter() default @EsFielddataFrequencyFilter(
      max = 0.1f, min = 0.01f, min_segment_size = 100);

  /**
   * Multi-fields allow the same string value to be indexed in multiple ways for different purposes,
   * such as one field for search and a multi-field for sorting and aggregations, or the same string
   * value analyzed by different analyzers.
   *
   * @return
   */
  EsMultiFields fields() default @EsMultiFields(entries = {});

  /**
   * Should the field be searchable? Accepts true (default) and false.
   *
   * @return
   */
  boolean index() default true;

  /**
   * What information should be stored in the index, for search and highlighting purposes. Defaults
   * to positions.
   *
   * @return
   */
  EsIndexOption index_options() default EsIndexOption.POSITIONS;

  /**
   * If enabled, two-term word combinations (shingles) are indexed into a separate field. This
   * allows exact phrase queries to run more efficiently, at the expense of a larger index. Note
   * that this works best when stopwords are not removed, as phrases containing stopwords will not
   * use the subsidiary field and will fall back to a standard phrase query. Accepts true or false
   * (default).
   */
  boolean index_phrases() default false;

  /**
   * f enabled, term prefixes of between 2 and 5 characters are indexed into a separate field. This
   * allows prefix searches to run more efficiently, at the expense of a larger index.
   */
  int index_prefixes_max_chars() default 0;

  /**
   * f enabled, term prefixes of between 2 and 5 characters are indexed into a separate field. This
   * allows prefix searches to run more efficiently, at the expense of a larger index.
   */
  int index_prefixes_min_chars() default 0;

  /**
   * Whether field-length should be taken into account when scoring queries. Accepts true (default)
   * or false.
   *
   * @return
   */
  boolean norms() default true;

  /**
   * The number of fake term position which should be inserted between each element of an array of
   * strings. Defaults to the position_increment_gap configured on the analyzer which defaults to
   * 100. 100 was chosen because it prevents phrase queries with reasonably large slops (less than
   * 100) from matching terms across field values.
   *
   * @return
   */
  short position_increment_gap() default 100;

  /**
   * The analyzer that should be used at search time on analyzed fields. Defaults to the analyzer
   * setting.
   *
   * @return
   */
  String search_analyzer() default "standard";

  /**
   * The analyzer that should be used at search time when a phrase is encountered. Defaults to the
   * search_analyzer setting.
   *
   * @return
   */
  String search_quote_analyzer() default EMPTY;

  /**
   * Which scoring algorithm or similarity should be used. Defaults to BM25.
   *
   * @return
   */
  String similarity() default "BM25";

  /**
   * Whether the field value should be stored and retrievable separately from the _source field.
   * Accepts true or false (default).
   *
   * @return
   */
  boolean store() default false;

  /**
   * Term vectors contain information about the terms produced by the analysis process, including:
   *
   * a list of terms. the position (or order) of each term. the start and end character offsets
   * mapping the term to its origin in the original string. These term vectors can be stored so that
   * they can be retrieved for a particular document.
   *
   * @return
   */
  EsTermVector term_vector() default EsTermVector.NO;

  enum EsTermVector {
    NO("no"), YES("yes"), WITH_POSITIONS("with_positions"), WITH_OFFSETS(
        "with_offsets"), WITH_POSITIONS_OFFSETS("with_positions_offsets");

    private final String value;

    EsTermVector(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

}
