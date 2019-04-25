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
package org.corant.suites.query.shared.dynamic;

import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * @author bingo 上午9:52:43
 *
 */
public interface DynamicQueryProcessor<Q, E> {

  long getCachedTimestemp();

  E getExecution();

  List<FetchQuery> getFetchQueries();

  List<QueryHint> getHints();

  Map<String, Class<?>> getParamConvertSchema();

  String getQueryName();

  Class<?> getResultClass();

  Q process(Map<String, Object> param);

}
