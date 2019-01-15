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
import static org.corant.shared.util.FieldUtils.traverseFields;
import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.genFieldMapping;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.getCollectionFieldEleType;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.isSimpleType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.elastic.metadata.ElasticIndexing;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.ElasticRelation;
import org.corant.suites.elastic.metadata.annotation.EsAlias;
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
import org.corant.suites.elastic.metadata.annotation.EsNested;
import org.corant.suites.elastic.metadata.annotation.EsNumeric;
import org.corant.suites.elastic.metadata.annotation.EsPercolator;
import org.corant.suites.elastic.metadata.annotation.EsRange;
import org.corant.suites.elastic.metadata.annotation.EsRelation;
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

  @Override
  public ElasticMapping resolve(Class<?> documentClass) {
    EsDocument document = findAnnotation(shouldNotNull(documentClass), EsDocument.class, false);
    ElasticIndexing indexing = ElasticIndexing.of(null, document, version);
    String versionPropertyName = document.versionPropertyName();
    boolean versioned = isNotBlank(versionPropertyName);
    VersionType versionType = document.versionType();
    ElasticRelation relation = null;
    EsRelation relAnn = findAnnotation(shouldNotNull(documentClass), EsRelation.class, false);
    if (relAnn != null) {
      relation = new ElasticRelation(documentClass, relAnn);
    }

    return new ElasticMapping(indexing, documentClass, resolveSchema(documentClass, relation),
        relation, versioned, versionPropertyName, versionType);
  }

  public Map<String, Object> resolveSchema(Class<?> documentClass, ElasticRelation relation) {
    Map<String, Object> bodyMap = new LinkedHashMap<>();
    Map<String, Object> fieldMap = new LinkedHashMap<>();
    handleFields(documentClass, fieldMap, new LinkedList<>());
    if (relation != null) {
      fieldMap.putAll(relation.genSchema());
    }
    bodyMap.put("properties", fieldMap);
    return asMap("_doc", bodyMap);
  }

  protected void handleCollectionField(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    Type ft = getCollectionFieldEleType(f, Collection.class);
    boolean handled = false;
    if (ft instanceof Class<?>) {
      Class<?> fcls = Class.class.cast(ft);
      if (isSimpleType(fcls)) {
        handleSimpleField(docCls, f, map, new LinkedList<>(path));
        handled = true;
      } else if (f.isAnnotationPresent(EsNested.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        handleNestedField(docCls, f, map, new LinkedList<>(path));
        handled = true;
      } else if (f.isAnnotationPresent(EsEmbedded.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        handleEmbeddedField(docCls, f, map, new LinkedList<>(path));
        handled = true;
      }
    }
    if (!handled) {
      notSupportLog(docCls, path);
    }
  }

  protected void handleEmbeddedField(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsEmbedded.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      handleFields(f.getType(), objProMap, new LinkedList<>(path));
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsEmbedded.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void handleField(Class<?> docCls, Field f, Map<String, Object> map, List<String> path) {
    Class<?> ft = shouldNotNull(f).getType();
    List<String> curPath = new LinkedList<>(path);
    curPath.add(f.getName());
    if (isSimpleType(ft)) {
      handleSimpleField(docCls, f, map, new LinkedList<>(curPath));
    } else if (Collection.class.isAssignableFrom(ft)) {
      handleCollectionField(docCls, f, map, new LinkedList<>(curPath));
    } else if (Map.class.isAssignableFrom(ft) && f.isAnnotationPresent(EsMap.class)) {
      handleMapField(docCls, f, map, new LinkedList<>(curPath));
    } else if (ft.isAnnotationPresent(EsEmbedded.class)) {
      handleEmbeddedField(docCls, f, map, new LinkedList<>(curPath));
    } else if (ft.isAnnotationPresent(EsNested.class)) {
      handleNestedField(docCls, f, map, new LinkedList<>(curPath));
    } else {
      notSupportLog(docCls, path);
    }
    if (f.isAnnotationPresent(EsAlias.class)) {
      EsAlias aliasAnn = f.getAnnotation(EsAlias.class);
      map.put(aliasAnn.name(), genFieldMapping(aliasAnn, curPath));
    }
  }

  protected void handleFields(Class<?> docCls, Map<String, Object> map, List<String> path) {
    traverseFields(docCls, f -> {
      if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())
          && (f.getDeclaringClass().equals(docCls)
              || f.getDeclaringClass().isAnnotationPresent(EsMappedSuperclass.class)
              || f.getDeclaringClass().isAnnotationPresent(EsEmbeddable.class)
              || f.getDeclaringClass().isAnnotationPresent(EsDocument.class))) {
        handleField(docCls, f, map, new LinkedList<>(path));
      } else {
        notSupportLog(docCls, path);
      }
    });
  }

  protected void handleMapField(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    notSupportLog(docCls, path);
  }

  protected void handleNestedField(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsNested.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      handleFields(f.getType(), objProMap, new LinkedList<>(path));
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsNested.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void handleRelation(EsRelation ann) {

  }

  protected void handleSimpleField(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
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
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void notSupportLog(Class<?> docCls, List<String> path) {
    logger.warning(
        () -> String.format("Field mapping of this type %s.%s is not supported for the time being.",
            docCls.getName(), String.join(".", path)));
  }

  protected String resolveTypeName(Class<?> documentClass, EsDocument document) {
    return defaultObject(document.typeName(), documentClass.getSimpleName());
  }

}
