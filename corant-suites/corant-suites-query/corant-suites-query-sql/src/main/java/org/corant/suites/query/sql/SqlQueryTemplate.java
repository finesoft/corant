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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午5:38:09
 *
 */
public class SqlQueryTemplate {

  final String database;
  final DBMS dbms;
  String sql;
  Object[] parameters = new Object[0];
  int limit = -1;
  int offset = 0;

  private SqlQueryTemplate(String database, DBMS dbms) {
    this.database = shouldNotBlank(database);
    this.dbms = defaultObject(dbms, () -> DBMS.MYSQL);
  }

  public static SqlQueryTemplate database(DBMS dbms, String database) {
    return new SqlQueryTemplate(database, dbms);
  }

  public Forwarding<Map<?, ?>> forward() {
    return null;
  }

  public <T> Forwarding<T> forward(Class<T> clazz) {
    return null;
  }

  public Map<?, ?> get() {
    return null;
  }

  public SqlQueryTemplate limit(int limit) {
    this.limit = ObjectUtils.max(limit, 1);
    return this;
  }

  public SqlQueryTemplate offset(int offset) {
    this.offset = ObjectUtils.max(offset, 0);
    return this;
  }

  public Paging<Map<?, ?>> page() {
    return null;
  }

  public <T> Paging<T> page(Class<T> clazz) {
    return null;
  }

  public SqlQueryTemplate parameters(Object... parameters) {
    this.parameters = new Object[parameters.length];
    System.arraycopy(parameters, 0, this.parameters, 0, parameters.length);
    return this;
  }

  public List<Map<?, ?>> select() {
    return null;
  }

  public <T> List<T> select(Class<T> clazz) {
    return null;
  }

  public SqlQueryTemplate sql(String sql) {
    this.sql = shouldNotBlank(sql);
    return this;
  }

  public Stream<Map<?, ?>> stream() {
    return null;
  }

  public <T> Stream<T> stream(Class<T> clazz) {
    return null;
  }
}
