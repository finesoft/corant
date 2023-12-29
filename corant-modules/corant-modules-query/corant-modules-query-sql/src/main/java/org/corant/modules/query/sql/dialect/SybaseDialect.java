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
package org.corant.modules.query.sql.dialect;

import java.util.Map;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午11:47:15
 */
public class SybaseDialect implements Dialect {

  public static final Dialect INSTANCE = new SybaseDialect();

  @Override
  public String getDefaultCountSql(String sql, Map<String, ?> hints) {
    return new StringBuilder(sql.length() + 64).append("SELECT COUNT(1) FROM ( ")
        .append(getNonOrderByPart(sql)).append(" ) AS tmp_count_").toString();
  }

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    throw new UnsupportedOperationException("The database Sybase limit script not supported");
  }

  public String getLimitString(String sql, int offset, String offsetPlaceholder, int limit,
      String limitPlaceholder, Map<String, ?> hints) {
    throw new UnsupportedOperationException("The database Sybase limit script not supported");
  }

  @Override
  public boolean supportsLimit() {
    return false;
  }
}
