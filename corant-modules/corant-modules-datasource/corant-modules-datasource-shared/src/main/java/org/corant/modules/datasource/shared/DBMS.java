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
package org.corant.modules.datasource.shared;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午11:28:20
 */
public enum DBMS {

  MYSQL() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(MYSQL_CLASS_PKG_KEY);
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
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(ORACLE_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(ORACLE_JDBC_URL_PREFIX);
    }

  },

  DB2() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(DB2_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(DB2_JDBC_URL_PREFIX);
    }

  },

  H2() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(H2_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(H2_JDBC_URL_PREFIX);
    }

  },

  HSQL() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(HSQL_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(HSQL_JDBC_URL_PREFIX);
    }

  },

  POSTGRE() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(POSTGRESQL_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(POSTGRESQL_JDBC_URL_PREFIX);
    }

  },

  SQLSERVER2005() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(SQLSERVER_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
    }

  },

  SQLSERVER2008() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(SQLSERVER_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
    }

  },

  SQLSERVER2012() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(SQLSERVER_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(SQLSERVER_JDBC_URL_PREFIX);
    }

  },

  SYBASE() {

    @Override
    boolean matchPackageName(Class<?> clazz) {
      return clazz != null && clazz.getPackageName().contains(SYBASE_CLASS_PKG_KEY);
    }

    @Override
    boolean matchURL(String URL) {
      return URL.startsWith(SYBASE_JDBC_URL_PREFIX);
    }

  };

  public static final String H2_JDBC_URL_PREFIX = "jdbc:h2";
  public static final String HSQL_JDBC_URL_PREFIX = "jdbc:hsqldb";
  public static final String DB2_JDBC_URL_PREFIX = "jdbc:db2:";
  public static final String MARIADB_JDBC_DRIVER = "org.mariadb.jdbc.Driver";
  public static final String MARIADB_JDBC_URL_PREFIX = "jdbc:mariadb:";
  public static final String MYSQL_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
  public static final String MYSQL_GOOGLE_JDBC_DRIVER = "com.mysql.jdbc.GoogleDriver";
  public static final String MYSQL_LEGACY_JDBC_DRIVER = "com.mysql.jdbc.Driver";
  public static final String MYSQL_JDBC_URL_PREFIX = "jdbc:mysql:";
  public static final String ORACLE_JDBC_URL_PREFIX = "jdbc:oracle:";
  public static final String POSTGRESQL_JDBC_URL_PREFIX = "jdbc:postgresql:";
  public static final String SQLSERVER_JDBC_URL_PREFIX = "jdbc:sqlserver:";
  public static final String SYBASE_JDBC_URL_PREFIX = "jdbc:sybase:";

  public static final String H2_CLASS_PKG_KEY = ".h2.";
  public static final String HSQL_CLASS_PKG_KEY = ".hsqldb.";
  public static final String DB2_CLASS_PKG_KEY = ".db2.";
  public static final String MARIADB_CLASS_PKG_KEY = ".mariadb.";
  public static final String MYSQL_CLASS_PKG_KEY = ".mysql.";
  public static final String ORACLE_CLASS_PKG_KEY = ".oracle.";
  public static final String POSTGRESQL_CLASS_PKG_KEY = ".postgresql.";
  public static final String SQLSERVER_CLASS_PKG_KEY = ".sqlserver.";
  public static final String SYBASE_CLASS_PKG_KEY = ".sybase.";

  public static DBMS fromClass(Class<?> klass) {
    if (klass != null)
      for (DBMS d : DBMS.values()) {
        if (d.matchPackageName(klass)) {
          return d;
        }
      }
    return null;
  }

  public static DBMS fromUrl(String url) {
    for (DBMS d : DBMS.values()) {
      if (d.matchURL(url)) {
        return d;
      }
    }
    return null;
  }

  abstract boolean matchPackageName(Class<?> clazz);

  abstract boolean matchURL(String URL);

}
