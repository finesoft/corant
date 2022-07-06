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

import static org.corant.shared.util.Lists.immutableListOf;
import static org.corant.shared.util.Maps.getMapBoolean;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.dynamic.SqlHelper;
import org.corant.modules.query.sql.SqlStatements;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午10:58:26
 *
 */
public interface Dialect {

  List<String> AGGREGATE_FUNCTIONS = immutableListOf("AVG", "COUNT", "MAX", "MIN", "SUM");

  String COUNT_FIELD_NAME = "total_";
  String COUNT_TEMP_TABLE_NAME = "tmp_count_";
  String USE_DEFAULT_COUNT_SQL_HINT_KEY = "_use_default_count_sql";

  String H2_JDBC_URL_PREFIX = "jdbc:h2";
  String HSQL_JDBC_URL_PREFIX = "jdbc:hsqldb";
  String DB2_JDBC_URL_PREFIX = "jdbc:db2:";
  String MARIADB_JDBC_DRIVER = "org.mariadb.jdbc.Driver";
  String MARIADB_JDBC_URL_PREFIX = "jdbc:mariadb:";
  String MYSQL_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
  String MYSQL_GOOGLE_JDBC_DRIVER = "com.mysql.jdbc.GoogleDriver";
  String MYSQL_LEGACY_JDBC_DRIVER = "com.mysql.jdbc.Driver";
  String MYSQL_JDBC_URL_PREFIX = "jdbc:mysql:";
  String ORACLE_JDBC_URL_PREFIX = "jdbc:oracle:";
  String POSTGRESQL_JDBC_URL_PREFIX = "jdbc:postgresql:";
  String SQLSERVER_JDBC_URL_PREFIX = "jdbc:sqlserver:";
  String SYBASE_JDBC_URL_PREFIX = "jdbc:sybase:";

  default Collection<String> getAggregationFunctionNames() {
    return AGGREGATE_FUNCTIONS;
  }

  /**
   * Convert SQL statement to Count SQL statement, optimized statements can be reorganized by
   * CCJSqlParserUtil parsing.
   *
   * @param sql to convert SQL
   * @param hints the hints use to improve the execution process
   * @return Count SQL statement
   */
  default String getCountSql(String sql, Map<String, ?> hints) {
    if (!getMapBoolean(hints, USE_DEFAULT_COUNT_SQL_HINT_KEY, false)) {
      try {
        return SqlStatements.resolveCountSql(sql, "1", COUNT_FIELD_NAME, COUNT_TEMP_TABLE_NAME,
            getAggregationFunctionNames());
      } catch (Exception ex) {
        return getDefaultCountSql(sql, hints);
      }
    } else {
      return getDefaultCountSql(sql, hints);
    }
  }

  /**
   * Convert SQL statement to Count SQL statement
   *
   * @param sql to convert SQL
   * @param hints the hints use to improve the execution process
   * @return Count SQL statement
   */
  default String getDefaultCountSql(String sql, Map<String, ?> hints) {
    return new StringBuilder(sql.length() + 64).append("SELECT COUNT(1) ").append(COUNT_FIELD_NAME)
        .append(" FROM ( ").append(getNonOrderByPart(sql)).append(" ) ")
        .append(COUNT_TEMP_TABLE_NAME).toString();
  }

  /**
   * Convert SQL statement to Paging SQL
   *
   * @param sql to convert SQL
   * @param offset begin offset
   * @param limit the fetched size
   * @param hints the hints use to improve the execution process
   * @return Paging SQL statement
   */
  String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints);

  /**
   * Convert SQL statement to Limit SQL default offset is 0
   *
   * @param sql to convert SQL
   * @param limit the fetched size
   * @param hints the hints use to improve the execution process
   * @return getLimitSql
   */
  default String getLimitSql(String sql, int limit, Map<String, ?> hints) {
    return getLimitSql(sql, 0, limit, hints);
  }

  default String getNonOrderByPart(String sql) {
    return SqlHelper.removeOrderBy(sql);
  }

  /**
   * Return whether the underling data base supports limitation sql.
   *
   * @return supportsLimit
   */
  boolean supportsLimit();

  /**
   * corant-modules-query-sql
   *
   * @author bingo 上午11:25:32
   *
   */
  enum DBMS {

    MYSQL() {
      @Override
      public Dialect instance() {
        return MySQLDialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(MYSQL_JDBC_URL_PREFIX) || URL.startsWith(MYSQL_GOOGLE_JDBC_DRIVER)
            || URL.startsWith(MYSQL_JDBC_DRIVER) || URL.startsWith(MYSQL_LEGACY_JDBC_DRIVER)
            || URL.startsWith(MARIADB_JDBC_DRIVER) || URL.startsWith(MARIADB_JDBC_URL_PREFIX);
      }
    },
    ORACLE() {
      @Override
      public Dialect instance() {
        return OracleDialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(ORACLE_JDBC_URL_PREFIX);
      }
    },
    DB2() {

      @Override
      public Dialect instance() {
        return DB2Dialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(DB2_JDBC_URL_PREFIX);
      }
    },
    H2() {

      @Override
      public Dialect instance() {
        return H2Dialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(H2_JDBC_URL_PREFIX);
      }
    },
    HSQL() {

      @Override
      public Dialect instance() {
        return HSQLDialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(HSQL_JDBC_URL_PREFIX);
      }
    },
    POSTGRE() {

      @Override
      public Dialect instance() {
        return PostgreSQLDialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(POSTGRESQL_JDBC_URL_PREFIX);
      }
    },
    SQLSERVER2005() {

      @Override
      public Dialect instance() {
        return SQLServer2005Dialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
      }
    },
    SQLSERVER2008() {

      @Override
      public Dialect instance() {
        return SQLServer2008Dialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
      }
    },
    SQLSERVER2012() {

      @Override
      public Dialect instance() {
        return SQLServer2012Dialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
      }
    },
    SYBASE() {

      @Override
      public Dialect instance() {
        return SybaseDialect.INSTANCE;
      }

      @Override
      boolean matchURL(String URL) {
        return URL.startsWith(SYBASE_JDBC_URL_PREFIX);
      }
    };

    public static DBMS url(String url) {
      for (DBMS d : DBMS.values()) {
        if (d.matchURL(url)) {
          return d;
        }
      }
      return null;
    }

    public abstract Dialect instance();

    abstract boolean matchURL(String URL);
  }
}
