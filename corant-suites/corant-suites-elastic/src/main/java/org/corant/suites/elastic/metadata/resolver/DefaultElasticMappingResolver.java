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
package org.corant.suites.elastic.metadata.resolver;

import static org.corant.shared.util.AnnotationUtils.findAnnotation;
import static org.corant.shared.util.ClassUtils.isPrimitiveArray;
import static org.corant.shared.util.ClassUtils.isPrimitiveOrWrapper;
import static org.corant.shared.util.ClassUtils.primitiveToWrapper;
import static org.corant.shared.util.CollectionUtils.isEmpty;
import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.FieldUtils.traverseFields;
import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.shouldBeFalse;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.TypeUtils;
import org.corant.suites.elastic.metadata.ElasticIndexing;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.annotation.EsArray;
import org.corant.suites.elastic.metadata.annotation.EsBinary;
import org.corant.suites.elastic.metadata.annotation.EsBoolean;
import org.corant.suites.elastic.metadata.annotation.EsDate;
import org.corant.suites.elastic.metadata.annotation.EsDocument;
import org.corant.suites.elastic.metadata.annotation.EsEmbeddable;
import org.corant.suites.elastic.metadata.annotation.EsEmbedded;
import org.corant.suites.elastic.metadata.annotation.EsGeoPoint;
import org.corant.suites.elastic.metadata.annotation.EsGeoShape;
import org.corant.suites.elastic.metadata.annotation.EsIp;
import org.corant.suites.elastic.metadata.annotation.EsKeyword;
import org.corant.suites.elastic.metadata.annotation.EsMap;
import org.corant.suites.elastic.metadata.annotation.EsMappedSuperclass;
import org.corant.suites.elastic.metadata.annotation.EsMultiFieldsEntry;
import org.corant.suites.elastic.metadata.annotation.EsMultiFieldsPair;
import org.corant.suites.elastic.metadata.annotation.EsNested;
import org.corant.suites.elastic.metadata.annotation.EsNumeric;
import org.corant.suites.elastic.metadata.annotation.EsNumeric.EsNumericType;
import org.corant.suites.elastic.metadata.annotation.EsPercolator;
import org.corant.suites.elastic.metadata.annotation.EsProperty;
import org.corant.suites.elastic.metadata.annotation.EsRange;
import org.corant.suites.elastic.metadata.annotation.EsText;
import org.corant.suites.elastic.metadata.annotation.EsTokenCount;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.index.VersionType;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午5:03:03
 *
 */
@ApplicationScoped
public class DefaultElasticMappingResolver implements ElasticMappingResolver {

  @Inject
  @ConfigProperty(name = "elastic.mapping.version", defaultValue = "V1")
  protected String version;

  @Inject
  protected Logger logger;

  public static void main(String... version) throws NoSuchFieldException, SecurityException {

    Field cf = Test.class.getDeclaredField("collection");
    Field mf = Test.class.getDeclaredField("map");
    System.out.println(getCollectionFieldElementType(cf, Collection.class));
    for (Type mt : getMapFieldKeyAndValueTypes(mf, Map.class)) {
      System.out.println(mt);
    }

  }

  protected static Type getCollectionFieldElementType(Field f, Class<?> contextRawType) {
    return TypeUtils
        .canonicalize(TypeUtils.getCollectionElementType(f.getGenericType(), contextRawType));
  }

  protected static Type[] getMapFieldKeyAndValueTypes(Field f, Class<?> contextRawType) {
    Type[] types = TypeUtils.getMapKeyAndValueTypes(f.getGenericType(), contextRawType);
    if (!isEmpty(types)) {
      Type[] result = new Type[types.length];
      result[0] = TypeUtils.canonicalize(types[0]);
      result[1] = TypeUtils.canonicalize(types[1]);
      return result;
    }
    return new Type[0];
  }

  @Override
  public ElasticMapping resolve(Class<?> documentClass) {
    EsDocument document = findAnnotation(shouldNotNull(documentClass), EsDocument.class, false);
    ElasticIndexing indexing = ElasticIndexing.of(null, document, version);
    String typeName = resolveTypeName(documentClass, document);
    String versionPropertyName = document.versionPropertyName();
    boolean versioned = isNotBlank(versionPropertyName);
    VersionType versionType = document.versionType();
    boolean allIndexed = document.allIndexed();
    return new ElasticMapping(indexing, documentClass, typeName,
        resolveSchema(documentClass, typeName, allIndexed), versioned, versionPropertyName,
        versionType);
  }

