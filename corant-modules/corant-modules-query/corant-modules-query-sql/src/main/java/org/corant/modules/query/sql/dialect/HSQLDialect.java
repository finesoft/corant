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
import org.corant.modules.query.shared.dynamic.SqlHelper;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午11:51:02
 *
 */
public class HSQLDialect implements Dialect {

  public static final Dialect INSTANCE = new HSQLDialect();

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    return getLimitString(sql, offset, limit, hints);
  }

  /**
   * <pre>
   * dialect.getLimitString("select * from user",":offset",":limit") will return
   * select limit :offset :limit * from user
   * </pre>
   */
  public String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    boolean hasOffset = offset > 0;
    return new StringBuffer(sql.length() + 10).append(sql)
        .insert(SqlHelper.shallowIndexOfPattern(sql, SqlHelper.SELECT_PATTERN, 0) + 7,
            hasOffset ? " LIMIT " + offset + " " + limit : " TOP " + limit)
        .toString();
  }

  @Override
  public String getNonOrderByPart(String sql) {
    if (sql != null) {
      int pos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.LIMIT_PATTERN, pos) < 0
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.FETCH_PATTERN, pos) < 0
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.OFFSET_PATTERN, pos) < 0) {
        return sql.substring(0, pos);
      }
    }
    return sql;
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

}
