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
package org.corant.modules.query.shared.dynamic;

import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午9:54:00
 */
public abstract class AbstractDynamicQuerierBuilder<P, S, Q extends DynamicQuerier<P, S>>
    extends AbstractNamedQuerierBuilder<Q> implements DynamicQuerierBuilder<P, S, Q> {

  protected AbstractDynamicQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler) {
    super(query, queryHandler, fetchQueryHandler);
  }
}
