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
package org.corant.suites.query.shared.mapping;

import static org.corant.shared.util.ObjectUtils.isEquals;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.spi.QueryProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-query
 *
 * @author bingo 下午12:59:22
 *
 */
@ApplicationScoped
public class QueryMappingService {

  private final Map<String, Query> queries = new HashMap<>();
  private volatile boolean initialized = false;

  @Inject
  Logger logger;

  @Inject
  @ConfigProperty(name = "query.mapping-file.paths", defaultValue = "META-INF/**QueryService.xml")
  String mappingFilePaths;

  @Inject
  @Any
  Instance<QueryProvider> queryProvider;

  public Query getQuery(String name) {
    return queries.get(name);
  }

  protected synchronized void initialize() {
    if (initialized) {
      return;
    }
    new QueryParser().parse(mappingFilePaths).forEach(m -> {
      List<String> brokens = m.selfValidate();
      if (!brokens.isEmpty()) {
        throw new QueryRuntimeException(String.join("\n", brokens));
      }
      m.getQueries().forEach(q -> {
        q.setParamMappings(m.getParaMapping());// copy
        if (queries.containsKey(q.getVersionedName())) {
          throw new QueryRuntimeException(
              "The 'name' [%s] of query element in query file [%s] can not repeat!",
              q.getVersionedName(), m.getUrl());
        } else {
          queries.put(q.getVersionedName(), q);
        }
      });
    });
    queries.keySet().forEach(q -> {
      List<String> refs = new LinkedList<>();
      List<String> tmp = new LinkedList<>(queries.get(q).getVersionedFetchQueryNames());
      while (!tmp.isEmpty()) {
        String tq = tmp.remove(0);
        refs.add(tq);
        if (isEquals(tq, q)) {
          throw new QueryRuntimeException(
              "The queries in system circular reference occurred on [%s -> %s]", q,
              String.join(" -> ", refs));
        }
        Query fq = queries.get(tq);
        if (fq == null) {
          throw new QueryRuntimeException(
              "The 'name' [%s] of 'fetch-query' in query [%s] in system can not found the refered query!",
              tq, q);
        }
        tmp.addAll(queries.get(tq).getVersionedFetchQueryNames());
      }
      refs.clear();
    });
    if (!queryProvider.isUnsatisfied()) {
      queryProvider.forEach(qp -> qp.provide().forEach(q -> queries.put(q.getVersionedName(), q)));
    }
    initialized = true;
    logger.info(() -> String.format("Find %s queries from mapping file path %s.", queries.size(),
        mappingFilePaths));
  }

  @PostConstruct
  protected void onPostConstruct() {
    initialize();
  }
}
