/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.datasource.agroal;

import static java.lang.String.format;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.agroal.api.AgroalDataSourceListener;

/**
 * corant-modules-datasource-agroal
 *
 * @author bingo 16:13:08
 */
public class AgroalCPDataSourceLogger implements AgroalDataSourceListener {

  protected Logger logger = Logger.getLogger(AgroalCPDataSourceLogger.class.getName());

  @Override
  public void beforeConnectionAcquire() {
    logger.finer(() -> "Acquire a connection");
  }

  @Override
  public void beforeConnectionCreation() {
    logger.finer(() -> "Create a connection");
  }

  @Override
  public void beforeConnectionDestroy(Connection connection) {
    logger.finer(() -> format("Destroy a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void beforeConnectionFlush(Connection connection) {
    logger.finer(() -> format("Flush a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void beforeConnectionLeak(Connection connection) {
    logger.finer(() -> format("Leak connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void beforeConnectionReap(Connection connection) {
    logger.finer(() -> format("Reap connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void beforeConnectionReturn(Connection connection) {
    logger.finer(() -> format("Return connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void beforeConnectionValidation(Connection connection) {
    logger.finer(() -> format("Validate connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionAcquire(Connection connection) {
    logger
        .finer(() -> format("Acquired a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionCreation(Connection connection) {
    logger.finer(() -> format("Created a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionDestroy(Connection connection) {
    logger
        .finer(() -> format("Destroyed a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionFlush(Connection connection) {
    logger.finer(() -> format("Flushed a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionInvalid(Connection connection) {
    logger.fine(() -> format("Invalid connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionLeak(Connection connection, Thread thread) {
    logger
        .warning(() -> format("Leaked a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionPooled(Connection connection) {
    logger.finer(() -> format("Pooled a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionReap(Connection connection) {
    logger.finer(() -> format("Reaped a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionReturn(Connection connection) {
    logger
        .finer(() -> format("Returned a connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onConnectionValid(Connection connection) {
    logger.finer(() -> format("Valid connection: %s, URL: %s", connection, getURL(connection)));
  }

  @Override
  public void onWarning(String message) {
    logger.warning(() -> message);
  }

  @Override
  public void onWarning(Throwable throwable) {
    logger.log(Level.WARNING, "Data source occurred error!", throwable);
  }

  String getURL(Connection connection) {
    if (connection != null) {
      try {
        if (!connection.isClosed()) {
          return connection.getMetaData().getURL();
        }
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Get url from connection occurred error!", e);
      }
    }
    return "unknown";
  }
}
