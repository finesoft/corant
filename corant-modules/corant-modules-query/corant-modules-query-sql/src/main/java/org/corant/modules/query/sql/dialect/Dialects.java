/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.corant.modules.datasource.shared.DBMS;
import org.corant.shared.util.Services;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午11:52:00
 */
public class Dialects {

  public static final boolean USE_CUSTOM_DIALECT =
      getConfigValue("corant.query.sql.use-custom-dialect", Boolean.TYPE, false);

  public static final Map<DBMS, Dialect> DEFAULT_DIALECTS;

  static {
    Map<DBMS, Dialect> temp = new EnumMap<>(DBMS.class);
    temp.put(DBMS.MYSQL, MySQLDialect.INSTANCE);
    temp.put(DBMS.DB2, DB2Dialect.INSTANCE);
    temp.put(DBMS.H2, H2Dialect.INSTANCE);
    temp.put(DBMS.HSQL, HSQLDialect.INSTANCE);
    temp.put(DBMS.ORACLE, OracleDialect.INSTANCE);
    temp.put(DBMS.POSTGRE, PostgreSQLDialect.INSTANCE);
    temp.put(DBMS.SQLSERVER2005, SQLServer2005Dialect.INSTANCE);
    temp.put(DBMS.SQLSERVER2008, SQLServer2008Dialect.INSTANCE);
    temp.put(DBMS.SQLSERVER2012, SQLServer2012Dialect.INSTANCE);
    temp.put(DBMS.SYBASE, SybaseDialect.INSTANCE);
    DEFAULT_DIALECTS = unmodifiableMap(temp);
  }

  public static Dialect resolve(DBMS dbms) {
    if (USE_CUSTOM_DIALECT) {
      Optional<DialectResolver> resolver = Services.findPreferentially(DialectResolver.class);
      if (resolver.isPresent()) {
        return defaultObject(resolver.get().resolve(dbms), () -> DEFAULT_DIALECTS.get(dbms));
      }
    }
    return DEFAULT_DIALECTS.get(dbms);
  }
}
