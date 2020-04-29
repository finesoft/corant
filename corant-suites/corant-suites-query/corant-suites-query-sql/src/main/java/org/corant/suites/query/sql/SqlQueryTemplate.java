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
package org.corant.suites.query.sql;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapInteger;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.suites.cdi.Instances.findNamed;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.shared.QueryObjectMapper;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import org.corant.suites.query.sql.dialect.Dialect;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;

/**
 * corant-suites-query-sql
 *
 * <p>
 * NOTE: unfinish yet!
 *
 * @author bingo 下午5:38:09
 *
 */
public class SqlQueryTemplate {

  public static final String SQL_PARAM_PH = "?";
  public static final char SQL_PARAM_PH_C = SQL_PARAM_PH.charAt(0);
  public static final char SQL_PARAM_ESC_C = '\'';
  public static final String SQL_PARAM_SP = ",";

  protected static final MapHandler mapHandler = new MapHandler();
  protected static final MapListHandler mapListHander = new MapListHandler();

  protected final QueryRunner runner;
  protected String sql;
  protected Object[] parameters = new Object[0];
  protected int limit = -1;
  protected int offset = 0;
  protected final Dialect dialect;
  protected final DataSource datasource;

  private SqlQueryTemplate(String database, DBMS dbms) {
    datasource = findNamed(DataSource.class, database).orElseThrow(QueryRuntimeException::new);
    dialect = defaultObject(dbms, () -> DBMS.MYSQL).instance();
    runner = new QueryRunner(datasource);
  }

  public static SqlQueryTemplate database(DBMS dbms, String database) {
    return new SqlQueryTemplate(database, dbms);
  }

  public static SqlQueryTemplate mysql(String database) {
    return new SqlQueryTemplate(database, DBMS.MYSQL);
  }

  public static SqlQueryTemplate sql2012(String database) {
    return new SqlQueryTemplate(database, DBMS.SQLSERVER2012);
  }

  public Forwarding<Map<String, Object>> forward() {
    Pair<String, Object[]> ps = processSqlAndParams(sql, parameters);
    String limitSql = dialect.getLimitSql(sql, offset, limit + 1);
    final Object[] params = ps.getRight();
    Forwarding<Map<String, Object>> result = Forwarding.inst();
    List<Map<String, Object>> list = query(limitSql, params);
    int size = list == null ? 0 : list.size();
    if (size > 0 && size > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list);
  }

  public <T> Forwarding<T> forward(Class<T> clazz) {
    Forwarding<Map<String, Object>> forwarding = forward();
    Forwarding<T> result = Forwarding.inst();
    return result.withHasNext(forwarding.hasNext()).withResults(forwarding.getResults().stream()
        .map(r -> QueryObjectMapper.OM.convertValue(r, clazz)).collect(Collectors.toList()));
  }

  public Map<?, ?> get() {
    Pair<String, Object[]> ps = processSqlAndParams(sql, parameters);
    return get(ps.getLeft(), ps.getRight());
  }

  public SqlQueryTemplate limit(int limit) {
    this.limit = ObjectUtils.max(limit, 1);
    return this;
  }

  public SqlQueryTemplate offset(int offset) {
    this.offset = ObjectUtils.max(offset, 0);
    return this;
  }

