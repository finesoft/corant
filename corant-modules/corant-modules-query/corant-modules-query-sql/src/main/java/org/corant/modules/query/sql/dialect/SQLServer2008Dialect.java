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
 * @author bingo 上午11:03:33
 *
 */
public class SQLServer2008Dialect extends SQLServer2005Dialect {

  public static final Dialect INSTANCE = new SQLServer2008Dialect();

  @Override
  public boolean supportsLimit() {
    return true;
  }

  @Override
  protected String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    int pos = getInsertPosition(sql);
    StringBuilder tsql = new StringBuilder(sql.length() + 50).append(sql, 0, pos);
    tsql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit)
        .append(" ROWS ONLY");
    if (pos > sql.length()) {
      tsql.append(sql.substring(pos - 1));
    }
    return tsql.toString();
  }

}
