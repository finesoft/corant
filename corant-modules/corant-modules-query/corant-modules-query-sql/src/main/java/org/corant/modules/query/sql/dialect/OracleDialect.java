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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.corant.modules.query.shared.dynamic.SqlHelper;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午11:45:24
 *
 */
public class OracleDialect implements Dialect {

  public static final Dialect INSTANCE = new OracleDialect();

  public static final String ORDER_SIBLINGS_BY = "order\\s+siblings\\s+by";
  public static final Pattern ORDER_SIBLINGS_BY_PATTERN =
      SqlHelper.buildShallowIndexPattern(ORDER_SIBLINGS_BY, true);

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    return getLimitString(sql, offset, limit, hints);
  }

  public String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    String sqlToUse = sql.trim();
    boolean isForUpdate = false;
    if (sqlToUse.toUpperCase(Locale.getDefault()).endsWith(" FOR UPDATE")) {
      sqlToUse = sqlToUse.substring(0, sqlToUse.length() - 11);
      isForUpdate = true;
    }
    StringBuilder pagingSelect = new StringBuilder(sqlToUse.length() + 100);
    if (offset >= 0) {
      pagingSelect.append("SELECT * FROM ( SELECT row_.*, ROWNUM rownum_ FROM ( ");
    } else {
      pagingSelect.append("SELECT * FROM ( ");
    }
    pagingSelect.append(sqlToUse);

    if (offset >= 0) {
      pagingSelect.append(" ) row_ ) WHERE rownum_ <= ").append(offset + limit)
          .append(" AND rownum_ > ").append(offset);
    } else {
      pagingSelect.append(" ) WHERE ROWNUM <= ").append(limit);
    }

    if (isForUpdate) {
      pagingSelect.append(" FOR UPDATE");
    }
    return pagingSelect.toString();
  }

  @Override
  public String getNonOrderByPart(String sql) {
    if (sql != null) {
      int pos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1) {
        return sql.substring(0, pos);
      }
      pos = SqlHelper.shallowIndexOfPattern(sql, ORDER_SIBLINGS_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1) {
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
