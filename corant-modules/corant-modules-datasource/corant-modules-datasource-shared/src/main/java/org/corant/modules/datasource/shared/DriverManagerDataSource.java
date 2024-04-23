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

import static java.lang.String.format;
import static org.corant.shared.util.Configurations.getAssembledConfigValue;
import static org.corant.shared.util.Strings.strip;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Objects;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午7:45:22
 */
public class DriverManagerDataSource implements DataSource {

  private static final Logger LOGGER = Logger.getLogger(DriverManagerDataSource.class.getName());
  protected static final String PASSWORD = "password";
  protected static final String USER = "user";
  protected static final String USER_NAME = USER + "name";

  protected final String jdbcUrl;
  protected final Properties properties;
  protected final String catalog;
  protected final String schema;
  protected Driver driver;

  public DriverManagerDataSource(String jdbcUrl) {
    this(jdbcUrl, null, null, null, null, null, null);
  }

  public DriverManagerDataSource(String jdbcUrl, Properties properties) {
    this(jdbcUrl, null, properties, null, null, null, null);
  }

  public DriverManagerDataSource(String jdbcUrl, String driverClassName, Properties properties,
      String username, String password) {
    this(jdbcUrl, driverClassName, properties, username, password, null, null);
  }

  public DriverManagerDataSource(String jdbcUrl, String driverClassName, Properties properties,
      String username, String password, String catalog, String schema) {
    this.jdbcUrl = getAssembledConfigValue(strip(jdbcUrl));
    this.catalog = getAssembledConfigValue(catalog);
    this.schema = getAssembledConfigValue(schema);
    this.properties = new Properties();
    if (properties != null) {
      properties.forEach((k, v) -> this.properties.put(k.toString(), v.toString()));
    }
    if (username != null) {
      this.properties.put(USER, this.properties.getProperty(USER, username));
    }
    if (password != null) {
      this.properties.put(PASSWORD, this.properties.getProperty(PASSWORD, password));
    }

    if (driverClassName != null) {
      Enumeration<Driver> drivers = DriverManager.getDrivers();
      while (drivers.hasMoreElements()) {
        Driver d = drivers.nextElement();
        if (d.getClass().getName().equals(driverClassName)) {
          driver = d;
          break;
        }
      }
      if (driver == null) {
        LOGGER.warning(() -> format(
            "Registered driver with driverClassName=%s was not found, trying direct instantiation.",
            driverClassName));
        Class<?> driverClass = null;
        ClassLoader threadContextClassLoader = Classes.defaultClassLoader();
        try {
          if (threadContextClassLoader != null) {
            try {
              driverClass = threadContextClassLoader.loadClass(driverClassName);
              LOGGER.fine(() -> format("Driver class %s found in Thread context class loader %s.",
                  driverClassName, threadContextClassLoader));
            } catch (ClassNotFoundException e) {
              LOGGER.fine(() -> format(
                  "Driver class %s not found in Thread context class loader %s, trying classloader %s.",
                  driverClassName, threadContextClassLoader, this.getClass().getClassLoader()));
            }
          }

          if (driverClass == null) {
            driverClass = this.getClass().getClassLoader().loadClass(driverClassName);
            LOGGER.fine(() -> format(
                "Driver class %s found in the DriverManagerDataSource class classloader %s.",
                driverClassName, this.getClass().getClassLoader()));
          }
        } catch (ClassNotFoundException e) {
          LOGGER.fine(() -> format(
              "Failed to load driver class %s from DriverManagerDataSource class classloader %s.",
              driverClassName, this.getClass().getClassLoader()));
        }

        if (driverClass != null) {
          try {
            driver = (Driver) Objects.newInstance(driverClass);
          } catch (Exception e) {
            LOGGER.log(Level.WARNING, e,
                () -> format(
                    "Failed to create instance of driver class %s, trying jdbcUrl resolution",
                    driverClassName));
          }
        }
      }
    }

    final String sanitizedUrl = jdbcUrl.replaceAll("([?&;]password=)[^&#;]*(.*)", "$1<masked>$2");
    try {
      if (driver == null) {
        driver = DriverManager.getDriver(jdbcUrl);
        LOGGER.fine(() -> format("Loaded driver with class name %s for jdbcUrl=%s.",
            driver.getClass().getName(), sanitizedUrl));
      } else if (!driver.acceptsURL(jdbcUrl)) {
        throw new CorantRuntimeException("Driver %s claims to not accept jdbcUrl, %s.",
            driverClassName, sanitizedUrl);
      }
    } catch (SQLException e) {
      throw new CorantRuntimeException(e, "Failed to get driver instance for jdbcUrl=%s.",
          sanitizedUrl);
    }
  }

  public DriverManagerDataSource(String jdbcUrl, String username, String password) {
    this(jdbcUrl, null, null, username, password, null, null);
  }

  public DriverManagerDataSource(String jdbcUrl, String driverClassName, String username,
      String password) {
    this(jdbcUrl, driverClassName, null, username, password, null, null);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(driver.connect(jdbcUrl, properties));
  }

  @Override
  public Connection getConnection(final String username, final String password)
      throws SQLException {
    final Properties cloned = (Properties) properties.clone();
    if (username != null) {
      cloned.put(USER, username);
      if (cloned.containsKey(USER_NAME)) {
        cloned.put(USER_NAME, username);
      }
    }
    if (password != null) {
      cloned.put(PASSWORD, password);
    }

    return getConnection(driver.connect(jdbcUrl, cloned));
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();// FIXME
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return driver.getParentLogger();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    DriverManager.setLoginTimeout(seconds);// FIXME
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  protected Connection getConnection(Connection connection) throws SQLException {
    if (catalog != null) {
      connection.setCatalog(catalog);
    }
    if (schema != null) {
      connection.setSchema(schema);
    }
    return connection;
  }
}
