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
package org.corant.suites.query.elastic;

import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Streams.AbstractBatchHandlerSpliterator;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

/**
 * corant-suites-query-elastic
 *
 * @author bingo 上午10:22:11
 *
 */
public class EsScrollableSpliterator extends AbstractBatchHandlerSpliterator<Map<String, Object>> {

  public static final int DFLT_BATCH_SIZE = AbstractNamedQueryService.MAX_SELECT_SIZE;
  private final TimeValue scrollKeepAlive;
  private final TransportClient client;
  private SearchResponse searchResponse;
  private long batchHitCounts = 0;

  private final AtomicInteger seq = new AtomicInteger(0);

  public EsScrollableSpliterator(TransportClient client, QueryBuilder queryBuilder,
      String indexName, String typeName) {
    this(client, queryBuilder, indexName, typeName, null, DFLT_BATCH_SIZE,
        Functions.emptyConsumer());
  }

  public EsScrollableSpliterator(TransportClient client, QueryBuilder queryBuilder,
      String indexName, String typeName, TimeValue scrollKeepAlive, int batchSize,
      Consumer<Long> fn) {
    this(client, indexName, scrollKeepAlive, batchSize, fn, request -> request.setTypes(typeName)
        .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC).setQuery(queryBuilder));
  }

  public EsScrollableSpliterator(TransportClient client, String indexName, String script) {
    this(client, indexName, script, null, DFLT_BATCH_SIZE, Functions.emptyConsumer());
  }

  public EsScrollableSpliterator(TransportClient client, String indexName, String script,
      TimeValue scrollKeepAlive, int batchSize, Consumer<Long> fn) {
    this(client, indexName, scrollKeepAlive, batchSize, fn,
        request -> request.setSource(EsQueryExecutor.buildSearchSourceBuilder(script)));
  }

  public EsScrollableSpliterator(TransportClient client, String indexName,
      TimeValue scrollKeepAlive, int batchSize, Consumer<Long> fn,
      Consumer<SearchRequestBuilder> callback) {
    super(Long.MAX_VALUE, Spliterator.IMMUTABLE, batchSize, fn);
    this.client = client;
    this.scrollKeepAlive = defaultObject(scrollKeepAlive, () -> TimeValue.timeValueMinutes(1));
    SearchRequestBuilder request = client.prepareSearch(indexName);
    if (callback != null) {
      callback.accept(request);
    }
    searchResponse = request.setScroll(this.scrollKeepAlive).setSize(batchSize).get();
    batchHitCounts = searchResponse.getHits().getHits().length;
  }

  @Override
  public boolean tryAdvance(Consumer<? super Map<String, Object>> action) {
    final int currentSeq = seq.getAndIncrement();
    if (currentSeq < batchHitCounts) {
      action.accept(searchResponse.getHits().getAt(currentSeq).getSourceAsMap());
      return true;
    }
    boolean hasNext = nextBatch();
    if (!hasNext) {
      return false;
    }
    return tryAdvance(action);
  }

  @Override
  public Spliterator<Map<String, Object>> trySplit() {
    throw new NotSupportedException();
  }

  private boolean nextBatch() {
    searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
        .setScroll(scrollKeepAlive).execute().actionGet();
    seq.set(0);
    batchHitCounts = searchResponse.getHits().getHits().length;
    return batchHitCounts != 0;
  }

}
