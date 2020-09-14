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
package org.corant.suites.elastic.service;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.forceCast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.suites.elastic.Elastic6Constants;
import org.corant.suites.elastic.metadata.ElasticIndexing;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.resolver.ElasticIndexingResolver;
import org.corant.suites.elastic.metadata.resolver.ElasticObjectMapper;
import org.corant.suites.elastic.model.ElasticDocument;
import org.corant.suites.elastic.model.ElasticVersionedDocument;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:10:36
 *
 */
public interface ElasticDocumentService {

  /**
   * Batch document indexing, this method can group and arrange batches to build indexes according
   * to the corresponding index configuration of documents.
   *
   * @param docs the documents to index
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @return number of successful
   * @see RefreshPolicy
   * @see BulkRequestBuilder
   */
  int bulkIndex(List<? extends ElasticDocument> docs, boolean flush);

  /**
   * Batch document indexing
   *
   * @param indexName the document index name
   * @param objs the documents
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @return index successfully or no
   *
   */
  int bulkIndex(String indexName, List<Map<?, ?>> objs, boolean flush);

  /**
   * Delete document indexing by index name and document id, no flush immediately.
   *
   * @param indexName the document index name
   * @param id the document id
   * @return delete successfully or no
   * @see #deleteByQuery(String, QueryBuilder, boolean)
   */
  default boolean delete(String indexName, String id) {
    return delete(indexName, id, false);
  }

  /**
   * Delete document indexing by index name and document id
   *
   * @param indexName the document index name
   * @param id the document id
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @return delete successfully or no
   * @see RefreshPolicy
   * @see #deleteByQuery(String, QueryBuilder, boolean)
   */
  default boolean delete(String indexName, String id, boolean flush) {
    return deleteByQuery(indexName,
        QueryBuilders.idsQuery().types(Elastic6Constants.TYP_NME).addIds(id), flush) > 0;
  }

  /**
   * Delete document indexing by index name and query builder
   *
   * @param indexName the document index name
   * @param queryBuiler query builder for filtering documents to be deleted
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @return number of deleted documents
   * @see RefreshPolicy
   * @see DeleteByQueryAction
   */
  long deleteByQuery(String indexName, QueryBuilder queryBuiler, boolean flush);

  /**
   * Get the document by document class and query builder
   *
   * @param <T> the document type
   * @param cls the document class
   * @param queryBuilder the query builder for selecting document
   * @return the document
   */
  default <T> T get(Class<T> cls, QueryBuilder queryBuilder) {
    ElasticIndexing indexing = shouldNotNull(resolveIndexing(cls));
    return get(cls, indexing.getName(), queryBuilder);
  }

  /**
   * Get the document by document class and document id
   *
   * @param <T> the document type
   * @param cls the document class
   * @param id the document id
   * @return the document
   */
  default <T> T get(Class<T> cls, String id) {
    return get(cls, QueryBuilders.idsQuery().types(Elastic6Constants.TYP_NME).addIds(id));
  }

  /**
   * Get the document by document class and document index name and query builder
   *
   * @param <T>
   * @param cls the document class
   * @param indexName document index name
   * @param queryBuilder the query builder for selecting document
   * @return the document
   */
  default <T> T get(Class<T> cls, String indexName, QueryBuilder queryBuilder) {
    List<T> list = select(cls, queryBuilder, null, 1);
    if (!isEmpty(list)) {
      return list.get(0);
    } else {
      return null;
    }
  }

  /**
   * Get the transport client
   *
   * @return getTransportClient
   */
  TransportClient getTransportClient();

  /**
   * Index single document, no flush immediately
   *
   * @param document the document to index
   * @return index successfully or no
   * @see #index(ElasticDocument, boolean)
   * @see #index(String, String, String, Map, boolean, long, VersionType)
   */
  default boolean index(ElasticDocument document) {
    shouldNotNull(document);
    return index(document, false);
  }

  /**
   * Index single document
   *
   * @param document the document to index
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @return index successfully or no
   */
  default boolean index(ElasticDocument document, boolean flush) {
    Class<?> docCls = shouldNotNull(document.getClass());
    ElasticIndexing indexing = shouldNotNull(resolveIndexing(docCls));
    ElasticMapping mapping = shouldNotNull(resolveMapping(docCls));
    if (document instanceof ElasticVersionedDocument) {
      ElasticVersionedDocument verDoc = (ElasticVersionedDocument) document;
      return index(indexing.getName(), document.getId(), document.getRId(), mapping.toMap(verDoc),
          flush, verDoc.getVn(), mapping.getVersionType());
    } else {
      return index(indexing.getName(), document.getId(), document.getRId(), mapping.toMap(document),
          flush, 0L, null);
    }
  }

  /**
   * Index document
   *
   * @param indexName the document index name
   * @param id the document id
   * @param routingId controls the shard routing of the request. Using this value to hash the
   *        shardand not the id.
   * @param obj the object to index
   * @param flush whether to refresh immediately, if true then use RefreshPolicy.IMMEDIATE else
   *        RefreshPolicy.NONE
   * @param version the version, which will cause the index operation to only be performed if a
   *        matchingversion exists and no changes happened on the doc since then.
   * @param versionType the versioning type
   * @return index successfully or no
   */
  boolean index(String indexName, String id, String routingId, Map<?, ?> obj, boolean flush,
      long version, VersionType versionType);

  /**
   * Resolve elastic indexing metadata by document class
   *
   * @param docCls
   * @return the resolved elastic indexing metadata
   * @see ElasticIndexing
   * @see ElasticIndexingResolver
   */
  ElasticIndexing resolveIndexing(Class<?> docCls);

  /**
   * Resolve elastic mapping metadata by document class
   *
   * @param docCls
   * @return the resolved elastic mapping metadata
   * @see ElasticMapping
   * @see ElasticIndexingResolver
   */
  ElasticMapping resolveMapping(Class<?> docCls);

  /**
   * Select documents by document class and query builder.
   *
   * @param <T> the document type
   * @param cls the document class
   * @param queryBuilder the query builder for filtering documents
   * @return the matched documents
   */
  default <T> List<T> select(Class<T> cls, QueryBuilder queryBuilder) {
    return select(cls, queryBuilder, null, Elastic6Constants.DFLT_SELECT_SIZE);
  }

  /**
   * Select documents
   *
   * @param <T> the document type
   * @param cls the document class
   * @param qb the query builder for filtering documents
   * @param sb the sort builder for sorting documents
   * @param size the number of documents expected to be returned
   * @return the matched documents
   * @see #select(String, QueryBuilder, SortBuilder, int, String...)
   */
  default <T> List<T> select(Class<T> cls, QueryBuilder qb, SortBuilder<?> sb, int size) {
    ElasticIndexing indexing = shouldNotNull(resolveIndexing(cls));
    List<Map<String, Object>> rawResults = select(indexing.getName(), qb, sb, size);
    if (!isEmpty(rawResults)) {
      return forceCast(rawResults.stream().map(m -> ElasticObjectMapper.toObject(m, cls))
          .collect(Collectors.toList()));
    }
    return new ArrayList<>();
  }

  /**
   * Select data
   *
   * @param indexName the index name
   * @param qb the query builder for filtering data
   * @param sb the sort builder for sorting data
   * @param size the number of search hits to return. if < 0 Defaults to 64.
   * @param pops the _source should be returned with every hit, usually is field names array
   * @return the matched data
   */
  List<Map<String, Object>> select(String indexName, QueryBuilder qb, SortBuilder<?> sb, int size,
      String... pops);

}
