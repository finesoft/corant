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

import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jndi.DefaultReference;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-datasource-shared
 *
 * Initialize the named qualifier data source bean for injection, use Unnamed qualifier for
 * injection while the configurations do not assign a name.
 *
 * @author bingo 上午12:18:32
 *
 */
public abstract class AbstractDataSourceExtension implements Extension {

  protected final Map<String, DataSourceConfig> dataSourceConfigs = new HashMap<>();
  protected final Set<String> dataSourceNames = new LinkedHashSet<>();
  protected final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  protected volatile boolean initedJndiSubCtx = false;
  protected volatile InitialContext jndi;

  public DataSource getDataSource(String name) {
    return getDataSources().get(name);
  }

  public Set<String> getDataSourceNames() {
    return Collections.unmodifiableSet(dataSourceNames);
  }

  public Map<String, DataSource> getDataSources() {
    return Collections.unmodifiableMap(dataSources);
  }

  protected Map<String, DataSourceConfig> getDataSourceConfigs() {
    return Collections.unmodifiableMap(dataSourceConfigs);
  }

  /**
   * Collect the data source configurations for produce data source bean.
   *
   * @param bbd onBeforeBeanDiscovery
   */
  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    dataSourceConfigs.clear();
    dataSourceNames.clear();
    dataSources.clear();
    DataSourceConfig.from(ConfigProvider.getConfig()).forEach(dataSourceConfigs::put);
    dataSourceNames.addAll(dataSourceConfigs.keySet());
    if (dataSourceConfigs.isEmpty()) {
      logger.info(() -> "Can not find any data source configurations.");
    } else {
      logger.info(() -> String.format("Find %s data source names %s", dataSourceNames.size(),
          String.join(", ", dataSourceNames)));
    }
  }

  protected synchronized void registerDataSource(String name, DataSource dataSource) {
    shouldBeNull(dataSources.put(name, dataSource), "The data source annotated %s dup!", name);
  }

  protected synchronized void registerJndi(String name, Annotation... qualifiers) {
    if (isNotBlank(name)) {
      try {
        if (jndi == null) {
          jndi = new InitialContext();
        }
        if (!initedJndiSubCtx) {
          jndi.createSubcontext(DataSourceConfig.JNDI_SUBCTX_NAME);
          initedJndiSubCtx = true;
        }
        String jndiName = DataSourceConfig.JNDI_SUBCTX_NAME + "/" + name;
        jndi.bind(jndiName, new DefaultReference(DataSource.class, qualifiers));
        logger.info(() -> String.format("Bind data source %s to jndi!", jndiName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

}
