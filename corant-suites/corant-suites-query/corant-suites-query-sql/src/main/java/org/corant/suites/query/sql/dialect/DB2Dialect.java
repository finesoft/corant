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
package org.corant.suites.query.sql.dialect;

import org.corant.suites.query.shared.dynamic.SqlHelper;

/**
 * corant-suites-query
 *
 * @author bingo 上午11:02:47
 *
 */
public class DB2Dialect implements Dialect {

  public static final Dialect INSTANCE = new DB2Dialect();

  @Override
  public String getLimitSql(String sql, int offset, int limit) {
    return getLimitString(sql, offset, limit);
  }

  /**
   * <pre>
   * dialect.getLimitString("select * from user", 12, ":offset",0,":limit") will return
   * select * from user limit :offset,:limit
   * </pre>
   */
  public String getLimitString(String sql, int offset, int limit) {
    int startOfSelect = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.SELECT_PATTERN, 0);
    StringBuilder sqlToUse =
        new StringBuilder(sql.length() + 128).append(sql.substring(0, startOfSelect))
            .append("SELECT * FROM ( SELECT ").append(getRowNumber(sql));
    if (SqlHelper.containDistinct(sql)) {
      sqlToUse.append(" row_.* FROM ( ").append(sql.substring(startOfSelect)).append(" ) AS row_");
    } else {
      sqlToUse.append(sql.substring(startOfSelect + 6));
    }
    sqlToUse.append(" ) AS temp_ WHERE rownumber_ ");
    if (offset > 0) {
      sqlToUse.append("BETWEEN ").append(offset).append(" AND ").append(offset + limit - 1);
    } else {
      sqlToUse.append("< ").append(limit);
    }
    return sqlToUse.toString();
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

  private String getRowNumber(String sql) {
    StringBuilder rownumber = new StringBuilder(50).append("ROWNUMBER() OVER(");
    String orderBy = SqlHelper.getOrderBy(sql);
    if (orderBy != null) {
      rownumber.append(orderBy);
    }
    rownumber.append(") as rownumber_,");
    return rownumber.toString();
  }

}
