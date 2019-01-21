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
import static org.corant.shared.util.CollectionUtils.isEmpty;
import static org.corant.shared.util.FieldUtils.traverseFields;
import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.ObjectUtils.shouldBeEquals;
import static org.corant.shared.util.ObjectUtils.shouldBeFalse;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.split;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.genFieldMapping;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.genJoinMapping;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.getCollectionFieldEleType;
import static org.corant.suites.elastic.metadata.resolver.ResolverUtils.isSimpleType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.ClassPaths;
import org.corant.shared.util.ClassPaths.ClassInfo;
import org.corant.suites.elastic.Elastic6Constants;
import org.corant.suites.elastic.ElasticExtension;
import org.corant.suites.elastic.metadata.ElasticIndexing;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.ElasticSetting;
import org.corant.suites.elastic.metadata.annotation.EsAlias;
import org.corant.suites.elastic.metadata.annotation.EsArray;
import org.corant.suites.elastic.metadata.annotation.EsBinary;
import org.corant.suites.elastic.metadata.annotation.EsBoolean;
import org.corant.suites.elastic.metadata.annotation.EsChildDocument;
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
import org.corant.suites.elastic.metadata.annotation.EsParentDocument;
import org.corant.suites.elastic.metadata.annotation.EsPercolator;
import org.corant.suites.elastic.metadata.annotation.EsRange;
import org.corant.suites.elastic.metadata.annotation.EsText;
import org.corant.suites.elastic.metadata.annotation.EsTokenCount;
import org.elasticsearch.index.VersionType;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午2:58:30
 *
 */
@ApplicationScoped
public class DefaultElasticIndexingResolver implements ElasticIndexingResolver {

  @Inject
  protected Logger logger;

  @Inject
  protected ElasticExtension extension;

  protected Map<Class<?>, ElasticIndexing> classIndices = new ConcurrentHashMap<>();

  protected Map<String, ElasticIndexing> namedIndices = new ConcurrentHashMap<>();

  protected Map<Class<?>, ElasticMapping> classMaps = new ConcurrentHashMap<>();

  @Override
  public ElasticIndexing get(Class<?> documentClass) {
    return classIndices.get(documentClass);
  }

  protected void buildIndex(Class<?> docCls) {
    EsDocument doc = findAnnotation(shouldNotNull(docCls), EsDocument.class, false);
    VersionType versionType = doc.versionType();
    String indexName = shouldNotNull(doc.indexName());
    shouldNotNull(extension.getConfig(indexName));
    EsParentDocument poc = findAnnotation(shouldNotNull(docCls), EsParentDocument.class, false);
    Map<String, Object> propertiesSchema = resolveSchema(docCls);
    ElasticMapping mapping = null;
    if (poc != null) {
      shouldNotNull(poc.fieldName());
      Class<?>[] childClses = poc.children();
      shouldBeFalse(isEmpty(childClses));
      mapping = new ElasticMapping(docCls, shouldNotNull(poc.name()), versionType);
      for (Class<?> childCls : childClses) {
        buildIndex(childCls, mapping, propertiesSchema);
      }
      shouldBeTrue(propertiesSchema.put(poc.fieldName(),
          asMap("relations", genJoinMapping(mapping))) == null);
    } else {
      mapping = new ElasticMapping(docCls, null, versionType);
    }
    ElasticSetting setting = new ElasticSetting(extension.getConfig(indexName).getSetting());
    ElasticIndexing indexing = new ElasticIndexing(indexName, setting, mapping,
        asMap(Elastic6Constants.TYP_NME, asMap("properties", propertiesSchema)));
    classIndices.put(mapping.getDocumentClass(), indexing);
    classMaps.put(docCls, mapping);
    for (ElasticMapping childMapping : mapping) {
      classIndices.put(childMapping.getDocumentClass(), indexing);
      classMaps.put(childMapping.getDocumentClass(), childMapping);
    }
    namedIndices.put(indexName, indexing);
  }

  protected void buildIndex(Class<?> childDocCls, ElasticMapping parentMapping,
      Map<String, Object> propertiesSchema) {
    EsChildDocument coc = shouldNotNull(findAnnotation(childDocCls, EsChildDocument.class, false));
    String childName = coc.name();
    VersionType versionType = coc.versionType();
    ElasticMapping childMapping = new ElasticMapping(childDocCls, childName, versionType);
    Map<String, Object> childPropertiesSchema = resolveSchema(childDocCls);
    parentMapping.getChildren().add(childMapping);
    childPropertiesSchema.forEach((k, s) -> {
      if (!propertiesSchema.containsKey(k)) {
        propertiesSchema.put(k, s);
      } else {
        shouldBeEquals(s, propertiesSchema.get(k));
      }
    });
    // next grand child
    if (!isEmpty(coc.children())) {
      for (Class<?> grandChild : coc.children()) {
        buildIndex(grandChild, childMapping, propertiesSchema);
      }
    }
  }

