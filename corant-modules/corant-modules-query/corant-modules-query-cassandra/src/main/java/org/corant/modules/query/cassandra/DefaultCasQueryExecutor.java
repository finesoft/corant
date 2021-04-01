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
package org.corant.modules.query.cassandra;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.max;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

/**
 * corant-modules-query-cassandra
 *
 * Unfinish yet!
 *
 * @author bingo 上午10:52:01
 *
 */
public class DefaultCasQueryExecutor implements CasQueryExecutor {

  final Cluster cluster;

  final int fetchSize;

  /**
   * @param cluster
   */
  public DefaultCasQueryExecutor(Cluster cluster, int fetchSize) {
    this.cluster = shouldNotNull(cluster);
    this.fetchSize = max(fetchSize, 1);
  }

  @Override
  public Map<String, Object> get(String keyspace, String cql, Object... args) {
    Map<String, Object> map = new LinkedHashMap<>();
    try (Session session = cluster.connect(keyspace)) {
      final Statement stm = prepare(session, cql, args).setFetchSize(1);
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
    if (offset <= 0) {
      int af = cql.toUpperCase(Locale.ROOT).lastIndexOf("ALLOW FILTERING");
      if (af != -1) {
        return select(keyspace, cql.substring(0, af) + " LIMIT " + limit + " ALLOW FILTERING",
            args);
      }
      return select(keyspace, cql + " LIMIT " + limit, args);
    } else {
      List<Map<String, Object>> list = new ArrayList<>();
      try (Session session = cluster.connect(keyspace)) {
        final Statement stm = prepare(session, cql, args).setFetchSize(limit);
        ResultSet rs = session.execute(stm);
        if (rs != null) {
          int currentRow = 0;
          int fetchedSize = rs.getAvailableWithoutFetching();
          byte[] nextState = rs.getExecutionInfo().getPagingStateUnsafe();
          while (fetchedSize > 0 && nextState != null && currentRow + fetchedSize < offset) {
            rs = session.execute(stm.setPagingStateUnsafe(nextState));
            currentRow += fetchedSize;
            fetchedSize = rs.getAvailableWithoutFetching();
            nextState = rs.getExecutionInfo().getPagingStateUnsafe();
          }
          if (currentRow < offset) {
            for (@SuppressWarnings("unused")
            Row row : rs) {
              if (++currentRow == offset) {
                break;
              }
            }
          }
          int remaining = limit;
          for (Row row : rs) {
            Map<String, Object> map = new LinkedHashMap<>();
            CasMapHandler.get(row).forEach((k, v) -> map.put(k.toString(), v));
            list.add(map);
            if (--remaining == 0) {
              break;
            }
          }
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
      return list;
    }
  }

  @Override
  public List<Map<String, Object>> select(String keyspace, String cql, Object... args) {
    List<Map<String, Object>> list = new ArrayList<>();
    try (Session session = cluster.connect(keyspace)) {
      final Statement stm = prepare(session, cql, args).setFetchSize(fetchSize);
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
