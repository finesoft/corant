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
package org.corant.modules.flyway;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.config.Configs;
import org.corant.modules.datasource.shared.AbstractDataSourceExtension;
import org.corant.modules.datasource.shared.DataSourceConfig;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.flyway.FlywayConfigProvider.DefaultFlywayConfigProvider;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Resources;
import org.corant.shared.util.StopWatch;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.MigrationResolver;

/**
 * corant-modules-flyway
 *
 * FIXME
 *
 * @author bingo 下午7:51:13
 *
 */
@ApplicationScoped
public class FlywayMigrator {

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<Callback> callbacks;

  @Inject
  @Any
  protected Instance<JavaMigration> javaMigrations;

  @Inject
  @Any
  protected Instance<MigrationResolver> migrationResolvers;

  @Inject
  @Any
  protected Instance<DataSourceService> dataSourceService;

  @Inject
  @Any
  protected Instance<AbstractDataSourceExtension> dataSourceExtensions;

  protected FlywayConfig globalFlywayConfig;

  // @Transactional // FIXME use JTA XA, the flyway migration schema history will be rollback
  public void migrate() {
    if (globalFlywayConfig.isEnable()) {
      StopWatch sw = StopWatch.press("Flyway migration");
      logger.info(() -> "Perform migration process if necessary...");
      getConfigProviders().map(this::build).filter(Objects::isNotNull).forEach(this::doMigrate);
      logger.info(() -> "Finished migration process.");
      sw.destroy(logger);
    } else {
      logger.fine(() -> String.format(
          "Disable migration process, If you want to migrate, set %s in the configuration file!",
          "corant.flyway.migrate.enable=true"));
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
      FluentConfiguration fc = Flyway.configure().configuration(globalFlywayConfig).dataSource(ds)
          .cleanDisabled(true).locations(locationsToUse.toArray(new String[0]));
      if (!callbacks.isUnsatisfied()) {
        fc.callbacks(callbacks.stream().toArray(Callback[]::new));
      }
      if (!javaMigrations.isUnsatisfied()) {
        fc.javaMigrations(javaMigrations.stream().toArray(JavaMigration[]::new));
      }
      if (!migrationResolvers.isUnsatisfied()) {
        fc.resolvers(migrationResolvers.stream().toArray(MigrationResolver[]::new));
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
          () -> String.format("Can't find any migrated data from location %s.", location));
      return false;
    }
  }

  protected void config(FlywayConfigProvider provider, FluentConfiguration fc) {}

  protected void doMigrate(Flyway flyway) {
    flyway.migrate();
  }

  protected Stream<FlywayConfigProvider> getConfigProviders() {
    if (!dataSourceExtensions.isUnsatisfied()) {
      return dataSourceExtensions.stream().flatMap(
          dse -> streamOf(dse.getConfigManager().getAllNames()).map(this::resolveConfigProvider));
    } else {
      return Stream.empty();
    }
  }

  protected String getLocation(String name) {
    return isNotBlank(name) ? globalFlywayConfig.getLocationPrefix() + "/" + name + "/"
        : globalFlywayConfig.getLocationPrefix() + "/";
  }

  @PostConstruct
  protected void onPostConstruct() {
    globalFlywayConfig =
        defaultObject(Configs.resolveSingle(FlywayConfig.class), FlywayConfig.EMPTY);
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
      if (globalFlywayConfig.isUseDriverManagerDataSource()
          && dataSourceExtensions.isResolvable()) {
        return DefaultFlywayConfigProvider.of(getLocation(name),
            dataSourceService.get().get(dataSourceExtensions.get().getConfigManager().get(name)));
      } else {
        return DefaultFlywayConfigProvider.of(getLocation(name),
            dataSourceService.get().resolve(name));
      }
    }
    throw new CorantRuntimeException("Can not found any data source named %s.", name);
  }
}
