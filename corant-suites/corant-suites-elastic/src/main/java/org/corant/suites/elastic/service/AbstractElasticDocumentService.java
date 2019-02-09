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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.isNotNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.elastic.Elastic6Constants;
import org.corant.suites.elastic.metadata.ElasticIndexing;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.resolver.ElasticIndexingResolver;
import org.corant.suites.elastic.model.ElasticDocument;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午6:37:55
 *
 */
@ApplicationScoped
public abstract class AbstractElasticDocumentService implements ElasticDocumentService {

  @Inject
  protected ElasticIndexingResolver indexingResolver;

  @Override
  public int bulkIndex(List<ElasticDocument> docList, boolean flush,
      Function<Class<? extends ElasticDocument>, ElasticIndexing> idxGetter,
      Function<Class<? extends ElasticDocument>, ElasticMapping> mapGetter) {
    Map<Class<? extends ElasticDocument>, List<ElasticDocument>> docMap = new HashMap<>();
    for (ElasticDocument doc : shouldNotNull(docList)) {
      if (isNotNull(doc)) {
        docMap.computeIfAbsent(doc.getClass(), (c) -> new ArrayList<>()).add(doc);
      }
    }
    BulkRequestBuilder brb = getTransportClient().prepareBulk()
        .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE);
    for (Entry<Class<? extends ElasticDocument>, List<ElasticDocument>> entry : docMap.entrySet()) {
      Class<? extends ElasticDocument> docCls = entry.getKey();
      ElasticIndexing indexing = idxGetter.apply(docCls);
      ElasticMapping mapping = mapGetter.apply(docCls);
      List<ElasticDocument> docs = entry.getValue();
      for (ElasticDocument doc : docs) {
        IndexRequest rb = indexRequestBuilder(indexing.getName(), doc.getEsId(), doc.getEsRId(),
            doc.getEsPId(), mapping.toMap(doc), false, 0l, null).request();
        brb.add(rb);
      }
    }
    try {
      return Arrays.stream(brb.execute().actionGet().getItems()).map(x -> x.isFailed() ? 0 : 1)
          .reduce(Integer.valueOf(0), Integer::sum);
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

  public abstract TransportClient getTransportClient();

  @Override
  public boolean index(String indexName, String id, String routingId, String parentId,
      Map<?, ?> obj, boolean flush, long version, VersionType versionType) {
    try {
      return indexRequestBuilder(indexName, id, routingId, parentId, obj, flush, version,
          versionType).get().getResult() != Result.NOOP;
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
        srb.setFetchSource(pops, new String[0]);
      }
      return Arrays.stream(srb.get().getHits().getHits()).map(x -> x.getSourceAsMap())
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  protected IndexRequestBuilder indexRequestBuilder(String indexName, String id, String routingId,
      String parentId, Map<?, ?> obj, boolean flush, long version, VersionType versionType) {
    IndexRequestBuilder rb =
        getTransportClient().prepareIndex(indexName, Elastic6Constants.TYP_NME, id)
            .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE)
            .setSource(XContentType.SMILE, obj);
    if (isNotBlank(routingId)) {
      rb.setRouting(routingId);
    }
    if (isNotBlank(parentId)) {
      rb.setParent(parentId);
    }
    if (versionType != VersionType.INTERNAL) {
      shouldBeTrue(version > 0);
      rb.setVersion(version);
      rb.setVersionType(versionType);
    }
    return rb;
  }
}
