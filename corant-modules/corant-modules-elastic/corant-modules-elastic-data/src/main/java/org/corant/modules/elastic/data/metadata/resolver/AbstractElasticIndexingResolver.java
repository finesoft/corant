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

import static org.corant.modules.elastic.data.metadata.resolver.ElasticObjectMapper.isSimpleType;
import static org.corant.modules.elastic.data.metadata.resolver.ResolverUtils.genFieldMapping;
import static org.corant.modules.elastic.data.metadata.resolver.ResolverUtils.genJoinMapping;
import static org.corant.modules.elastic.data.metadata.resolver.ResolverUtils.getCollectionFieldEleType;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeEquals;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Fields.traverseFields;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.split;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.corant.modules.elastic.data.ElasticConfig;
import org.corant.modules.elastic.data.metadata.ElasticIndexing;
import org.corant.modules.elastic.data.metadata.ElasticMapping;
import org.corant.modules.elastic.data.metadata.ElasticSetting;
import org.corant.modules.elastic.data.metadata.annotation.EsAlias;
import org.corant.modules.elastic.data.metadata.annotation.EsArray;
import org.corant.modules.elastic.data.metadata.annotation.EsBinary;
import org.corant.modules.elastic.data.metadata.annotation.EsBoolean;
import org.corant.modules.elastic.data.metadata.annotation.EsChildDocument;
import org.corant.modules.elastic.data.metadata.annotation.EsDate;
import org.corant.modules.elastic.data.metadata.annotation.EsDocument;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbeddable;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbedded;
import org.corant.modules.elastic.data.metadata.annotation.EsGeoPoint;
import org.corant.modules.elastic.data.metadata.annotation.EsGeoShape;
import org.corant.modules.elastic.data.metadata.annotation.EsIp;
import org.corant.modules.elastic.data.metadata.annotation.EsKeyword;
import org.corant.modules.elastic.data.metadata.annotation.EsMappedSuperclass;
import org.corant.modules.elastic.data.metadata.annotation.EsNested;
import org.corant.modules.elastic.data.metadata.annotation.EsNumeric;
import org.corant.modules.elastic.data.metadata.annotation.EsParentDocument;
import org.corant.modules.elastic.data.metadata.annotation.EsPercolator;
import org.corant.modules.elastic.data.metadata.annotation.EsRange;
import org.corant.modules.elastic.data.metadata.annotation.EsText;
import org.corant.modules.elastic.data.metadata.annotation.EsTokenCount;
import org.corant.shared.resource.ClassResource;
import org.corant.shared.util.Resources;
import org.elasticsearch.index.VersionType;

/**
 * corant-modules-elastic-data
 *
 * <p>
 * This class is an abstract implementation of the ElasticIndexingResolver, managed by the CDI
 * container, and its scope of application is ApplicationScoped. When the container initializes this
 * class instance, it has completed the extraction of various metadatas
 * {@code AbstractElasticIndexingResolver#initialize()}.
 * </p>
 *
 * @author bingo 下午2:58:30
 *
 */
@ApplicationScoped
public abstract class AbstractElasticIndexingResolver implements ElasticIndexingResolver {

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  protected Map<Class<?>, ElasticIndexing> classIndices = new ConcurrentHashMap<>();

  protected Map<String, ElasticIndexing> namedIndices = new ConcurrentHashMap<>();

  protected Map<Class<?>, ElasticMapping> classMaps = new ConcurrentHashMap<>();

  @Override
  public ElasticIndexing getIndexing(Class<?> documentClass) {
    return classIndices.get(documentClass);
  }

  @Override
  public Map<String, ElasticIndexing> getIndexings() {
    return Collections.unmodifiableMap(namedIndices);
  }

  @Override
  public ElasticMapping getMapping(Class<?> documentClass) {
    return classMaps.get(documentClass);
  }

  @Override
  public Map<Class<?>, ElasticIndexing> getMappings() {
    return Collections.unmodifiableMap(classIndices);
  }

  protected void assembly(ElasticIndexing indexing, ElasticMapping mapping) {
    classIndices.put(mapping.getDocumentClass(), indexing);
    classMaps.put(mapping.getDocumentClass(), mapping);
    for (ElasticMapping childMapping : mapping) {
      classIndices.put(childMapping.getDocumentClass(), indexing);
      classMaps.put(childMapping.getDocumentClass(), childMapping);
    }
    namedIndices.put(indexing.getName(), indexing);
  }

