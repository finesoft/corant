/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.flyway;

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.StreamUtils.asStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.flyway.FlywayConfigProvider.DefaultFlywayConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * corant-suites-flyway
 *
 * @author bingo 下午7:51:13
 *
 */
@ApplicationScoped
public class FlywayMigrator {

  @Inject
  @ConfigProperty(name = "flyway.migrate.enable", defaultValue = "false")
  Boolean enable;

  @Inject
  Logger logger;

  @Inject
  @Any
  Instance<Callback> callbacks;

  @Inject
  @Any
  Instance<AbstractDataSourceExtension> dataSourceExtensions;

  public void migrate() {
    if (enable != null && enable.booleanValue()) {
      logger.info(() -> "Start migrate process");
      getConfigProviders().map(this::build).forEach(this::doMigrate);
    } else {
      logger.info(() -> String.format(
          "Disable migrate process, If you want to migrate, set %s in the configuration file!",
          "migrate.enable=true"));
    }
  }

  protected Flyway build(FlywayConfigProvider provider) {
    DataSource ds = provider.getDataSource();
    Collection<String> locations = provider.getLocations();
    Set<String> locationsToUse =
        locations == null ? asSet(defaultLocation(ds)) : new HashSet<>(locations);
    logger.info(() -> String.format("Build flyway instance that location is [%s]",
        String.join(";", locationsToUse.toArray(new String[0]))));
    FluentConfiguration fc =
        Flyway.configure().dataSource(ds).locations(locationsToUse.toArray(new String[0]));
    if (!callbacks.isUnsatisfied()) {
      fc.callbacks(callbacks.stream().toArray(Callback[]::new));
    }
    config(provider, fc);
    return fc.load();
  }

  protected void config(FlywayConfigProvider provider, FluentConfiguration fc) {}

  protected String defaultLocation(DataSource ds) {
    return "META-INF/dbmigration";
  }

  protected void doMigrate(Flyway flyway) {
    flyway.migrate();
  }

  protected Stream<FlywayConfigProvider> getConfigProviders() {
    // FIXME
    if (!dataSourceExtensions.isUnsatisfied()) {
      return dataSourceExtensions.stream().flatMap(dse -> {
        return asStream(dse.getDataSources()).map((e) -> DefaultFlywayConfigProvider
            .of(defaultLocation(null) + "/" + e.getKey(), e.getValue()));
      });
    } else {
      return Stream.empty();
    }
  }
}
