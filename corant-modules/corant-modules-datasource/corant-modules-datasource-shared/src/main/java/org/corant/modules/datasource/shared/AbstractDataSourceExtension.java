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
package org.corant.modules.datasource.shared;

import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.config.declarative.ConfigInstances;
import org.corant.context.naming.NamingReference;
import org.corant.context.qualifier.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;

/**
 * corant-modules-datasource-shared
 *
 * Initialize the named qualifier data source bean for injection, use Unnamed qualifier for
 * injection while the configurations do not assign a name.
 *
 * @author bingo 上午12:18:32
 *
 */
public abstract class AbstractDataSourceExtension implements Extension {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  protected volatile NamedQualifierObjectManager<DataSourceConfig> configManager =
      NamedQualifierObjectManager.empty();
  protected volatile InitialContext jndi;

  /**
   * Returns a named qualifier configuration object manager for data source
   *
   * @return the configManager
   */
  public NamedQualifierObjectManager<DataSourceConfig> getConfigManager() {
    return configManager;
  }

  /**
   * Collect the data source configurations for produce data source bean.
   *
   * @param bbd the CDI event
   */
  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    Map<String, DataSourceConfig> configs = ConfigInstances.resolveMulti(DataSourceConfig.class);
    configManager = new DefaultNamedQualifierObjectManager<>(configs.values());
    if (configManager.isEmpty()) {
      logger.info(() -> "Can not find any data source configurations.");
    } else {
      logger.fine(() -> String.format("Found %s data sources named [%s].", configManager.size(),
          String.join(", ", configManager.getAllDisplayNames())));
    }
  }

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    configManager.destroy();
  }

  protected synchronized void registerJndi(String name, Annotation... qualifiers) {
    if (isNotBlank(name)) {
      try {
        if (jndi == null) {
          jndi = new InitialContext();
          jndi.createSubcontext(DataSourceConfig.JNDI_SUBCTX_NAME);
        }
        String jndiName = DataSourceConfig.JNDI_SUBCTX_NAME + "/" + name;
        jndi.bind(jndiName, new NamingReference(DataSource.class, qualifiers));
        logger.fine(() -> String.format("Bind data source %s to jndi.", jndiName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

}
