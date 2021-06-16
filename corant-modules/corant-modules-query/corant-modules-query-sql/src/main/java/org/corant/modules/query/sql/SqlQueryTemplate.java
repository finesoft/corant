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
package org.corant.modules.query.sql;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Objects.asStrings;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Streams.streamOf;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.datasource.shared.DriverManagerDataSource;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.sql.dialect.Dialect;
import org.corant.modules.query.sql.dialect.Dialect.DBMS;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Retry;
import org.corant.shared.util.Retry.RetryInterval;

/**
 * corant-modules-query-sql
 *
 * <p>
 * NOTE: unfinish yet!
 *
 * @author bingo 下午5:38:09
 *
 */
public class SqlQueryTemplate {

  protected static final Logger logger = Logger.getLogger(SqlQueryTemplate.class.getName());
  protected static final MapHandler mapHandler = new MapHandler();
  protected static final MapListHandler mapListHander = new MapListHandler();

  protected final QueryRunner runner;
  protected final Dialect dialect;
  protected final DataSource datasource;
  protected String sql;
  protected Object[] parameters = Objects.EMPTY_ARRAY;
  protected Map<String, Object> namedParameters = new HashMap<>();
  protected boolean useNamedParameter = false;
  protected int limit = 16;
  protected int offset = 0;

  public SqlQueryTemplate(DataSource dataSource, DBMS dbms) {
    datasource = dataSource;
    dialect = defaultObject(dbms, () -> DBMS.MYSQL).instance();
    runner = new QueryRunner(dataSource);
  }

  public SqlQueryTemplate(DBMS dbms, String dataSourceName) {
    datasource = resolve(DataSourceService.class).get(dataSourceName);
    dialect = defaultObject(dbms, () -> DBMS.MYSQL).instance();
    runner = new QueryRunner(datasource);
  }

  public static SqlQueryTemplate mysql(DataSource dataSource) {
    return new SqlQueryTemplate(dataSource, DBMS.MYSQL);
  }

  public static SqlQueryTemplate mysql(String dataSourceName) {
    return new SqlQueryTemplate(DBMS.MYSQL, dataSourceName);
  }

  public static SqlQueryTemplate of(DBMS dbms, String dataSourceName) {
    return new SqlQueryTemplate(dbms, dataSourceName);
  }

  public static SqlQueryTemplate of(String jdbcUrl) {
    final DriverManagerDataSource ds = new DriverManagerDataSource(jdbcUrl);
    final DBMS dbms = DBMS.url(jdbcUrl);
    return new SqlQueryTemplate(ds, dbms);
  }

  public static SqlQueryTemplate of(String jdbcUrl, Properties properties) {
    final DriverManagerDataSource ds = new DriverManagerDataSource(jdbcUrl, properties);
    final DBMS dbms = DBMS.url(jdbcUrl);
    return new SqlQueryTemplate(ds, dbms);
  }

  public static SqlQueryTemplate of(String jdbcUrl, String driverClassName, Properties properties,
      String username, String password) {
    final DriverManagerDataSource ds =
        new DriverManagerDataSource(jdbcUrl, driverClassName, properties, username, password);
    final DBMS dbms = DBMS.url(jdbcUrl);
    return new SqlQueryTemplate(ds, dbms);
  }

  public static SqlQueryTemplate of(String jdbcUrl, String username, String password) {
    final DriverManagerDataSource ds = new DriverManagerDataSource(jdbcUrl, username, password);
    final DBMS dbms = DBMS.url(jdbcUrl);
    return new SqlQueryTemplate(ds, dbms);
  }

  public static SqlQueryTemplate of(String jdbcUrl, String driverClassName, String username,
      String password) {
    final DriverManagerDataSource ds =
        new DriverManagerDataSource(jdbcUrl, driverClassName, username, password);
    final DBMS dbms = DBMS.url(jdbcUrl);
    return new SqlQueryTemplate(ds, dbms);
  }

  public static SqlQueryTemplate oracle(DataSource dataSource) {
    return new SqlQueryTemplate(dataSource, DBMS.ORACLE);
  }

  public static SqlQueryTemplate oracle(String dataSourceName) {
    return new SqlQueryTemplate(DBMS.ORACLE, dataSourceName);
  }

  public static SqlQueryTemplate postgre(DataSource dataSource) {
    return new SqlQueryTemplate(dataSource, DBMS.POSTGRE);
  }

  public static SqlQueryTemplate postgre(String dataSourceName) {
    return new SqlQueryTemplate(DBMS.POSTGRE, dataSourceName);
  }

