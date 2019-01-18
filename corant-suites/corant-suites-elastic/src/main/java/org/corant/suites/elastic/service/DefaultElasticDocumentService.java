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

import static org.corant.shared.util.ObjectUtils.isNotNull;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.elastic.Elastic6Constants;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.resolver.DefaultElasticMappingResolver;
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
public class DefaultElasticDocumentService implements ElasticDocumentService {

  @Inject
  protected DefaultElasticMappingResolver mappingResolver;

  @Inject
  protected ElasticTransportClientService transportClientService;

  @Override
  public int bulkIndex(List<ElasticDocument> docList, boolean flush,
      Function<Class<? extends ElasticDocument>, ElasticMapping<?>> mapper) {
    Map<ElasticMapping<?>, List<ElasticDocument>> map = new LinkedHashMap<>();
    for (ElasticDocument doc : shouldNotNull(docList)) {
      ElasticMapping<?> mapping = doc == null ? null : mapper.apply(doc.getClass());
      if (isNotNull(mapping)) {
        map.computeIfAbsent(mapping, (k) -> new ArrayList<>()).add(doc);
      }
    }
    BulkRequestBuilder brb = getTransportClient().prepareBulk()
        .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE);
    for (Entry<ElasticMapping<?>, List<ElasticDocument>> entry : map.entrySet()) {
      ElasticMapping<?> mapping = entry.getKey();
      List<ElasticDocument> docs = entry.getValue();
      for (ElasticDocument doc : docs) {
        IndexRequest rb = indexRequestBuilder(mapping.getIndex().getName(), doc.getEsId(),
            doc.getEsParentId(), mapping.toMap(doc), false, 0l, null).request();
        brb.add(rb);
      }
    }
    if (flush) {
      brb.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
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

  public TransportClient getTransportClient() {
    return getTransportClientService().getTransportClient();
  }

  public ElasticTransportClientService getTransportClientService() {
    return transportClientService;
  }

  @Override
  public boolean index(String indexName, String id, String parentId, Map<?, ?> obj, boolean flush,
      long version, VersionType versionType) {
    try {
      return indexRequestBuilder(indexName, id, parentId, obj, flush, version, versionType).get()
          .getResult() != Result.NOOP;
    } catch (ElasticsearchException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public <T> ElasticMapping<T> resolveMapping(Class<T> docCls) {
    return mappingResolver.resolve(docCls);
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

  protected IndexRequestBuilder indexRequestBuilder(String indexName, String id, String parentId,
      Map<?, ?> obj, boolean flush, long version, VersionType versionType) {
    IndexRequestBuilder rb =
        getTransportClient().prepareIndex(indexName, Elastic6Constants.TYP_NME, id)
            .setRefreshPolicy(flush ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE)
            .setSource(obj, XContentType.SMILE);
    if (isNotBlank(parentId)) {
      rb.setParent(parentId);
    }
    if (isNotNull(versionType)) {
      shouldBeTrue(version > 0);
      rb.setVersion(version);
      rb.setVersionType(versionType);
    }
    return rb;
  }
}
