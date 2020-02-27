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
package org.corant.suites.query.cassandra;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

/**
 * corant-suites-query-cassandra
 *
 * Unfinish yet!
 *
 * @author bingo 上午10:52:01
 *
 */
public class DefaultCasQueryExecutor implements CasQueryExecutor {

  final Function<String, Cluster> clusterFactory;

  /**
   * @param cluster
   */
  public DefaultCasQueryExecutor(Function<String, Cluster> clusterFactory) {
    super();
    this.clusterFactory = shouldNotNull(clusterFactory);
  }

  @Override
  public Map<String, Object> get(String keyspace, String cql, Object... args) {
    Map<String, Object> map = new LinkedHashMap<>();
    try (Cluster cluster = clusterFactory.apply(keyspace)) {
      final Session session = cluster.connect(keyspace);
      final Statement stm = prepare(session, cql, args);
      stm.setFetchSize(1);
      ResultSet rs = session.execute(stm);
      if (rs != null) {
        CasMapHandler.get(rs).forEach(x -> x.forEach((k, v) -> map.put(k.toString(), v)));
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
    return map;
  }

  @Override
  public List<Map<String, Object>> paging(String keyspace, String cql, int offset, int limit,
      Object... args) {
    if (offset == 0) {
      return select(keyspace, cql + " LIMIT " + limit, args);
    } else {

    }
    return null;
  }

  @Override
  public String resolveCountCql(String cql) {
    return null;
  }

  @Override
  public List<Map<String, Object>> select(String keyspace, String cql, Object... args) {
    List<Map<String, Object>> list = new ArrayList<>();
    try (Cluster cluster = clusterFactory.apply(keyspace)) {
      final Session session = cluster.connect(keyspace);
      final Statement stm = prepare(session, cql, args);
      stm.setFetchSize(1);
      ResultSet rs = session.execute(stm);
      if (rs != null) {
        CasMapHandler.get(rs).forEach(x -> {
          Map<String, Object> map = new LinkedHashMap<>();
          x.forEach((k, v) -> map.put(k.toString(), v));
          list.add(map);
        });
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

  protected Statement prepare(Session session, String cql, Object... args) {
    if (isEmpty(args)) {
      return session.prepare(cql).bind();
    }
    return session.prepare(cql).bind(args);
  }

}
