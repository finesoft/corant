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
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Maps.mapOf;
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
import java.util.function.BiFunction;
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
import org.corant.modules.datasource.shared.util.DbUtilBasicRowProcessor;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.sql.dialect.Dialect;
import org.corant.modules.query.sql.dialect.Dialect.DBMS;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.retry.BackoffStrategy;
import org.corant.shared.retry.BackoffStrategy.FixedBackoffStrategy;
import org.corant.shared.retry.RetryStrategy.MaxAttemptsRetryStrategy;
import org.corant.shared.retry.RetryStrategy.ThrowableClassifierRetryStrategy;
import org.corant.shared.retry.Retryer;
import org.corant.shared.retry.SynchronousRetryer;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Retry;

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
  protected static final MapHandler mapHandler = new MapHandler(DbUtilBasicRowProcessor.INST);
  protected static final MapListHandler mapListHander = new MapListHandler();

  protected final QueryRunner runner;
  protected final Dialect dialect;
  protected final DataSource dataSource;
  protected String sql;
  protected Object[] ordinaryParameters = Objects.EMPTY_ARRAY;
  protected Map<String, Object> namedParameters = new HashMap<>();
  protected boolean useNamedParameter = false;
  protected int limit = 16;
  protected int offset = 0;
  protected Map<String, Object> hints = new HashMap<>();

  public SqlQueryTemplate(DataSource dataSource, DBMS dbms) {
    this.dataSource = dataSource;
    dialect = defaultObject(dbms, () -> DBMS.MYSQL).instance();
    runner = new QueryRunner(dataSource);
  }

  public SqlQueryTemplate(DBMS dbms, String dataSourceName) {
    dataSource = resolve(DataSourceService.class).resolve(dataSourceName);
    dialect = defaultObject(dbms, () -> DBMS.MYSQL).instance();
    runner = new QueryRunner(dataSource);
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

  /**
   * Execute the query according to the {@link #offset} and {@link #limit} to return the result set
   * and return the mark of whether there are more records in this query. It is generally used to
   * iteratively query all the records that meet the conditions.
   */
  public Forwarding<Map<String, Object>> forward() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalize(sql, namedParameters)
        : SqlStatements.normalize(sql, ordinaryParameters);
    String limitSql = dialect.getLimitSql(ps.getLeft(), offset, limit + 1, hints);
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
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalize(sql, namedParameters)
        : SqlStatements.normalize(sql, ordinaryParameters);
    return get(ps.getLeft(), ps.getRight());
  }

  public <T> T getAs(final Class<T> clazz) {
    return getAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> T getAs(final Function<Object, T> converter) {
    Map<?, ?> r = get();
    return r == null ? null : converter.apply(r);
  }

  public SqlQueryTemplate hints(Object... hints) {
    this.hints.clear();
    this.hints.putAll(mapOf(hints));
    return this;
  }

  /**
   * The expected number of query result set or the expected size of the result set of each
   * iteration of the streaming query
   *
   * @see QueryParameter#getLimit()
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
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalize(sql, namedParameters)
        : SqlStatements.normalize(sql, ordinaryParameters);
    String useSql = ps.getLeft();
    Object[] params = ps.getRight();
    String limitSql = dialect.getLimitSql(useSql, offset, limit, hints);
    try {
      List<Map<String, Object>> list = query(limitSql, params);
      Paging<Map<String, Object>> result = Paging.of(offset, limit);
      int size = sizeOf(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = dialect.getCountSql(useSql, hints);
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
    ordinaryParameters = new Object[parameters.length];
    System.arraycopy(parameters, 0, ordinaryParameters, 0, parameters.length);
    useNamedParameter = false;
    return this;
  }

  public List<Map<String, Object>> select() {
    Pair<String, Object[]> ps = useNamedParameter ? SqlStatements.normalize(sql, namedParameters)
        : SqlStatements.normalize(sql, ordinaryParameters);
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

  /**
   * Set the query SQL statement, support the use of {@code ? } as a placeholder and the use of
   * {@code :name} as a placeholder. When using {@code ? }, the query parameter should be an array
   * {@link #parameters(Object...)}, and using {@code :name} the query parameter should be a Map
   * {@link #parameters(Map)}
   * <p>
   *
   * <pre>
   * Examples:
   *    1.Using {@code ? }:
   *        SqlQueryTemplate.oracle("ds")
   *        .sql("SELECT * FROM T_User WHERE id=? AND name=?")
   *        .parameter(123,"bingo")
   *        .get()
   *    2.Using {@code :name}:
   *        SqlQueryTemplate.oracle("ds")
   *        .sql("SELECT * FROM T_User WHERE id=:paramId AND name=:paramName")
   *        .parameter(Map.of("paramId",123,"paramName","bingo")
   *        .get()
   *
   *    The final SQL statements generated by the above two methods should be the same.
   * </pre>
   *
   * @param sql
   */
  public SqlQueryTemplate sql(String sql) {
    this.sql = shouldNotBlank(sql);
    return this;
  }

  public Stream<Map<String, Object>> stream() {
    return stream(StreamConfig.DFLT_INST);
  }

  /**
   * Use a specific configuration for streaming query.
   * <p>
   * If the value of offset > 0 or the value of retry time > 0 or the query parameter reviser is not
   * null then the query is through continuous {@link #forward()} query and output result set, which
   * means that the query is executed continuously in batches according to the size set by
   * {@link #limit}, otherwise, perform a complete query.
   *
   * If an exception occurs during the query, may retry according to the configuration, and may
   * modify the query parameters during each query.
   *
   * @param config the stream query configuration
   */
  public Stream<Map<String, Object>> stream(StreamConfig config) {
    if (offset > 0 || config.retryTimes > 0 || config.ordinaryParameterReviser != null
        || config.namedParameterReviser != null) {
      return streamOf(new ForwardIterator(this, config));
    } else {
      final Pair<String, Object[]> ps =
          useNamedParameter ? SqlStatements.normalize(sql, namedParameters)
              : SqlStatements.normalize(sql, ordinaryParameters);
      try {
        return new StreamableQueryRunner(null, limit, null, null, null).streamQuery(
            dataSource.getConnection(), true, ps.key(), mapHandler, config.terminater, ps.value());
      } catch (SQLException e) {
        throw new CorantRuntimeException(e);
      }
    }
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

  /**
   * corant-modules-query-sql
   *
   * <p>
   * Used to adjust the streaming query process. For example: when an exception occurs, can set a
   * retry rule to make the query regain; because the {@link SqlQueryTemplate#forward()} query
   * method adopted by the streaming query will execute multiple consecutive forward queries, this
   * configuration can modify its query parameters during each {@link SqlQueryTemplate#forward()}
   * query, especially It is suitable for streaming queries with unique keys and sorting of unique
   * keys, which can improve certain performance.
   *
   * @author bingo 上午10:04:25
   *
   */
  public static class StreamConfig {

    static final Duration one_second = Duration.ofSeconds(1L);

    static final StreamConfig DFLT_INST = new StreamConfig();

    protected int retryTimes = 0;

    protected BackoffStrategy retryBackoffStrategy = new FixedBackoffStrategy(one_second);

    protected BiPredicate<Integer, Object> terminater;

    protected BiFunction<Map<String, Object>, Object[], Integer> ordinaryParameterReviser;

    protected BiFunction<Map<String, Object>, Map<String, Object>, Integer> namedParameterReviser;

    protected Set<Class<? extends Throwable>> stopOn;

    /**
     * Set up a named query parameter adjustment consumer, where the first parameter of
     * {@code parameterReviser} is the last record of the result set of the previous batch of
     * queries; the second parameter is the query parameter, user can modify it according to the
     * last record and other conditions, the modified parameters can be used for the next batch of
     * queries.
     *
     * @param parameterReviser the parameter reviser
     */
    public StreamConfig namedParameterRevise(
        BiConsumer<Map<String, Object>, Map<String, Object>> parameterReviser) {
      return namedParameterReviser((r, p) -> {
        parameterReviser.accept(r, p);
        return 0;
      });
    }

    /**
     * Set up a named query parameter adjustment function, where the first parameter of
     * {@code parameterReviser} is the last record of the result set of the previous batch of
     * queries; the second parameter is the query parameter, user can modify it according to the
     * last record and other conditions, the modified parameters can be used for the next batch of
     * queries; the function return value will be used to update the {@link SqlQueryTemplate#offset}
     * for the next batch of queries.
     *
     * @param parameterReviser the parameter reviser
     */
    public StreamConfig namedParameterReviser(
        BiFunction<Map<String, Object>, Map<String, Object>, Integer> parameterReviser) {
      namedParameterReviser = parameterReviser;
      return this;
    }

    /**
     * Set up a ordinary query parameter adjustment consumer, where the first parameter of
     * {@code parameterReviser} is the last record of the result set of the previous batch of
     * queries; the second parameter is the query parameter, user can modify it according to the
     * last record and other conditions, the modified parameters can be used for the next batch of
     * queries.
     */
    public StreamConfig ordinaryParameterRevise(
        BiConsumer<Map<String, Object>, Object[]> parameterReviser) {
      return ordinaryParameterReviser((r, p) -> {
        parameterReviser.accept(r, p);
        return 0;
      });
    }

    /**
     * Set up a ordinary query parameter adjustment function, where the first parameter of
     * {@code parameterReviser} is the last record of the result set of the previous batch of
     * queries; the second parameter is the query parameter, user can modify it according to the
     * last record and other conditions, the modified parameters can be used for the next batch of
     * queries; the function return value will be used to update the {@link SqlQueryTemplate#offset}
     * for the next batch of queries.
     *
     * @param parameterReviser the parameter reviser
     */
    public StreamConfig ordinaryParameterReviser(
        BiFunction<Map<String, Object>, Object[], Integer> parameterReviser) {
      ordinaryParameterReviser = parameterReviser;
      return this;
    }

    /**
     * Set up a retry back-off strategy for query retrying, only the {@link #retryTimes} >0 this
     * retry back-off strategy can take effect.
     *
     * @param retryBackoffStrategy the retry retry back-off strategy, default is one second
     *
     * @see BackoffStrategy
     * @see SynchronousRetryer
     */
    public StreamConfig retryBackoffStategy(BackoffStrategy retryBackoffStrategy) {
      this.retryBackoffStrategy = defaultObject(retryBackoffStrategy,
          () -> new FixedBackoffStrategy(Duration.ofSeconds(2L)));
      return this;
    }

    /**
     * Set up a retry time for query retrying. If the given retry times <=0 means that don't retry
     * during the query exception occurred.
     *
     * @param retryTimes the retry times must be greater then 0
     *
     * @see Retry
     * @see Retryer
     */
    public StreamConfig retryTimes(int retryTimes) {
      this.retryTimes = max(retryTimes, 0);
      return this;
    }

    /**
     * Set some exception types, when a given type of exception occurs during the query retry
     * process, it is forced to interrupt and throw an exception, only the {@link #retryTimes} >0
     * this can take effect.
     *
     * @param throwables the exception types
     * @see Retry
     * @see Retryer#thrower(BiConsumer)
     */
    @SuppressWarnings("unchecked")
    public StreamConfig stopOn(Class<? extends Throwable>... throwables) {
      stopOn = linkedHashSetOf(throwables);
      return this;
    }

    public boolean terminateIf(Integer counter, Map<String, Object> current) {
      return terminater != null && !terminater.test(counter, current);
    }

    /**
     * Set the interruption condition of a streaming query. When the condition is met, the output
     * will be interrupted. The first parameter of the given {@code terminater} is the number of
     * result set records that have been output, and the second parameter is the last record of the
     * result set of the current batch query, user can use those parameters to decide whether to
     * interrupt the stream.
     *
     * @param terminater the terminate predicate
     */
    public StreamConfig terminater(BiPredicate<Integer, Object> terminater) {
      this.terminater = terminater;
      return this;
    }
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 下午7:01:30
   *
   */
  static class ForwardIterator implements Iterator<Map<String, Object>> {
    private final SqlQueryTemplate tpl;
    private final StreamConfig config;
    Forwarding<Map<String, Object>> buffer = null;
    int counter = 0;
    Map<String, Object> next = null;
    Retryer retryer;

    ForwardIterator(SqlQueryTemplate tpl, StreamConfig config) {
      this.tpl = tpl;
      this.config = config;
      retryer =
          config.retryTimes > 0
              ? Retry.synchronousRetryer()
                  .retryStrategy(new MaxAttemptsRetryStrategy(config.retryTimes + 1)
                      .and(new ThrowableClassifierRetryStrategy().abortOn(config.stopOn)))
                  .backoffStrategy(config.retryBackoffStrategy)
              : null;
    }

    @Override
    public boolean hasNext() {
      initialize();
      if (!config.terminateIf(counter, next)) {
        if (!buffer.hasResults()) {
          if (buffer.hasNext()) {
            revise(next, config);
            buffer.with(fetch());
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

    private Forwarding<Map<String, Object>> fetch() {
      if (retryer != null) {
        return retryer.invoke(tpl::forward);
      } else {
        return tpl.forward();
      }
    }

    private void initialize() {
      if (buffer == null) {
        buffer = defaultObject(fetch(), Forwarding::inst);
        counter = buffer.hasResults() ? 1 : 0;
      }
    }

    private void revise(Map<String, Object> next, StreamConfig config) {
      boolean revise = false;
      if (tpl.useNamedParameter) {
        if (config.namedParameterReviser != null) {
          tpl.offset = config.namedParameterReviser.apply(next, tpl.namedParameters);
          revise = true;
        }
      } else if (config.ordinaryParameterReviser != null) {
        tpl.offset = config.ordinaryParameterReviser.apply(next, tpl.ordinaryParameters);
        revise = true;
      }
      if (!revise) {
        tpl.offset += tpl.limit;
      }
    }
  }
}
