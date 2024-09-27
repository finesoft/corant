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

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static org.corant.context.Beans.findAnyway;
import static org.corant.context.Beans.findNamed;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.NEWLINE;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Throwables.rethrow;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.corant.modules.query.QuerierConfig;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.QueryParser;
import org.corant.modules.query.mapping.SchemaNames;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.spi.ResultBeanMapperHintHandler;
import org.corant.modules.query.shared.spi.ResultBeanMapperHintHandler.ResultBeanMapper;
import org.corant.modules.query.spi.FetchQueryParameterResolver;
import org.corant.modules.query.spi.FetchQueryPredicate;
import org.corant.modules.query.spi.FetchQueryResultInjector;
import org.corant.modules.query.spi.QueryProvider;
import org.corant.modules.query.spi.QueryScriptResolver;
import org.corant.modules.query.spi.ResultHintResolver;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午12:59:22
 */
@ApplicationScoped
public class QueryMappingService {

  public static final String MAPPING_FILE_PATH_CFG_KEY = "corant.query.mapping-file.paths";
  public static final String DEFAULT_MAPPING_FILE_PATH = "META-INF/**Query.xml";

  protected final static ReadWriteLock rwl = new ReentrantReadWriteLock();
  protected final static AtomicLong initializedVersion = new AtomicLong(0);

