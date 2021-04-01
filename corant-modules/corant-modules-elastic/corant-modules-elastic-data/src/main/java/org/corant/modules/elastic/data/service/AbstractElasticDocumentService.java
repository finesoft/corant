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
package org.corant.modules.elastic.data.service;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.isNotNull;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.modules.elastic.data.Elastic6Constants;
import org.corant.modules.elastic.data.metadata.ElasticIndexing;
import org.corant.modules.elastic.data.metadata.ElasticMapping;
import org.corant.modules.elastic.data.metadata.resolver.ElasticIndexingResolver;
import org.corant.modules.elastic.data.model.ElasticDocument;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Strings;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午6:37:55
 *
 */
@SuppressWarnings("unchecked")
public abstract class AbstractElasticDocumentService implements ElasticDocumentService {

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  protected ElasticIndexingResolver indexingResolver;

  @Override
  public int bulkIndex(List<? extends ElasticDocument> docList, boolean flush) {
    // grouping docs by doc class
    Map<Class<? extends ElasticDocument>, List<ElasticDocument>> docMap = new HashMap<>();
    for (ElasticDocument doc : shouldNotNull(docList)) {
      if (isNotNull(doc)) {
        docMap.computeIfAbsent(doc.getClass(), c -> new ArrayList<>()).add(doc);
      }
    }
    // create bulk request builder
    final BulkRequestBuilder requestBuilder = getTransportClient().prepareBulk()
        .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE);

    // build requests
    for (Entry<Class<? extends ElasticDocument>, List<ElasticDocument>> entry : docMap.entrySet()) {
      Class<? extends ElasticDocument> docCls = entry.getKey();
      ElasticIndexing indexing = resolveIndexing(docCls);
      ElasticMapping mapping = resolveMapping(docCls);
      List<ElasticDocument> docs = entry.getValue();
      for (ElasticDocument doc : docs) {
        requestBuilder.add(indexRequestBuilder(indexing.getName(), doc.getId(), doc.getRId(),
            mapping.toMap(doc), false, 0L, null).request());
      }
    }
    docMap.clear();
    try {
      return Arrays.stream(requestBuilder.execute().actionGet().getItems()).map(response -> {
        if (response.isFailed()) {
          logger.log(Level.WARNING, response.getFailure().getCause(), response::getFailureMessage);
          return 0;
        }
        return 1;
      }).reduce(0, Integer::sum);
    } catch (ElasticsearchException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public int bulkIndex(String indexName, List<Map<?, ?>> objs, boolean flush) {
    if (isEmpty(objs)) {
      return 0;
    }
    // create bulk request builder
    final BulkRequestBuilder requestBuilder = getTransportClient().prepareBulk()
        .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE);
    for (Map<?, ?> doc : objs) {
      requestBuilder
          .add(indexRequestBuilder(indexName, getMapString(doc, "id"), null, doc, false, 0L, null)
              .request());
    }
    try {
      return Arrays.stream(requestBuilder.execute().actionGet().getItems()).map(response -> {
        if (response.isFailed()) {
          logger.log(Level.WARNING, response.getFailure().getCause(), response::getFailureMessage);
          return 0;
        }
        return 1;
      }).reduce(0, Integer::sum);
    } catch (ElasticsearchException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public long deleteByQuery(String indexName, QueryBuilder qb, boolean flush) {
    try {
      return DeleteByQueryAction.INSTANCE.newRequestBuilder(getTransportClient()).filter(qb)
          .refresh(flush).source(indexName).get().getDeleted();
    } catch (ElasticsearchException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public TransportClient getTransportClient() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean index(String indexName, String id, String routingId, Map<?, ?> obj, boolean flush,
      long version, VersionType versionType) {
    try {
      return indexRequestBuilder(indexName, id, routingId, obj, flush, version, versionType).get()
          .getResult() != Result.NOOP;
    } catch (ElasticsearchException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public ElasticIndexing resolveIndexing(Class<?> docCls) {
    return indexingResolver.getIndexing(docCls);
  }

  @Override
  public ElasticMapping resolveMapping(Class<?> docCls) {
    return indexingResolver.getMapping(docCls);
  }

  @Override
  public List<Map<String, Object>> select(String indexName, QueryBuilder qb, SortBuilder<?> sb,
      int size, String... pops) {
    if (isNotNull(qb)) {
      final SearchRequestBuilder srb = getTransportClient().prepareSearch(indexName).setQuery(qb)
          .setSize(size < 0 ? Elastic6Constants.DFLT_SELECT_SIZE : size);
      if (isNotNull(sb)) {
        srb.addSort(sb);
      }
      if (pops.length > 0) {
        srb.setFetchSource(pops, Strings.EMPTY_ARRAY);
      }
      return Arrays.stream(srb.get().getHits().getHits()).map(SearchHit::getSourceAsMap)
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  protected IndexRequestBuilder indexRequestBuilder(String indexName, String id, String routingId,
      Map<?, ?> obj, boolean flush, long version, VersionType versionType) {
    return indexRequestBuilderx(indexName, id, routingId, null, obj, flush, version, versionType);
  }

  protected IndexRequestBuilder indexRequestBuilderx(String indexName, String id, String routingId,
      String parentId, Map<?, ?> obj, boolean flush, long version, VersionType versionType) {
    IndexRequestBuilder rb =
        getTransportClient().prepareIndex(indexName, Elastic6Constants.TYP_NME, id)
            .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE)
            .setSource((Map<String, ?>) obj, XContentType.SMILE);
    if (isNotBlank(routingId)) {
      rb.setRouting(routingId);
    }
    if (isNotBlank(parentId)) {
      rb.setParent(parentId);
    }
    VersionType useVersionType = defaultObject(versionType, VersionType.INTERNAL);
    if (useVersionType != VersionType.INTERNAL) {
      shouldBeTrue(version > 0);
      rb.setVersion(version);
      rb.setVersionType(useVersionType);
    }
    return rb;
  }
}
