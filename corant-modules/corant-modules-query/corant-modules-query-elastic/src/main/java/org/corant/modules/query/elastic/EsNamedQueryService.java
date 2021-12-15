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
package org.corant.modules.query.elastic;

import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.NamedQueryService;
import org.elasticsearch.common.unit.TimeValue;

/**
 * corant-modules-query-elastic
 *
 * <p>
 * Note: The query of Elastic Search is special, and there may be long-tail data, so when the result
 * set returned by select exceeds the limit, no throws exception.
 *
 * @author bingo 下午3:07:55
 *
 */
public interface EsNamedQueryService extends NamedQueryService {

  Map<String, Object> aggregate(String q, Object param);

  <T> Stream<T> scrolledSearch(String q, Object param, TimeValue scrollKeepAlive, int batchSize);

  Map<String, Object> search(String q, Object param);

}
