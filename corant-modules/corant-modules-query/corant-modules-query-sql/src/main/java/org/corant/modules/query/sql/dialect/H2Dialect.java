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
 * @author bingo 上午11:43:17
 */
public class H2Dialect implements Dialect {

  public static final Dialect INSTANCE = new H2Dialect();

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    return getLimitString(sql, offset, Integer.toString(offset), Integer.toString(limit), hints);
  }

  @Override
  public String getNonOrderByPart(String sql) {
    if (sql != null) {
      int pos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.LIMIT_PATTERN, pos) < 0
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

  /**
   * <pre>
   * dialect.getLimitString("select * from user", 12, ":offset",0,":limit") will return
   * select * from user limit :offset,:limit
   * </pre>
   */
  protected String getLimitString(String sql, int offset, String offsetPlaceholder,
      String limitPlaceholder, Map<String, ?> hints) {
    if (offset > 0) {
      return new StringBuilder(sql.length() + 32).append(sql).append(" LIMIT ")
          .append(limitPlaceholder).append(" OFFSET ").append(offsetPlaceholder).toString();
    } else {
      return new StringBuilder(sql.length() + 16).append(sql).append(" LIMIT ")
          .append(limitPlaceholder).toString();
    }
  }

}
