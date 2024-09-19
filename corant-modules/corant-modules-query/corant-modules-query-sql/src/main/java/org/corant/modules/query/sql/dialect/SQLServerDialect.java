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

import static org.corant.shared.util.Sets.immutableSetOf;
import java.util.Collection;
import java.util.Set;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午7:09:20
 */
public abstract class SQLServerDialect implements Dialect {

  public static final String SQL_DFLT_ORDERBY = "ORDER BY CURRENT_TIMESTAMP";

  // SQLServer 2017 above
  public static final Set<String> AGGREGATE_FUNCTIONS = immutableSetOf("APPROX_COUNT_DISTINCT",
      "AVG", "CHECKSUM_AGG", "COUNT", "COUNT_BIG", "GROUPING", "GROUPING_ID", "MAX", "MIN", "STDEV",
      "STDEVP", "STRING_AGG", "SUM", "VAR", "VARP");

  @Override
  public Collection<String> getAggregationFunctionNames() {
    return AGGREGATE_FUNCTIONS;
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

}
