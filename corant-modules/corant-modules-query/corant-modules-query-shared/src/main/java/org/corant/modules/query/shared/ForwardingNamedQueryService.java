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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.List;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午11:08:42
 */
@ApplicationScoped
@RequiredConfiguration(key = "corant.query.enable-forwarding-named-query-service",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
public class ForwardingNamedQueryService implements NamedQueryService {

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected QueryObjectMapper objectMapper;

  @Inject
  protected Instance<NamedQueryServiceManager> queryServiceManagers;

  @Override
  public <T> Forwarding<T> forward(String q, Object p) {
    return getQueryService(q).forward(q, p);
  }

  @Override
  public <T> T get(String q, Object p) {
    return getQueryService(q).get(q, p);
  }

  @Override
  public QueryObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public <T> Paging<T> page(String q, Object p) {
    return getQueryService(q).page(q, p);
  }

  @Override
  public <T> List<T> select(String q, Object p) {
    return getQueryService(q).select(q, p);
  }

  @Override
  public <T> Stream<T> stream(String q, Object p) {
    return getQueryService(q).stream(q, p);
  }

  protected NamedQueryService getQueryService(String queryName) {
    Query query = shouldNotNull(mappingService.getQuery(queryName),
        () -> new QueryRuntimeException("Can't find any named '%s' query", queryName));
    final QueryType usedQueryType =
        defaultObject(query.getType(), NamedQueryServiceManager.DEFAULT_QUERY_TYPE);
    final String usedQualifier =
        defaultObject(query.getQualifier(), NamedQueryServiceManager.DEFAULT_QUALIFIER);
    for (NamedQueryServiceManager nqsm : queryServiceManagers) {
      if (nqsm.getType() == usedQueryType) {
        NamedQueryService nqs = nqsm.get(usedQualifier);
        if (nqs != null) {
          return nqs;
        }
      }
    }
    throw new QueryRuntimeException(
        "Can't find any query service with query type: %s, qualifier: %s", usedQueryType,
        usedQualifier);
  }
}