  public static SqlQueryTemplate sql2012(DataSource dataSource) {
    return new SqlQueryTemplate(dataSource, DBMS.SQLSERVER2012);
  }

  public static SqlQueryTemplate sql2012(String dataSourceName) {
    return new SqlQueryTemplate(DBMS.SQLSERVER2012, dataSourceName);
  }

  public Forwarding<Map<String, Object>> forward() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalized(namedParameters, sql)
        : SqlStatements.normalized(sql, parameters);
    String limitSql = dialect.getLimitSql(ps.getLeft(), offset, limit + 1);
    final Object[] params = ps.getRight();
    Forwarding<Map<String, Object>> result = Forwarding.inst();
    List<Map<String, Object>> list = query(limitSql, params);
    if (sizeOf(list) > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list);
  }

  public <T> Forwarding<T> forwardAs(final Class<T> clazz) {
    return forwardAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Forwarding<T> forwardAs(final Function<Object, T> converter) {
    Forwarding<Map<String, Object>> forwarding = forward();
    Forwarding<T> result = Forwarding.inst();
    return result.withHasNext(forwarding.hasNext())
        .withResults(forwarding.getResults().stream().map(converter).collect(Collectors.toList()));
  }

  public Map<?, ?> get() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalized(namedParameters, sql)
        : SqlStatements.normalized(sql, parameters);
    return get(ps.getLeft(), ps.getRight());
  }

  public <T> T getAs(final Class<T> clazz) {
    return getAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> T getAs(final Function<Object, T> converter) {
    Map<?, ?> r = get();
    return r == null ? null : converter.apply(r);
  }

  /**
   * The expected number of query result set or the expected size of the result set of each
   * iteration of the streaming query
   *
   * @see QueryParameter#getLimit()
   *
   * @param limit
   * @return limit
   */
  public SqlQueryTemplate limit(int limit) {
    this.limit = Objects.max(limit, 1);
    return this;
  }

  /**
   * @see QueryParameter#getOffset()
   * @param offset
   * @return offset
   */
  public SqlQueryTemplate offset(int offset) {
    this.offset = Objects.max(offset, 0);
    return this;
  }

  public Paging<Map<String, Object>> page() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalized(namedParameters, sql)
        : SqlStatements.normalized(sql, parameters);
    String useSql = ps.getLeft();
    Object[] params = ps.getRight();
    String limitSql = dialect.getLimitSql(useSql, offset, limit);
    try {
      List<Map<String, Object>> list = query(limitSql, params);
      Paging<Map<String, Object>> result = Paging.of(offset, limit);
      int size = sizeOf(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = dialect.getCountSql(useSql);
          result.withTotal(getMapInteger(get(totalSql, params), Dialect.COUNT_FIELD_NAME));
        }
      }
      return result.withResults(list);
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  public <T> Paging<T> page(final Class<T> clazz) {
    return pageAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Paging<T> pageAs(final Function<Object, T> converter) {
    Paging<Map<String, Object>> paging = page();
    Paging<T> result = Paging.of(paging.getOffset(), limit);
    result.withTotal(paging.getTotal());
    result.withResults(paging.getResults().stream().map(converter).collect(Collectors.toList()));
    return result;
  }

  public SqlQueryTemplate parameters(Map<String, Object> parameters) {
    namedParameters.clear();
    if (parameters != null) {
      namedParameters.putAll(parameters);
    }
    useNamedParameter = true;
    return this;
  }

  public SqlQueryTemplate parameters(Object... parameters) {
    this.parameters = new Object[parameters.length];
    System.arraycopy(parameters, 0, this.parameters, 0, parameters.length);
    useNamedParameter = false;
    return this;
  }

  public List<Map<String, Object>> select() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalized(namedParameters, sql)
        : SqlStatements.normalized(sql, parameters);
    return query(ps.key(), ps.value());
  }

  public <T> List<T> selectAs(final Class<T> clazz) {
    return selectAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> List<T> selectAs(final Function<Object, T> converter) {
    return select().stream().map(converter).collect(Collectors.toList());
  }

  public <T> T single(final Class<T> clazz) {
    List<Map<String, Object>> result = select();
    if (isEmpty(result)) {
      return null;
    } else {
      shouldBeTrue(result.size() == 1 && result.get(0).size() == 1, () -> new QueryRuntimeException(
          "The size %s of query result set must not greater than one and the result record must have only one field. SQL: %s.",
          result.size(), sql));
      return toObject(result.get(0).entrySet().iterator().next().getValue(), clazz);
    }
  }

  public SqlQueryTemplate sql(String sql) {
    this.sql = shouldNotBlank(sql);
    return this;
  }

  public Stream<Map<String, Object>> stream() {
    return stream(StreamConfig.DFLT_INST);
  }

  public Stream<Map<String, Object>> stream(StreamConfig config) {
    return streamOf(new Iterator<Map<String, Object>>() {
      Forwarding<Map<String, Object>> buffer = null;
      int counter = 0;
      Map<String, Object> next = null;

      @Override
      public boolean hasNext() {
        initialize();
        if (!config.terminateIf(counter, next)) {
          if (!buffer.hasResults()) {
            if (buffer.hasNext()) {
              revise(next, config);
              buffer.with(fetch(config));
              return buffer.hasResults();
            }
          } else {
            return true;
          }
        }
        return false;
      }

      @Override
      public Map<String, Object> next() {
        initialize();
        if (!buffer.hasResults()) {
          throw new NoSuchElementException();
        }
        counter++;
        next = buffer.getResults().remove(0);
        return next;
      }

      private Forwarding<Map<String, Object>> fetch(StreamConfig config) {
        if (config.needRetry()) {
          return Retry.retryer().times(config.retryTimes).interval(config.retryInterval)
              .thrower(
                  isNotEmpty(config.stopOn) ? (i, e) -> config.stopOn.contains(e.getClass()) : null)
              .execute(() -> forward());
        } else {
          return forward();
        }
      }

      private void initialize() {
        if (buffer == null) {
          buffer = defaultObject(fetch(config), Forwarding::inst);
          counter = buffer.hasResults() ? 1 : 0;
        }
      }

      private void revise(Map<String, Object> next, StreamConfig config) {
        boolean revise = false;
        if (useNamedParameter) {
          if (config.namedParameterReviser != null) {
            config.namedParameterReviser.accept(next, namedParameters);
            revise = true;
          }
        } else {
          if (config.parameterReviser != null) {
            config.parameterReviser.accept(next, parameters);
            revise = true;
          }
        }
        if (!revise) {
          offset += limit;
        }
      }
    });
  }

  public <T> Stream<T> streamAs(final Class<T> clazz) {
    return streamAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Stream<T> streamAs(final Function<Object, T> converter) {
    return stream().map(converter);
  }

  protected Map<String, Object> get(String sql, Object... parameter) {
    try {
      logger.fine(() -> String.format("%nQuery parameter: [%s]%nQuery SQL:%n%s",
          String.join(", ", asStrings(parameter)), sql));
      return defaultObject(runner.query(sql, mapHandler, parameter), HashMap::new);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected List<Map<String, Object>> query(String sql, Object... parameter) {
    try {
      logger.fine(() -> String.format("%nQuery parameter: [%s]%nQuery SQL:%n%s",
          String.join(", ", asStrings(parameter)), sql));
      return defaultObject(runner.query(sql, mapListHander, parameter), ArrayList::new);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e);
    }
  }

  public static class StreamConfig {

    static final Duration defRtyItl = Duration.ofSeconds(1L);

    static final StreamConfig DFLT_INST = new StreamConfig();

    protected int retryTimes = 0;

    protected RetryInterval retryInterval = RetryInterval.noBackoff(defRtyItl);

    protected BiPredicate<Integer, Object> terminater;

    protected BiConsumer<Map<String, Object>, Object[]> parameterReviser;

    protected BiConsumer<Map<String, Object>, Map<String, Object>> namedParameterReviser;

    protected Set<Class<?>> stopOn;

    public StreamConfig namedParameterReviser(
        BiConsumer<Map<String, Object>, Map<String, Object>> namedParameterReviser) {
      this.namedParameterReviser = namedParameterReviser;
      return this;
    }

    public boolean needRetry() {
      return retryTimes > 0;
    }

    public StreamConfig parameterReviser(
        BiConsumer<Map<String, Object>, Object[]> parameterReviser) {
      this.parameterReviser = parameterReviser;
      return this;
    }

    public StreamConfig retryInterval(RetryInterval retryInterval) {
      this.retryInterval =
          defaultObject(retryInterval, RetryInterval.noBackoff(Duration.ofMillis(2000L)));
      return this;
    }

    public StreamConfig retryTimes(int retryTimes) {
      this.retryTimes = max(retryTimes, 0);
      return this;
    }

    @SuppressWarnings("unchecked")
    public StreamConfig stopOn(Class<? extends Throwable>... throwables) {
      stopOn = linkedHashSetOf(throwables);
      return this;
    }

    public boolean terminateIf(Integer counter, Object current) {
      return terminater != null && !terminater.test(counter, current);
    }

    public StreamConfig terminater(BiPredicate<Integer, Object> terminater) {
      this.terminater = terminater;
      return this;
    }
  }
}
