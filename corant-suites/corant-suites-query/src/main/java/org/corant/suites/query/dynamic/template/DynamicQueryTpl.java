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
package org.corant.suites.query.dynamic.template;

import java.util.List;
import java.util.Map;
import org.corant.suites.query.mapping.FetchQuery;

/**
 * asosat-query
 *
 * @author bingo 下午3:51:20
 *
 */
public interface DynamicQueryTpl<T> {

  long getCachedTimestemp();

  List<FetchQuery> getFetchQueries();

  Map<String, Class<?>> getParamConvertSchema();

  String getQueryName();

  Class<?> getResultClass();

  Object getTemplate();

  T process(Map<String, Object> param);

}
