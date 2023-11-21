/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.Querier;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午11:41:34
 */
public interface FetchableNamedQueryService extends NamedQueryService {

  default FetchedResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    throw new NotSupportedException();
  }

  default void handleFetching(Object result, Querier parentQuerier) {
    throw new NotSupportedException();
  }

  class FetchedResult {
    public final FetchQuery fetchQuery;
    public final Querier fetchQuerier;
    public final List<Map<String, Object>> fetchedList;

    public FetchedResult(FetchQuery fetchQuery, Querier fetchQuerier,
        List<Map<String, Object>> fetchedList) {
      this.fetchQuery = fetchQuery;
      this.fetchQuerier = fetchQuerier;
      this.fetchedList = fetchedList;
    }
  }
}
