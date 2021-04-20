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
package org.corant.modules.query.shared;

import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.mapping.FetchQuery;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午9:54:08
 *
 */
public interface NamedQueryService extends QueryService<String, Object> {

  FetchResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier);

  class FetchResult {
    public final Querier fetchQuerier;
    public final List<Map<String, Object>> fetchedList;

    public FetchResult(Querier fetchQuerier, List<Map<String, Object>> fetchedList) {
      this.fetchQuerier = fetchQuerier;
      this.fetchedList = fetchedList;
    }
  }
}