  protected void buildMetadata(Class<?> docCls) {
    ElasticConfig config = getConfig();
    EsDocument doc = findAnnotation(shouldNotNull(docCls), EsDocument.class, false);
    VersionType versionType = doc.versionType();
    String indexName = shouldNotNull(doc.indexName());
    final Map<String, Object> propertiesSchema = resolveSchema(docCls);
    EsParentDocument poc = findAnnotation(shouldNotNull(docCls), EsParentDocument.class, false);
    ElasticMapping mapping;
    if (poc != null) {
      Class<?>[] childClses = poc.children();
      shouldBeFalse(isEmpty(childClses));
      String joinFieldName = shouldNotNull(poc.fieldName());
      mapping =
          new ElasticMapping(docCls, true, joinFieldName, shouldNotNull(poc.name()), versionType);
      for (Class<?> childCls : childClses) {
        buildMetadata(childCls, mapping, propertiesSchema);
      }
      shouldBeNull(propertiesSchema.put(shouldNotNull(poc.fieldName()), genJoinMapping(mapping)));
    } else {
      mapping = new ElasticMapping(docCls, true, null, null, versionType);
    }
    ElasticSetting setting = ElasticSetting.of(config.getSetting(), doc);
    final Map<String, Object> schema = new HashMap<>(mapOf("properties", propertiesSchema));
    ElasticIndexing indexing = new ElasticIndexing(indexName, setting, mapping, schema);
    assembly(indexing, mapping);
    logger.fine(() -> String.format("Build elastic index object for %s.", docCls.getName()));
  }