  protected Map<String, Object> genFieldMapping(EsArray ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", ann.eleType());
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsBinary ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "binary");
    map.put("doc_values", ann.doc_values());
    map.put("store", ann.store());
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsBoolean ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsDate ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsEmbedded ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("dynamic", ann.dynamic());
    map.put("enabled", ann.enabled());
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsGeoPoint ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "geo_point");
    map.put("ignore_malformed", ann.ignore_malformed());
    map.put("ignore_z_value", ann.ignore_z_value());
    if (isNotBlank(ann.null_value())) {
      map.put("null_value", ann.null_value());
    }
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsGeoShape ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "geo_shape");
    EsProperty[] properties = ann.options();
    if (!isEmpty(properties)) {
      for (EsProperty property : properties) {
        map.put(property.name(), property.value());
      }
    }
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsIp ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsKeyword ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "keyword");
    map.put("boost", ann.boost());
    map.put("doc_values", ann.doc_values());
    map.put("index", ann.index());
    map.put("store", ann.store());
    map.put("eager_global_ordinals", ann.eager_global_ordinals());
    map.put("ignore_above", ann.ignore_above());
    map.put("index_options", ann.index_options().getValue());
    if (isNotBlank(ann.normalizer()) && !ann.normalizer().equalsIgnoreCase("$null$")) {
      map.put("normalizer", ann.normalizer());
    }
    map.put("norms", ann.norms());
    map.put("similarity", ann.similarity());
    if (ann.fields() != null && ann.fields().entries().length != 0) {
      Map<String, Object> fm = new LinkedHashMap<>();
      for (EsMultiFieldsEntry e : ann.fields().entries()) {
        shouldNotNull(e.fieldName());
        shouldBeFalse(fm.containsKey(e.fieldName()));
        Map<String, String> pm = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsNested ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "nested");
    map.put("dynamic", ann.dynamic());
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsNumeric ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsPercolator ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "percolator");
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsRange ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genFieldMapping(EsText ann) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "text");
    map.put("boost", ann.boost());
    map.put("fielddata", ann.fielddata());
    if (ann.fielddata() && ann.fielddata_frequency_filter() != null) {
      Map<String, Object> fm = new LinkedHashMap<>();
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
      Map<String, Object> fm = new LinkedHashMap<>();
      for (EsMultiFieldsEntry e : ann.fields().entries()) {
        shouldNotNull(e.fieldName());
        shouldBeFalse(fm.containsKey(e.fieldName()));
        Map<String, Object> pm = new LinkedHashMap<>();
        for (EsMultiFieldsPair p : e.pairs()) {
          if (p.valueType() == java.lang.String.class) {
            pm.put(p.key(), p.value());
          } else if (p.valueType() == java.lang.Boolean.class) {
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
      map.put("index_prefixes", asMap("min_chars", ann.index_prefixes_min_chars(), "max_chars",
          ann.index_prefixes_max_chars()));
    }
    map.put("index_phrases", ann.index_phrases());
    return map;
  }

  protected Map<String, Object> genFieldMapping(EsTokenCount ann) {
    Map<String, Object> map = new LinkedHashMap<>();
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

  protected Map<String, Object> genPrimitiveTypeDfltMapping(Class<?> cls) {
    Map<String, Object> map = new LinkedHashMap<>();
    Class<?> tcls = cls.isArray() ? cls.getComponentType() : cls;
    Class<?> acls = primitiveToWrapper(tcls);
    if (Boolean.class.isAssignableFrom(acls)) {
      map.put("type", "boolean");
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("null_value", false);
      map.put("store", false);
    } else if (Number.class.isAssignableFrom(acls)) {
      String esTypeName = acls.getSimpleName().toLowerCase(Locale.ENGLISH);
      if (acls.equals(BigDecimal.class) || acls.equals(Double.class) || acls.equals(Float.class)) {
        esTypeName = "double";
      } else if (acls.equals(BigInteger.class)) {
        esTypeName = "long";
      }
      map.put("type", esTypeName);
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("store", false);
      map.put("coerce", true);
      map.put("include_in_all", false);
      map.put("ignore_malformed", false);
    } else if (Character.class.isAssignableFrom(acls)) {
      map.put("type", "keyword");
      map.put("boost", 1.0f);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("ignore_above", 256);
      map.put("index_options", "docs");
      map.put("norms", false);
      // map.put("search_analyzer", "standard"); FIXME
      map.put("similarity", "classic");
    } else if (CharSequence.class.isAssignableFrom(acls)) {
      map.put("type", "text");
      map.put("boost", 1.0f);
      map.put("fielddata", false);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("index_options", "docs");
      map.put("norms", false);
      // map.put("position_increment_gap", 0); FIXME
      map.put("analyzer", "standard");
      map.put("search_analyzer", "standard");
      map.put("similarity", "classic");
      map.put("search_quote_analyzer", "standard");
      map.put("term_vector", "no");
    } else if (Temporal.class.isAssignableFrom(acls) || Date.class.isAssignableFrom(acls)) {
      map.put("type", "date");
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("format", DATE_FMT);
      map.put("include_in_all", false);
      map.put("ignore_malformed", false);
      map.put("store", false);
    } else if (Enum.class.isAssignableFrom(acls) || acls.equals(Locale.class)
        || acls.equals(Class.class) || acls.equals(Currency.class) || acls.equals(TimeZone.class)
        || acls.equals(URI.class) || acls.equals(URL.class)) {
      map.put("type", "keyword");
      map.put("boost", 1.0f);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("ignore_above", 256);
      map.put("index_options", "docs");
      map.put("norms", false);
      map.put("search_analyzer", "standard");
      map.put("similarity", "classic");
    }
    return map;
  }

  protected void handleCollectionField(Class<?> docCls, Field f, Map<String, Object> map) {
    Type colEleType = getCollectionFieldElementType(f, Collection.class);
    if (!(colEleType instanceof Class<?>)) {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being", docCls.getName(),
          f.getName()));
    } else {
      Class<?> colFieldCls = Class.class.cast(colEleType);
      if (isPrimitiveOrWrapper(colFieldCls)) {
        if (f.isAnnotationPresent(EsArray.class)) {
          handleFieldAnnotation(f, map);
        } else {
          Map<String, Object> tmp = genPrimitiveTypeDfltMapping(colFieldCls);
          if (!tmp.isEmpty()) {
            map.put(f.getName(), tmp);
          }
        }
      } else if (f.isAnnotationPresent(EsNested.class)
          && colFieldCls.isAnnotationPresent(EsEmbeddable.class)) {
        Map<String, Object> nestedProMap = new LinkedHashMap<>();
        handleFields(colFieldCls, nestedProMap);
        if (!nestedProMap.isEmpty()) {
          Map<String, Object> nestedMap = genFieldMapping(f.getAnnotation(EsNested.class));
          map.put(f.getName(), nestedMap);
          nestedMap.put("properties", nestedProMap);
        }
      }
    }
  }

  protected void handleEmbeddedField(Class<?> docCls, Field f, Map<String, Object> map) {
    if (f.isAnnotationPresent(EsEmbedded.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      handleFields(f.getType(), objProMap);
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsEmbedded.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else if (f.isAnnotationPresent(EsRange.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsRange.class)));
    } else {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being", docCls.getName(),
          f.getName()));
    }
  }

  protected void handleField(Class<?> documentClass, Field f, Map<String, Object> map) {
    Class<?> ft = shouldNotNull(f).getType();
    if (isPrimitiveOrWrapper(ft) || isPrimitiveArray(ft)) {
      handlePrimitiveOrWrappersField(documentClass, f, map);
    } else if (Collection.class.isAssignableFrom(ft)) {
      handleCollectionField(documentClass, f, map);
    } else if (Map.class.isAssignableFrom(ft) && f.isAnnotationPresent(EsMap.class)) {
      handleMapField(documentClass, f, map);
    } else if (ft.isAnnotationPresent(EsEmbedded.class)) {
      handleEmbeddedField(documentClass, f, map);
    } else if (ft.isAnnotationPresent(EsNested.class)) {
      handleNestedField(documentClass, f, map);
    } else {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being",
          documentClass.getName(), f.getName()));
    }
  }

  protected void handleFieldAnnotation(Field f, Map<String, Object> map) {
    if (shouldNotNull(f).isAnnotationPresent(EsArray.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsArray.class)));
    } else if (f.isAnnotationPresent(EsBinary.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsBinary.class)));
    } else if (f.isAnnotationPresent(EsBoolean.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsBoolean.class)));
    } else if (f.isAnnotationPresent(EsDate.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsDate.class)));
    } else if (f.isAnnotationPresent(EsGeoPoint.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsGeoPoint.class)));
    } else if (f.isAnnotationPresent(EsGeoShape.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsGeoShape.class)));
    } else if (f.isAnnotationPresent(EsIp.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsIp.class)));
    } else if (f.isAnnotationPresent(EsKeyword.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsKeyword.class)));
    } else if (f.isAnnotationPresent(EsNumeric.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsNumeric.class)));
    } else if (f.isAnnotationPresent(EsPercolator.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsPercolator.class)));
    } else if (f.isAnnotationPresent(EsRange.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsRange.class)));
    } else if (f.isAnnotationPresent(EsText.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsText.class)));
    } else if (f.isAnnotationPresent(EsTokenCount.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsTokenCount.class)));
    }
  }

  protected void handleFields(Class<?> cls, Map<String, Object> map) {
    traverseFields(cls, f -> {
      if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())
          && (f.getDeclaringClass().equals(cls)
              || f.getDeclaringClass().isAnnotationPresent(EsMappedSuperclass.class)
              || f.getDeclaringClass().isAnnotationPresent(EsEmbeddable.class)
              || f.getDeclaringClass().isAnnotationPresent(EsDocument.class))) {
        handleField(cls, f, map);
      } else {
        logger.warning(() -> String
            .format("This type %s mapping is not supported for the time being", cls.getName()));
      }
    });
  }

  protected void handleMapField(Class<?> docCls, Field f, Map<String, Object> map) {
    Type[] mapEleTypes = getMapFieldKeyAndValueTypes(f, Map.class);
    if (mapEleTypes.length != 2 || !(mapEleTypes[0] instanceof Class<?>)
        || !(mapEleTypes[1] instanceof Class<?>)) {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being", docCls.getName(),
          f.getName()));
    } else {
      Class<?> keyCls = Class.class.cast(mapEleTypes[0]);
      Class<?> valCls = Class.class.cast(mapEleTypes[1]);
      if ((isPrimitiveOrWrapper(keyCls) || isPrimitiveArray(keyCls)
          || keyCls.isAnnotationPresent(EsEmbeddable.class))
          && (isPrimitiveOrWrapper(valCls) || isPrimitiveArray(valCls)
              || valCls.isAnnotationPresent(EsEmbeddable.class))) {
        // TODO FIXME
      }
    }
  }

  protected void handleNestedField(Class<?> docCls, Field f, Map<String, Object> map) {
    if (f.isAnnotationPresent(EsNested.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      handleFields(f.getType(), objProMap);
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsNested.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else if (f.isAnnotationPresent(EsRange.class)) {
      map.put(f.getName(), genFieldMapping(f.getAnnotation(EsRange.class)));
    } else {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being", docCls.getName(),
          f.getName()));
    }
  }

  protected void handlePrimitiveOrWrappersField(Class<?> docCls, Field f, Map<String, Object> map) {
    if (f.isAnnotationPresent(EsArray.class) || f.isAnnotationPresent(EsBinary.class)
        || f.isAnnotationPresent(EsBoolean.class) || f.isAnnotationPresent(EsDate.class)
        || f.isAnnotationPresent(EsGeoPoint.class) || f.isAnnotationPresent(EsGeoShape.class)
        || f.isAnnotationPresent(EsTokenCount.class) || f.isAnnotationPresent(EsIp.class)
        || f.isAnnotationPresent(EsKeyword.class) || f.isAnnotationPresent(EsNumeric.class)
        || f.isAnnotationPresent(EsPercolator.class) || f.isAnnotationPresent(EsText.class)) {
      handleFieldAnnotation(f, map);
    } else {
      logger.warning(() -> String.format(
          "Field mapping of this type %s.%s is not supported for the time being", docCls.getName(),
          f.getName()));
    }
  }

  protected Map<String, Object> resolveSchema(Class<?> documentClass, String typeName,
      boolean allIndexed) {
    Map<String, Object> bodyMap = new LinkedHashMap<>();
    Map<String, Object> fieldMap = new LinkedHashMap<>();
    handleFields(documentClass, fieldMap);
    bodyMap.put("properties", fieldMap);
    if (!allIndexed) {
      bodyMap.put("_all", asMap("enabled", false));
    }
    return asMap(typeName, bodyMap);
  }

  protected String resolveTypeName(Class<?> documentClass, EsDocument document) {
    return defaultObject(document.typeName(), documentClass.getSimpleName());
  }

  static class Test {
    List<String> collection;
    Map<String, BigInteger> map;
  }
}