  protected final Map<String, Query> queries = new HashMap<>();
  protected volatile boolean initialized = false;

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = MAPPING_FILE_PATH_CFG_KEY, defaultValue = DEFAULT_MAPPING_FILE_PATH)
  protected String mappingFilePaths;

  @Inject
  @Any
  protected Instance<QueryProvider> queryProviders;

  @Inject
  @Any
  protected Instance<QueryMappingClient> resolvers;

  @Inject
  @Any
  protected Instance<BeforeQueryMappingInitializeHandler> preInitializeHandlers;

  @Inject
  @Any
  protected Instance<AfterQueryMappingInitializedHandler> postInitializedHandlers;

  public static long getInitializedVersion() {
    return initializedVersion.get();
  }

  public String getMappingFilePaths() {
    return mappingFilePaths;
  }

  public Collection<Query> getQueries() {
    Lock l = rwl.readLock();
    l.lock();
    try {
      return unmodifiableCollection(queries.values());
    } finally {
      l.unlock();
    }
  }

  public Query getQuery(String name) {
    Lock l = rwl.readLock();
    l.lock();
    try {
      return queries.get(name);
    } finally {
      l.unlock();
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  @Experimental // NOTE since the query scripts may be cached in thread local
  public void reinitialize() {
    Lock l = rwl.writeLock();
    Throwable throwable = null;
    l.lock();
    try {
      logger.info("Start query mapping re-initialization.");
      doInitialize();
      logger.info("Completed query mapping re-initialization.");
    } catch (Exception ex) {
      throwable = ex;
      initialized = false;
      queries.clear();
    } finally {
      l.unlock();
      if (throwable != null) {
        rethrow(throwable);
      }
    }
  }

  protected void doInitialize() {
    initialized = false;
    if (!preInitializeHandlers.isUnsatisfied()) {
      final Collection<Query> oldQueries = getQueries();
      final long civn = getInitializedVersion();
      preInitializeHandlers.forEach(l -> l.beforeQueryMappingInitialize(oldQueries, civn));
    }
    queries.clear();
    new QueryParser().parse(resolveMappingFilePaths()).forEach(m -> {
      List<String> broken = m.selfValidate();
      if (!broken.isEmpty()) {
        throw new QueryRuntimeException(String.join(NEWLINE, broken));
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
            if (ResultBeanMapperHintHandler.HINT_NAME.equals(qh.getKey())) {
              Named named = ResultBeanMapperHintHandler.resolveNamedQualifier(qh);
              if (named == null || findAnyway(ResultBeanMapper.class, named).isEmpty()) {
                throw new QueryRuntimeException(
                    "The query hint [%s] in query [%s] file [%s] resolver can't find.", qh.getKey(),
                    q.getVersionedName(), m.getUrl());
              }
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
                  fq.getQueryReference().getVersionedName(), q.getVersionedName(), m.getUrl());
            } else if (fq.getInjectionScript() != null && fq.getInjectionScript().isValid()
                && fq.getInjectionScript().getType() == ScriptType.CDI
                && findNamed(FetchQueryResultInjector.class, fq.getInjectionScript().getCode())
                    .isEmpty()) {
              throw new QueryRuntimeException(
                  "The script of fetch query injection [%s] in query [%s] file [%s] can't find the script resolver.",
                  fq.getQueryReference().getVersionedName(), q.getVersionedName(), m.getUrl());
            } else if (fq.getParameters() != null) {
              for (FetchQueryParameter fp : fq.getParameters()) {
                if (fp.getScript() != null && fp.getScript().isValid()
                    && fp.getScript().getType() == ScriptType.CDI
                    && findNamed(FetchQueryParameterResolver.class, fp.getScript().getCode())
                        .isEmpty()) {
                  throw new QueryRuntimeException(
                      "The script of fetch query parameter [%s] in query [%s] file [%s] can't find the script resolver.",
                      fq.getQueryReference().getVersionedName(), q.getVersionedName(), m.getUrl());
                }
              }
            }
          }
        }
      });
    });
    queries.keySet().forEach(q -> {
      List<String> refs = new LinkedList<>();

      // check pagination count query
      Query query = queries.get(q);
      String paginationQueryName =
          query.getProperty(QuerierConfig.PRO_KEY_PAGINATION_COUNT_QUERY_NAME, String.class);
      if (isNotBlank(paginationQueryName)) {
        String paginationQuery = SchemaNames.resolveVersionedName(paginationQueryName,
            query.getProperty(QuerierConfig.PRO_KEY_PAGINATION_COUNT_QUERY_VERSION, String.class));
        if (!queries.containsKey(paginationQuery)) {
          throw new QueryRuntimeException(
              "Can't not find any [%s] pagination count query for query [%s]", paginationQuery, q);
        }
      }

      // check circular reference
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
              "The 'name' [%s] of 'fetch-query' in query [%s] in system can not found the referred query!",
              tq, q);
        }
        tmp.addAll(queries.get(tq).getVersionedFetchQueryNames());
      }
      refs.clear();
    });
    if (!queryProviders.isUnsatisfied()) {
      // FIXME CIRCULAR NO CHECK!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! bingo
      queryProviders.forEach(qp -> qp.provide().forEach(q -> queries.put(q.getVersionedName(), q)));
    }
    initialized = true;
    initializedVersion.incrementAndGet();
    if (!postInitializedHandlers.isUnsatisfied()) {
      final Collection<Query> newQueries = getQueries();
      final long civn = getInitializedVersion();
      postInitializedHandlers.forEach(l -> l.afterQueryMappingInitialized(newQueries, civn));
    }
    logger.info(() -> format("Found %s queries from mapping file path %s.", queries.size(),
        mappingFilePaths));
  }

  protected void initialize() {
    Lock l = rwl.writeLock();
    l.lock();
    Throwable throwable = null;
    try {
      logger.info("Start query mapping initialization.");
      doInitialize();
      logger.info("Complete query mapping initialization.");
    } catch (Exception ex) {
      throwable = ex;
      initialized = false;
      queries.clear();
    } finally {
      l.unlock();
      if (throwable != null) {
        rethrow(throwable);
      }
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    initialize();
  }

  @PreDestroy
  protected void onPreDestroy() {
    uninitialize();
  }

  protected String[] resolveMappingFilePaths() {
    Set<String> paths = new LinkedHashSet<>();
    if (!resolvers.isUnsatisfied()) {
      resolvers.forEach(s -> {
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

  protected void uninitialize() {
    Lock l = rwl.writeLock();
    l.lock();
    try {
      logger.info("Start query mapping un-initialization.");
      queries.clear();
      initialized = false;
      logger.info("Completed query mapping un-initialization.");
    } finally {
      l.unlock();
    }
  }

  @FunctionalInterface
  public interface AfterQueryMappingInitializedHandler extends Sortable {
    void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion);
  }

  @FunctionalInterface
  public interface BeforeQueryMappingInitializeHandler extends Sortable {
    void beforeQueryMappingInitialize(Collection<Query> queries, long initializedVersion);
  }

  @FunctionalInterface
  public interface QueryMappingClient {
    Set<String> getMappingFilePaths();
  }
}