  protected Set<Class<?>> getDocumentClasses() {
    Set<String> docPaths = extension.getConfigs().values().stream()
        .flatMap(v -> asStream(split(";", v.getDocumentPaths(), true, true)))
        .collect(Collectors.toSet());
    Set<Class<?>> docClses = new LinkedHashSet<>();
    for (String docPath : docPaths) {
      ClassPaths.anyway(docPath).getClasses().map(ClassInfo::load)
          .filter(dc -> dc.isAnnotationPresent(EsDocument.class)).forEach(docClses::add);
    }
    return docClses;
  }

  protected void initialize() {
    Set<Class<?>> docClses = getDocumentClasses();
    for (Class<?> docCls : docClses) {
      buildIndex(docCls);
    }
  }

  protected void notSupportLog(Class<?> docCls, List<String> path) {
    logger.warning(
        () -> String.format("Field mapping of this type %s.%s is not supported for the time being.",
            docCls.getName(), String.join(".", path)));
  }

  @PostConstruct
  protected void onPostConstruct() {
    initialize();
  }

  protected void resolveClassSchema(Class<?> docCls, Map<String, Object> map, List<String> path) {
    traverseFields(docCls, f -> {
      if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())
          && (f.getDeclaringClass().equals(docCls)
              || f.getDeclaringClass().isAnnotationPresent(EsMappedSuperclass.class)
              || f.getDeclaringClass().isAnnotationPresent(EsEmbeddable.class)
              || f.getDeclaringClass().isAnnotationPresent(EsDocument.class))) {
        resolveFieldSchema(docCls, f, map, new LinkedList<>(path));
      } else {
        notSupportLog(docCls, path);
      }
    });
  }

  protected void resolveCollectionFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    Type ft = getCollectionFieldEleType(f, Collection.class);
    boolean handled = false;
    if (ft instanceof Class<?>) {
      Class<?> fcls = Class.class.cast(ft);
      if (isSimpleType(fcls)) {
        resolveSimpleFieldSchema(docCls, f, map, new LinkedList<>(path));
        handled = true;
      } else if (f.isAnnotationPresent(EsNested.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        resolveNestedFieldSchema(docCls, f, map, new LinkedList<>(path));
        handled = true;
      } else if (f.isAnnotationPresent(EsEmbedded.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        resolveEmbeddedFieldSchema(docCls, f, map, new LinkedList<>(path));
        handled = true;
      }
    }
    if (!handled) {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveEmbeddedFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsEmbedded.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      resolveClassSchema(f.getType(), objProMap, new LinkedList<>(path));
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsEmbedded.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    Class<?> ft = shouldNotNull(f).getType();
    List<String> curPath = new LinkedList<>(path);
    curPath.add(f.getName());
    if (isSimpleType(ft)) {
      resolveSimpleFieldSchema(docCls, f, map, new LinkedList<>(curPath));
    } else if (Collection.class.isAssignableFrom(ft)) {
      resolveCollectionFieldSchema(docCls, f, map, new LinkedList<>(curPath));
    } else if (Map.class.isAssignableFrom(ft) && f.isAnnotationPresent(EsMap.class)) {
      resolveMapFieldSchema(docCls, f, map, new LinkedList<>(curPath));
    } else if (ft.isAnnotationPresent(EsEmbedded.class)) {
      resolveEmbeddedFieldSchema(docCls, f, map, new LinkedList<>(curPath));
    } else if (ft.isAnnotationPresent(EsNested.class)) {
      resolveNestedFieldSchema(docCls, f, map, new LinkedList<>(curPath));
    } else {
      notSupportLog(docCls, path);
    }
    if (f.isAnnotationPresent(EsAlias.class)) {
      EsAlias aliasAnn = f.getAnnotation(EsAlias.class);
      map.put(aliasAnn.name(), genFieldMapping(aliasAnn, curPath));
    }
  }

  protected void resolveMapFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    notSupportLog(docCls, path);
  }

  protected void resolveNestedFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsNested.class)) {
      Map<String, Object> objProMap = new LinkedHashMap<>();
      resolveClassSchema(f.getType(), objProMap, new LinkedList<>(path));
      if (!objProMap.isEmpty()) {
        Map<String, Object> objMap = genFieldMapping(f.getAnnotation(EsNested.class));
        map.put(f.getName(), objMap);
        objMap.put("properties", objProMap);
      }
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected Map<String, Object> resolveSchema(Class<?> docCls) {
    Map<String, Object> fieldMap = new LinkedHashMap<>();
    resolveClassSchema(docCls, fieldMap, new LinkedList<>());
    return fieldMap;
  }

  protected void resolveSimpleFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
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
}
