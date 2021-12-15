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
package org.corant.modules.elastic.data.metadata.resolver;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.modules.elastic.data.metadata.ElasticMapping;
import org.corant.modules.elastic.data.metadata.annotation.EsAlias;
import org.corant.modules.elastic.data.metadata.annotation.EsArray;
import org.corant.modules.elastic.data.metadata.annotation.EsBinary;
import org.corant.modules.elastic.data.metadata.annotation.EsBoolean;
import org.corant.modules.elastic.data.metadata.annotation.EsDate;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbedded;
import org.corant.modules.elastic.data.metadata.annotation.EsGeoPoint;
import org.corant.modules.elastic.data.metadata.annotation.EsGeoShape;
import org.corant.modules.elastic.data.metadata.annotation.EsIp;
import org.corant.modules.elastic.data.metadata.annotation.EsKeyword;
import org.corant.modules.elastic.data.metadata.annotation.EsMultiFieldsEntry;
import org.corant.modules.elastic.data.metadata.annotation.EsMultiFieldsPair;
import org.corant.modules.elastic.data.metadata.annotation.EsNested;
import org.corant.modules.elastic.data.metadata.annotation.EsNumeric;
import org.corant.modules.elastic.data.metadata.annotation.EsNumeric.EsNumericType;
import org.corant.modules.elastic.data.metadata.annotation.EsPercolator;
import org.corant.modules.elastic.data.metadata.annotation.EsProperty;
import org.corant.modules.elastic.data.metadata.annotation.EsRange;
import org.corant.modules.elastic.data.metadata.annotation.EsText;
import org.corant.modules.elastic.data.metadata.annotation.EsTokenCount;
import org.corant.shared.util.Types;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午2:19:52
 *
 */
public class ResolverUtils {

