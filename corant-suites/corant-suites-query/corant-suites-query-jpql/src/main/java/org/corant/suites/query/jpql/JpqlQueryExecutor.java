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
package org.corant.suites.query.jpql;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * corant-suites-query-jpql
 *
 * @author bingo 下午12:09:13
 *
 */
public interface JpqlQueryExecutor {

  <T> T get(String jpql, Map<String, Object> args) throws SQLException;

  <T> T get(String jpql, Object... args) throws SQLException;

  <T> List<T> select(String jpql, Map<String, Object> args) throws SQLException;

  <T> List<T> select(String jpql, Object... args) throws SQLException;

  <T> Stream<T> stream(String jpql, Map<String, Object> args);

  <T> Stream<T> stream(String jpql, Object... args);

}