  protected void buildMetadata(Class<?> childDocCls, ElasticMapping parentMapping,
      Map<String, Object> propertiesSchema) {
    EsChildDocument coc = shouldNotNull(findAnnotation(childDocCls, EsChildDocument.class, false));
    VersionType versionType = coc.versionType();
    ElasticMapping childMapping = new ElasticMapping(childDocCls, false,
        parentMapping.getJoinFiledName(), coc.name(), versionType);
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
        buildMetadata(grandChild, childMapping, propertiesSchema);
      }
    }
  }

  /**
   * Create elastic indexes with resolved metadatas
   */
  protected abstract void createIndex();

  /**
   * Get the config
   *
   * @return the config
   */
  protected abstract ElasticConfig getConfig();

  /**
   * Get all the document classes that need to be indexed through the configuration information
   *
   * @return the document classes
   */
  protected Set<Class<?>> getDocumentClasses() {
    Set<String> docPaths = streamOf(split(getConfig().getDocumentPaths(), ",", true, true))
        .collect(Collectors.toSet());
    Set<Class<?>> docClses = new LinkedHashSet<>();
    for (String docPath : docPaths) {
      Resources.tryFromClassPath(docPath).filter(c -> c instanceof ClassResource)
          .map(c -> (ClassResource) c).map(ClassResource::load)
          .filter(dc -> dc.isAnnotationPresent(EsDocument.class)).forEach(docClses::add);
    }
    return docClses;
  }

  /**
   * Get all the document classes with related annotations, parse and create the index metadata
   * object of the document, and create the index immediately if necessary
   *
   * initialize
   */
  protected void initialize() {
    Set<Class<?>> docClses = getDocumentClasses();
    for (Class<?> docCls : docClses) {
      buildMetadata(docCls);
    }
    if (getConfig().isAutoUpdateSchema()) {
      createIndex();
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

  @PreDestroy
  protected synchronized void onPreDestroy() {
    classIndices.clear();
    namedIndices.clear();
    classMaps.clear();
    logger.fine(() -> "Clear indices schema caches");
  }

  protected void resolveClassSchema(Class<?> docCls, Map<String, Object> map, List<String> path) {
    traverseFields(docCls, f -> {
      List<String> currentPath = new LinkedList<>(path);
      currentPath.add(f.getName());
      if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())
          && (f.getDeclaringClass().equals(docCls) || isSimpleType(f.getType())
              || f.getDeclaringClass().isAnnotationPresent(EsMappedSuperclass.class)
              || f.getDeclaringClass().isAnnotationPresent(EsEmbeddable.class)
              || f.getDeclaringClass().isAnnotationPresent(EsDocument.class))) {
        resolveFieldSchema(docCls, f, map, currentPath);
      } else {
        notSupportLog(docCls, currentPath);
      }
    });
  }

  protected void resolveCollectionFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    Type ft = getCollectionFieldEleType(f, Collection.class);
    boolean handled = false;
    if (ft instanceof Class<?>) {
      Class<?> fcls = (Class<?>) ft;
      if (isSimpleType(fcls)) {
        resolveSimpleFieldSchema(docCls, f, map, new LinkedList<>(path));
        handled = true;
      } else if (f.isAnnotationPresent(EsNested.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        // resolveNestedFieldSchema(docCls, f, map, new LinkedList<>(path));
        resolveNestedFieldSchema(f.getName(), fcls, f.getAnnotation(EsNested.class), map, path);
        handled = true;
      } else if (f.isAnnotationPresent(EsEmbedded.class)
          && fcls.isAnnotationPresent(EsEmbeddable.class)) {
        // resolveEmbeddedFieldSchema(docCls, f, map, new LinkedList<>(path));
        resolveEmbeddedFieldSchema(f.getName(), fcls, f.getAnnotation(EsEmbedded.class), map, path);
        handled = true;
      }
    }
    if (!handled) {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveComponentFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsEmbedded.class)) {
      resolveEmbeddedFieldSchema(docCls, f, map, new LinkedList<>(path));
    } else if (f.isAnnotationPresent(EsNested.class)) {
      resolveNestedFieldSchema(docCls, f, map, new LinkedList<>(path));
    } else if (f.isAnnotationPresent(EsRange.class)) {
      resolveRangeFieldSchema(docCls, f, map);
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveEmbeddedFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsEmbedded.class)) {
      resolveEmbeddedFieldSchema(f.getName(), f.getType(), f.getAnnotation(EsEmbedded.class), map,
          path);
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveEmbeddedFieldSchema(String fn, Class<?> fc, EsEmbedded esEmbedded,
      Map<String, Object> map, List<String> path) {
    Map<String, Object> objProMap = new HashMap<>();
    resolveClassSchema(fc, objProMap, new LinkedList<>(path));
    if (!objProMap.isEmpty()) {
      Map<String, Object> objMap = genFieldMapping(esEmbedded);
      map.put(fn, objMap);
      objMap.put("properties", objProMap);
    }
  }

  protected void resolveFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    Class<?> ft = shouldNotNull(f).getType();
    if (isSimpleType(ft)) {
      resolveSimpleFieldSchema(docCls, f, map, new LinkedList<>(path));
    } else if (Collection.class.isAssignableFrom(ft)) {
      resolveCollectionFieldSchema(docCls, f, map, new LinkedList<>(path));
    } else if (Map.class.isAssignableFrom(ft)) {
      resolveMapFieldSchema(docCls, f, map, new LinkedList<>(path));
    } else {
      resolveComponentFieldSchema(docCls, f, map, new LinkedList<>(path));
    }
    if (f.isAnnotationPresent(EsAlias.class)) {
      EsAlias aliasAnn = f.getAnnotation(EsAlias.class);
      map.put(aliasAnn.name(), genFieldMapping(aliasAnn, path));
    }
  }

  protected void resolveMapFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    notSupportLog(docCls, path);
  }

  protected void resolveNestedFieldSchema(Class<?> docCls, Field f, Map<String, Object> map,
      List<String> path) {
    if (f.isAnnotationPresent(EsNested.class)) {
      resolveNestedFieldSchema(f.getName(), f.getType(), f.getAnnotation(EsNested.class), map,
          path);
    } else {
      notSupportLog(docCls, path);
    }
  }

  protected void resolveNestedFieldSchema(String fn, Class<?> fc, EsNested nested,
      Map<String, Object> map, List<String> path) {
    Map<String, Object> objProMap = new HashMap<>();
    resolveClassSchema(fc, objProMap, new LinkedList<>(path));
    if (!objProMap.isEmpty()) {
      Map<String, Object> objMap = genFieldMapping(nested);
      map.put(fn, objMap);
      objMap.put("properties", objProMap);
    }
  }

  protected void resolveRangeFieldSchema(Class<?> docCls, Field f, Map<String, Object> map) {
    map.put(f.getName(), genFieldMapping(f.getAnnotation(EsRange.class)));
  }

  protected Map<String, Object> resolveSchema(Class<?> docCls) {
    Map<String, Object> fieldMap = new HashMap<>();
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
