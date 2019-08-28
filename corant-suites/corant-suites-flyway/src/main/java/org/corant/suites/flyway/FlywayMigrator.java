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
package org.corant.suites.flyway;

import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.defaultString;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.kernel.api.DataSourceService;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.Resources;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.datasource.shared.DataSourceConfig;
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
  @ConfigProperty(name = "flyway.migrate.cleanOnValidationError", defaultValue = "false")
  Boolean cleanOnValidationError;

  @Inject
  @ConfigProperty(name = "flyway.migrate.cleanDisabled", defaultValue = "true")
  Boolean cleanDisabled;

  @Inject
  @ConfigProperty(name = "flyway.migrate.file-path")
  Optional<String> filePath;

  @Inject
  Logger logger;

  @Inject
  @Any
  Instance<Callback> callbacks;

  @Inject
  @Any
  Instance<DataSourceService> dataSourceService;

  @Inject
  @Any
  Instance<AbstractDataSourceExtension> dataSourceExtensions;

  public void migrate(@Observes PostCorantReadyEvent e) {
    if (enable.booleanValue()) {
      logger.info(() -> "Perform migrate process if necessary...");
      getConfigProviders().map(this::build).filter(ObjectUtils::isNotNull).forEach(this::doMigrate);
      logger.info(() -> "Finished migrate process.");
    } else {
      logger.info(() -> String.format(
          "Disable migrate process, If you want to migrate, set %s in the configuration file!",
          "flyway.migrate.enable=true"));
    }
  }

  protected Optional<Set<String>> additionalLocation(FlywayConfigProvider provider) {
    return Optional.empty();
  }

  protected Flyway build(FlywayConfigProvider provider) {
    DataSource ds = provider.getDataSource();
    Set<String> locationsToUse = new LinkedHashSet<>();
    for (String location : provider.getLocations()) {
      if (checkLocation(location)) {
        locationsToUse.add(location);
      }
    }
    additionalLocation(provider).ifPresent(adls -> {
      for (String adl : adls) {
        if (checkLocation(adl)) {
          locationsToUse.add(adl);
        }
      }
    });
    if (!locationsToUse.isEmpty()) {
      logger.info(() -> String.format("Build flyway instance from locations [%s]",
          String.join(",", locationsToUse)));
      FluentConfiguration fc = Flyway.configure().dataSource(ds)
          .cleanOnValidationError(cleanOnValidationError).cleanDisabled(cleanDisabled)
          .locations(locationsToUse.toArray(new String[locationsToUse.size()]));
      if (!callbacks.isUnsatisfied()) {
        fc.callbacks(callbacks.stream().toArray(Callback[]::new));
      }
      config(provider, fc);
      return fc.load();
    }
    return null;
  }

  protected boolean checkLocation(String location) {
    try {
      return Resources.from(location)
          .anyMatch(r -> r.getLocation().toLowerCase(Locale.ROOT).endsWith(".sql"));
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can't find any migrated data from location %s", location));
      return false;
    }
  }

  protected void config(FlywayConfigProvider provider, FluentConfiguration fc) {}

  protected void doMigrate(Flyway flyway) {
    flyway.migrate();
  }

  protected Stream<FlywayConfigProvider> getConfigProviders() {
    if (!dataSourceExtensions.isUnsatisfied()) {
      return dataSourceExtensions.stream().flatMap(dse -> {
        return streamOf(dse.getConfigManager().getAllNames()).map(this::resolveConfigProvider);
      });
    } else {
      return Stream.empty();
    }
  }

  protected String getLocation(String name) {
    if (filePath != null && filePath.isPresent()) {
      return filePath.get() + "/" + name;
    } else {
      return "META-INF/dbmigration/" + name;
    }
  }

  protected DefaultFlywayConfigProvider resolveConfigProvider(String name) {
    if (defaultString(name).startsWith(DataSourceConfig.JNDI_SUBCTX_NAME)) {
      try {
        return DefaultFlywayConfigProvider.of(
            getLocation(name.substring(name.indexOf(DataSourceConfig.JNDI_SUBCTX_NAME))),
            forceCast(new InitialContext().lookup(name)));
      } catch (NamingException ex) {
        throw new CorantRuntimeException(ex);
      }
    } else if (dataSourceService.isResolvable()) {
      return DefaultFlywayConfigProvider.of(getLocation(name), dataSourceService.get().get(name));
    }
    throw new CorantRuntimeException("Can not found any data source named %s", name);

  }
}