  public Paging<Map<String, Object>> page() {
    Pair<String, Object[]> ps = processSqlAndParams(sql, parameters);
    String useSql = ps.getLeft();
    Object[] params = ps.getRight();
    String limitSql = dialect.getLimitSql(useSql, offset, limit);
    try {
      List<Map<String, Object>> list = query(limitSql, params);
      Paging<Map<String, Object>> result = Paging.of(offset, limit);
      int size = list == null ? 0 : list.size();
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

  public <T> Paging<T> page(Class<T> clazz) {
    Paging<Map<String, Object>> paging = page();
    Paging<T> result = Paging.of(paging.getOffset(), limit);
    result.withTotal(paging.getTotal());
    result.withResults(paging.getResults().stream()
        .map(r -> QueryObjectMapper.OM.convertValue(r, clazz)).collect(Collectors.toList()));
    return result;
  }

  public SqlQueryTemplate parameters(Object... parameters) {
    this.parameters = new Object[parameters.length];
    System.arraycopy(parameters, 0, this.parameters, 0, parameters.length);
    return this;
  }

  public List<Map<String, Object>> select() {
    Pair<String, Object[]> ps = processSqlAndParams(sql, parameters);
    try {
      return runner.query(ps.getLeft(), mapListHander, ps.getRight());
    } catch (SQLException e) {
      throw new QueryRuntimeException(e);
    }
  }

  public <T> List<T> select(Class<T> clazz) {
    return select().stream().map(r -> QueryObjectMapper.OM.convertValue(r, clazz))
        .collect(Collectors.toList());
  }

  public SqlQueryTemplate sql(String sql) {
    this.sql = shouldNotBlank(sql);
    return this;
  }

  public Stream<Map<String, Object>> stream() {
    return streamOf(new Iterator<Map<String, Object>>() {

      final Forwarding<Map<String, Object>> buffer = forward();

      @Override
      public boolean hasNext() {
        if (!buffer.hasResults()) {
          if (buffer.hasNext()) {
            offset += limit;
            buffer.with(forward());
            return buffer.hasResults();
          }
        } else {
          return true;
        }
        return false;
      }

      @Override
      public Map<String, Object> next() {
        if (!buffer.hasResults()) {
          throw new NoSuchElementException();
        }
        return buffer.getResults().remove(0);
      }
    });
  }

  public <T> Stream<T> stream(Class<T> clazz) {
    return stream().map(r -> QueryObjectMapper.OM.convertValue(r, clazz));
  }

  protected Map<String, Object> get(String sql, Object... parameter) {
    try {
      return defaultObject(runner.query(sql, mapHandler, parameter), HashMap::new);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e);
    }
  }

  /**
   * FIXME: Need another implementation
   *
   * @param sql
   * @param params
   * @return processSqlAndParams
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected Pair<String, Object[]> processSqlAndParams(String sql, Object... params) {
    if (isEmpty(params) || isBlank(sql)
        || streamOf(params).noneMatch(p -> p instanceof Collection || p.getClass().isArray())) {
      return Pair.of(sql, params);
    }
    int sqlLen = sql.length();
    int escs = 0;
    List<Integer> poses = new ArrayList<>(params.length);
    for (int i = 0; i < sqlLen; i++) {
      char c = sql.charAt(i);
      if (c == SQL_PARAM_ESC_C) {
        escs++;
      } else if (c == SQL_PARAM_PH_C && escs % 2 == 0) {
        poses.add(i);
      }
    }
    shouldBeTrue(poses.size() == params.length, "Parameters and sql statements not match.", sql);
    StringBuilder useSql = new StringBuilder();
    List<Object> useParams = new ArrayList<>();
    int paramPos = 0;
    int paramIdx = 0;
    for (Integer pos : poses) {
      useSql.append(sql.substring(paramPos, pos));
      paramPos = pos + 1;
      Object param = params[paramIdx++];
      List<Object> collectionParams = new ArrayList<>();
      List<String> placeHolders = new ArrayList<>();
      if (param instanceof Collection) {
        collectionParams.addAll((Collection) param);
      } else if (param != null && param.getClass().isArray()) {
        collectionParams.addAll(listOf((Object[]) param));
      } else {
        collectionParams.add(param);
      }
      for (Object cp : collectionParams) {
        useParams.add(cp);
        placeHolders.add(SQL_PARAM_PH);
      }
      useSql.append(String.join(SQL_PARAM_SP, placeHolders));
    }
    if (paramPos < sqlLen) {
      useSql.append(sql.substring(paramPos));
    }
    return Pair.of(useSql.toString(), useParams.toArray());
  }

  protected List<Map<String, Object>> query(String sql, Object... parameter) {
    try {
      return defaultObject(runner.query(sql, mapListHander, parameter), ArrayList::new);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e);
    }
  }
}
