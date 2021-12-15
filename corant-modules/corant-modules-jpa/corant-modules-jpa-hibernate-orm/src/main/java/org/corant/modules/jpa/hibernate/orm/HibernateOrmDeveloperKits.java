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
package org.corant.modules.jpa.hibernate.orm;

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.propertiesOf;
import static org.corant.shared.util.Strings.replace;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;
import java.util.function.Consumer;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.corant.Corant;
import org.corant.config.CorantConfigResolver;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.kernel.util.CommandLine;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.jpa.shared.JPAExtension;
import org.corant.modules.jpa.shared.JPAUtils;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceUnitLiteral;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.util.Resources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.TargetType;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午2:15:40
 *
 */
public class HibernateOrmDeveloperKits {

  public static final String JPA_ORM_XML_NAME_END_WITH = "JpaOrm.xml";

  public static void stdoutPersistClasses(String pkg, Consumer<String> out) {
    try (Corant corant = prepare()) {
      new ArrayList<>(JPAUtils.getPersistenceClasses(pkg)).stream().map(Class::getName)
          .sorted(String::compareTo).forEach(out);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutPersistes(String pkg, Consumer<String> ormFilesOut,
      Consumer<String> entityClsOut) {
    try (Corant corant = prepare()) {
      stdoutPersistes(pkg, JPA_ORM_XML_NAME_END_WITH, ormFilesOut, entityClsOut);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutPersistes(String pkg, String ormFileNameEndWith,
      Consumer<String> ormFilesOut, Consumer<String> entityClsOut) {
    try (Corant corant = prepare()) {
      stdoutPersistJpaOrmXml(pkg, ormFileNameEndWith, ormFilesOut);
      stdoutPersistClasses(pkg, entityClsOut);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutPersistJpaOrmXml(String pkg, Consumer<String> out) {
    try (Corant corant = prepare()) {
      stdoutPersistJpaOrmXml(pkg, JPA_ORM_XML_NAME_END_WITH, out);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutPersistJpaOrmXml(String pkg, String endWith, Consumer<String> out) {
    try (Corant corant = prepare()) {
      String usePkg = replace(pkg, ".", "/");
      Resources.fromClassPath(usePkg).filter(f -> f.getLocation().endsWith(endWith))
          .map(ClassPathResource::getClassPath).sorted(String::compareTo).forEach(s -> {
            if (s.contains(usePkg) && s.endsWith(endWith)) {
              out.accept(s.substring(s.indexOf(usePkg)));
            }
          });
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutRebuildSchema(String pu) {
    try (Corant corant = prepare()) {
      out(false);
      new SchemaExport().setFormat(true).setDelimiter(";").execute(EnumSet.of(TargetType.STDOUT),
          Action.BOTH, createMetadataImplementor(pu));
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void stdoutUpdateSchema(String pu, String... integrations) {
    stdoutUpdateSchema(pu, ";", integrations);
  }

  public static void stdoutUpdateSchema(String pu, String delimiter, String... integrations) {
    try (Corant corant = prepare()) {
      out(false);
      new SchemaUpdate().setFormat(true).setDelimiter(delimiter)
          .execute(EnumSet.of(TargetType.STDOUT), createMetadataImplementor(pu, integrations));
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  @SuppressWarnings("deprecation")
  public static void validateNamedQuery(String pu, String... integrations) {
    try (Corant corant = prepare()) {
      out(false);
      SessionFactoryImplementor sf =
          (SessionFactoryImplementor) createEntityManagerFactoryBuilderImpl(pu, integrations)
              .build();
      sf.getNamedQueryRepository().checkNamedQueries(sf.getQueryPlanCache());
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void validateSchema(String pu, String pkg) {
    try (Corant corant = prepare()) {
      new SchemaValidator().validate(createMetadataImplementor(pu));
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static EntityManagerFactoryBuilderImpl createEntityManagerFactoryBuilderImpl(String pu,
      String... integrations) {
    Properties props = propertiesOf(integrations);
    props.put(AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY,
        UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
    props.put(AvailableSettings.HBM2DDL_CHARSET_NAME, "UTF-8");
    props.put(AvailableSettings.HBM2DDL_DATABASE_ACTION, "none");
    JPAExtension extension = select(JPAExtension.class).get();
    DataSourceService dataSourceService = select(DataSourceService.class).get();
    PersistenceUnitInfoMetaData pum =
        shouldNotNull(extension.getPersistenceUnitInfoMetaData(PersistenceUnitLiteral.of(pu)));
    PersistenceUnitInfoMetaData usePum =
        pum.with(pum.getProperties(), PersistenceUnitTransactionType.JTA);
    usePum.configDataSource(dsn -> dataSourceService.tryResolve(dsn));
    props.putAll(usePum.getProperties());
    return EntityManagerFactoryBuilderImpl.class
        .cast(Bootstrap.getEntityManagerFactoryBuilder(usePum, props));
  }

  protected static MetadataImplementor createMetadataImplementor(String pu,
      String... integrations) {
    EntityManagerFactoryBuilderImpl entityManagerFactoryBuilderImpl =
        createEntityManagerFactoryBuilderImpl(pu, integrations);
    entityManagerFactoryBuilderImpl.build();
    return entityManagerFactoryBuilderImpl.getMetadata();
  }

  protected static void out(boolean end) {
    if (!end) {
      System.out.println("\n/**-->>>>>>>> Schema output start**/");
    } else {
      String version =
          "V" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
      System.out.println("\n/**--Version: " + version + "__todo.sql");
      System.out.println("\n--<<<<<<<< Schema output end. **/\n");
    }
  }

  protected static Corant prepare() {
    LoggerFactory.disableAccessWarnings();
    LoggerFactory.disableLogger();
    CorantConfigResolver.adjust("corant.webserver.auto-start", "false",
        "corant.flyway.migrate.enable", "false");
    return Corant.startup(HibernateOrmDeveloperKits.class,
        new String[] {Corant.DISABLE_BOOST_LINE_CMD,
            new CommandLine(Corant.DISABLE_BEFORE_START_HANDLER_CMD,
                "org.corant.modules.logging.Log4jProvider").toString()});
  }
}
