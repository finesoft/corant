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
package org.corant.suites.datasource.shared;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import org.corant.config.PropertyInjector;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jta.shared.TransactionIntegration;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午2:23:54
 *
 */
public class JDBCTransactionIntegration implements TransactionIntegration {

  private static final Logger logger = Logger.getLogger(JDBCTransactionIntegration.class.getName());

  @Override
  public XAResource[] getRecoveryXAResources() {
    logger.info(() -> "Resolving JDBC XAResources for JTA recovery processes.");
    List<XAResource> res = new ArrayList<>();
    Instance<AbstractDataSourceExtension> extensions =
        CDI.current().select(AbstractDataSourceExtension.class);
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> {
        et.getConfigManager().getAllWithNames().values().forEach(cfg -> {
          if (cfg.isJta() && cfg.isXa()) {
            if (!XADataSource.class.isAssignableFrom(cfg.getDriver())) {
              logger.warning(() -> String.format(
                  "The data source [%s] is XA, but driver class is not a XA data source, recovery connections are only available for XADataSource.",
                  cfg.getName()));
            } else {
              try {
                XADataSource xads = cfg.getDriver().asSubclass(XADataSource.class)
                    .getDeclaredConstructor().newInstance();
                if (isNotEmpty(cfg.getJdbcProperties())) {
                  new PropertyInjector(xads).inject(cfg.getJdbcProperties());
                }
                res.add(getXAResource(xads, cfg));
                logger.info(() -> String.format(
                    "Added JDBC XA data source[%s] XAResource to JTA recovery processes.",
                    cfg.getName()));
              } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                  | InvocationTargetException | NoSuchMethodException | SecurityException
                  | SQLException e) {
                throw new CorantRuntimeException(e, "Unable to instantiate javax.sql.XADataSource");
              }
            }
          }
        });
      });
    }

    return res.toArray(new XAResource[res.size()]);
  }

  XAResource getXAResource(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    XAConnection conn = null;
    try {
      conn = xads.getXAConnection();
      return conn.getXAResource();
    } catch (SQLException nfe) {
      conn = xads.getXAConnection(cfg.getUsername(), cfg.getPassword());
      return conn.getXAResource();
    }
  }
}