  public static Map<String, Object> genFieldMapping(EsAlias ann, List<String> path) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "alias");
    map.put("path", String.join(".", path));
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsArray ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", ann.eleType());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsBinary ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "binary");
    map.put("doc_values", ann.doc_values());
    map.put("store", ann.store());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsBoolean ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "boolean");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("store", ann.store());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsDate ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "date");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    map.put("format", ann.format());
    map.put("ignore_malformed", ann.ignore_malformed());
    if (isNotBlank(ann.locale())) {
      map.put("locale", ann.locale());
    }
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("store", ann.store());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsEmbedded ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("dynamic", ann.dynamic());
    map.put("enabled", ann.enabled());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsGeoPoint ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "geo_point");
    map.put("ignore_malformed", ann.ignore_malformed());
    map.put("ignore_z_value", ann.ignore_z_value());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsGeoShape ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "geo_shape");
    EsProperty[] properties = ann.options();
    if (!isEmpty(properties)) {
      for (EsProperty property : properties) {
        map.put(property.name(), property.value());
      }
    }
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsIp ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "ip");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("store", ann.store());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsKeyword ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "keyword");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    map.put("store", ann.store());
    map.put("eager_global_ordinals", ann.eager_global_ordinals());
    map.put("ignore_above", ann.ignore_above());
    map.put("index_options", ann.index_options().getValue());
    if (isNotBlank(ann.normalizer()) && !"$null$".equalsIgnoreCase(ann.normalizer())) {
      map.put("normalizer", ann.normalizer());
    }
    map.put("norms", ann.norms());
    map.put("similarity", ann.similarity());
    if (ann.fields() != null && ann.fields().entries().length != 0) {
      Map<String, Object> fm = new HashMap<>();
      for (EsMultiFieldsEntry e : ann.fields().entries()) {
        shouldNotNull(e.fieldName());
        shouldBeFalse(fm.containsKey(e.fieldName()));
        Map<String, String> pm = new HashMap<>();
        for (EsMultiFieldsPair p : e.pairs()) {
          pm.put(p.key(), p.value());
        }
        fm.put(e.fieldName(), pm);
      }
      if (!fm.isEmpty()) {
        map.put("fields", fm);
      }
    }
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("split_queries_on_whitespace", ann.split_queries_on_whitespace());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsNested ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "nested");
    map.put("dynamic", ann.dynamic());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsNumeric ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", ann.type().getValue());
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("store", ann.store());
    map.put("coerce", ann.coerce());
    map.put("ignore_malformed", ann.ignore_malformed());
    if (ann.type() == EsNumericType.SCALED_FLOAT) {
      map.put("scaling_factor", ann.scaling_factor());
    }
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsPercolator ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "percolator");
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsRange ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", ann.type().getValue());
    map.put("boost", ann.boost());
    map.put("coerce", ann.coerce());
    map.put("index", ann.index());
    map.put("store", ann.store());
    EsProperty[] properties = ann.properties();
    if (!isEmpty(properties)) {
      for (EsProperty property : properties) {
        map.put(property.name(), property.value());
      }
    }
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsText ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "text");
    map.put("boost", ann.boost());
    map.put("fielddata", ann.fielddata());
    if (ann.fielddata() && ann.fielddata_frequency_filter() != null) {
      Map<String, Object> fm = new HashMap<>();
      fm.put("max", ann.fielddata_frequency_filter().max());
      fm.put("min", ann.fielddata_frequency_filter().min());
      fm.put("min_segment_size", ann.fielddata_frequency_filter().min_segment_size());
      map.put("fielddata_frequency_filter", fm);
    }
    map.put("index", ann.index());
    map.put("store", ann.store());
    map.put("eager_global_ordinals", ann.eager_global_ordinals());
    map.put("index_options", ann.index_options().getValue());
    map.put("norms", ann.norms());
    if (ann.position_increment_gap() != 100) {
      map.put("position_increment_gap", ann.position_increment_gap());
    }
    map.put("analyzer", ann.analyzer());
    map.put("search_analyzer", ann.search_analyzer());
    map.put("similarity", ann.similarity());
    if (ann.fields() != null && ann.fields().entries().length != 0) {
      Map<String, Object> fm = new HashMap<>();
      for (EsMultiFieldsEntry e : ann.fields().entries()) {
        shouldNotNull(e.fieldName());
        shouldBeFalse(fm.containsKey(e.fieldName()));
        Map<String, Object> pm = new HashMap<>();
        for (EsMultiFieldsPair p : e.pairs()) {
          if (p.valueType() == String.class) {
            pm.put(p.key(), p.value());
          } else if (p.valueType() == Boolean.class) {
            pm.put(p.key(), toBoolean(p.value()));
          }

        }
        fm.put(e.fieldName(), pm);
      }
      if (!fm.isEmpty()) {
        map.put("fields", fm);
      }
    }
    if (isNotBlank(ann.search_quote_analyzer())) {
      map.put("search_quote_analyzer", ann.search_quote_analyzer());
    } else {
      map.put("search_quote_analyzer", ann.search_analyzer());
    }
    map.put("term_vector", ann.term_vector().getValue());
    if (ann.index_prefixes_min_chars() >= 0 && ann.index_prefixes_max_chars() > 0) {
      map.put("index_prefixes", mapOf("min_chars", ann.index_prefixes_min_chars(), "max_chars",
          ann.index_prefixes_max_chars()));
    }
    map.put("index_phrases", ann.index_phrases());
    return map;
  }

  public static Map<String, Object> genFieldMapping(EsTokenCount ann) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "token_count");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    map.put("analyzer", ann.analyzer());
    map.put("enable_position_increments", ann.enable_position_increments());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    map.put("store", ann.store());
    return map;
  }

  public static Map<String, Object> genJoinMapping(ElasticMapping mapping) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", "join");
    Map<String, Set<String>> relations = new HashMap<>();
    resolveJoinMapping(mapping, relations);
    map.put("relations", relations);
    return map;
  }

  public static Type getCollectionFieldEleType(Field f, Class<?> contextRawType) {
    return Types.canonicalize(Types.getCollectionElementType(f.getGenericType(), contextRawType));
  }

  public static Type[] getMapFieldKeyValTypes(Field f, Class<?> contextRawType) {
    Type[] types = Types.getMapKeyAndValueTypes(f.getGenericType(), contextRawType);
    if (!isEmpty(types)) {
      Type[] result = new Type[types.length];
      result[0] = Types.canonicalize(types[0]);
      result[1] = Types.canonicalize(types[1]);
      return result;
    }
    return new Type[0];
  }

  private static void resolveJoinMapping(ElasticMapping mapping,
      Map<String, Set<String>> relation) {
    if (!isEmpty(mapping.getChildren())) {
      shouldBeTrue(relation.put(mapping.getName(), mapping.getChildren().stream()
          .map(ElasticMapping::getName).collect(Collectors.toSet())) == null);
      for (ElasticMapping childMapping : mapping) {
        if (!isEmpty(childMapping.getChildren())) {
          resolveJoinMapping(childMapping, relation);
        }
      }
    }
  }
}
