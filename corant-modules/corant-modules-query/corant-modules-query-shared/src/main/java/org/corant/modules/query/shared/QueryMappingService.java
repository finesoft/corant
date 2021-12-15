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

import static org.corant.context.Beans.findNamed;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.NEWLINE;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.QueryParser;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.spi.FetchQueryParameterResolver;
import org.corant.modules.query.spi.FetchQueryPredicate;
import org.corant.modules.query.spi.FetchQueryResultInjector;
import org.corant.modules.query.spi.QueryProvider;
import org.corant.modules.query.spi.QueryScriptResolver;
import org.corant.modules.query.spi.ResultHintResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午12:59:22
 *
 */
@ApplicationScoped
public class QueryMappingService {

  protected final Map<String, Query> queries = new HashMap<>();
  protected final ReadWriteLock rwl = new ReentrantReadWriteLock();
  protected volatile boolean initialized = false;

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = "corant.query.mapping-file.paths", defaultValue = "META-INF/**Query.xml")
  protected String mappingFilePaths;

  @Inject
  @Any
  protected Instance<QueryProvider> queryProvider;

  @Inject
  @Any
  protected Instance<QueryMappingClient> resolver;

  public String getMappingFilePaths() {
    return mappingFilePaths;
  }

  public List<Query> getQueries() {
    Lock l = rwl.readLock();
    try {
      l.lock();
      return new ArrayList<>(queries.values());
    } finally {
      l.unlock();
    }
  }

  public Query getQuery(String name) {
    Lock l = rwl.readLock();
    try {
      l.lock();
      return queries.get(name);
    } finally {
      l.unlock();
    }
  }

  public void reinitialize() {
    Lock l = rwl.writeLock();
    try {
      l.lock();
      initialized = false;
      queries.clear();
      if (!resolver.isUnsatisfied()) {
        resolver.forEach(QueryMappingClient::onServiceInitialize);
      }
      initialize();
    } finally {
      l.unlock();
    }
  }

  protected void doInitialize() {
    if (initialized) {
      return;
    }
    new QueryParser().parse(resolveMappingFilePaths()).forEach(m -> {
      List<String> brokens = m.selfValidate();
      if (!brokens.isEmpty()) {
        throw new QueryRuntimeException(String.join(NEWLINE, brokens));
      }
      m.getQueries().forEach(q -> {
        // q.setParamMappings(m.getParaMapping());// copy
        Query repeat = queries.get(q.getVersionedName());
        if (repeat != null) {
          throw new QueryRuntimeException(
              "The 'name' [%s] of query element in query file [%s] can not repeat, the previous query file [%s].",
              q.getVersionedName(), m.getUrl(), repeat.getMappingFilePath());
        } else {
          queries.put(q.getVersionedName(), q);
        }
        // check script CDI
        if (q.getScript().getType() == ScriptType.CDI
            && findNamed(QueryScriptResolver.class, q.getScript().getCode()).isEmpty()) {
          throw new QueryRuntimeException(
              "The script of query [%s] element in query file [%s] can't find the script resolver.",
              q.getVersionedName(), m.getUrl());
        }
        if (q.getHints() != null) {
          for (QueryHint qh : q.getHints()) {
            if (qh.getScript() != null && qh.getScript().isValid()
                && qh.getScript().getType() == ScriptType.CDI
                && findNamed(ResultHintResolver.class, qh.getScript().getCode()).isEmpty()) {
              throw new QueryRuntimeException(
                  "The script of query hint [%s] in query [%s] file [%s] can't find the script resolver.",
                  qh.getKey(), q.getVersionedName(), m.getUrl());
            }
          }
        }
        if (q.getFetchQueries() != null) {
          for (FetchQuery fq : q.getFetchQueries()) {
            if (fq.getPredicateScript() != null && fq.getPredicateScript().isValid()
                && fq.getPredicateScript().getType() == ScriptType.CDI
                && findNamed(FetchQueryPredicate.class, fq.getPredicateScript().getCode())
                    .isEmpty()) {
              throw new QueryRuntimeException(
                  "The script of fetch query predicate [%s] in query [%s] file [%s] can't find the script resolver.",
                  fq.getReferenceQuery().getVersionedName(), q.getVersionedName(), m.getUrl());
            } else if (fq.getInjectionScript() != null && fq.getInjectionScript().isValid()
                && fq.getInjectionScript().getType() == ScriptType.CDI
                && findNamed(FetchQueryResultInjector.class, fq.getInjectionScript().getCode())
                    .isEmpty()) {
              throw new QueryRuntimeException(
                  "The script of fetch query injection [%s] in query [%s] file [%s] can't find the script resolver.",
                  fq.getReferenceQuery().getVersionedName(), q.getVersionedName(), m.getUrl());
            } else if (fq.getParameters() != null) {
              for (FetchQueryParameter fp : fq.getParameters()) {
                if (fp.getScript() != null && fp.getScript().isValid()
                    && fp.getScript().getType() == ScriptType.CDI
                    && findNamed(FetchQueryParameterResolver.class, fp.getScript().getCode())
                        .isEmpty()) {
                  throw new QueryRuntimeException(
                      "The script of fetch query parameter [%s] in query [%s] file [%s] can't find the script resolver.",
                      fq.getReferenceQuery().getVersionedName(), q.getVersionedName(), m.getUrl());
                }
              }
            }
          }
        }
      });
    });
    queries.keySet().forEach(q -> {
      List<String> refs = new LinkedList<>();
      List<String> tmp = new LinkedList<>(queries.get(q).getVersionedFetchQueryNames());
      while (!tmp.isEmpty()) {
        String tq = tmp.remove(0);
        refs.add(tq);
        if (areEqual(tq, q)) {
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
      // FIXME CIRCULAR NO CHECK!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! bingo
      queryProvider.forEach(qp -> qp.provide().forEach(q -> queries.put(q.getVersionedName(), q)));
    }
    initialized = true;
    logger.info(() -> String.format("Found %s queries from mapping file path %s.", queries.size(),
        mappingFilePaths));
  }

  protected void initialize() {
    Lock l = rwl.writeLock();
    try {
      l.lock();
      doInitialize();
    } finally {
      l.unlock();
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    Lock l = rwl.writeLock();
    try {
      l.lock();
      initialize();
    } finally {
      l.unlock();
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    Lock l = rwl.writeLock();
    try {
      l.lock();
      queries.clear();
      initialized = false;
      logger.fine(() -> "Clear all query mappings.");
    } finally {
      l.unlock();
    }
  }

  protected String[] resolveMappingFilePaths() {
    Set<String> paths = new LinkedHashSet<>();
    if (!resolver.isUnsatisfied()) {
      resolver.forEach(s -> {
        Set<String> ps = s.getMappingFilePaths();
        if (isNotEmpty(ps)) {
          for (String p : ps) {
            if (isNotBlank(p)) {
              paths.add(p);
            }
          }
        }
      });
    } // FIXME still has not figured out
    if (isEmpty(paths)) {
      paths.addAll(resolvePaths(mappingFilePaths));
    }
    return paths.toArray(new String[paths.size()]);
  }

  protected Set<String> resolvePaths(String... paths) {
    Set<String> resolved = new LinkedHashSet<>();
    for (String path : paths) {
      Collections.addAll(resolved, split(path, ",", true, true));
    }
    return resolved;
  }

  public interface QueryMappingClient {

    Set<String> getMappingFilePaths();

    void onServiceInitialize();
  }
}
