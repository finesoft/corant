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
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.propertiesOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.replace;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import javax.sql.DataSource;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.corant.Corant;
import org.corant.config.CorantConfigResolver;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.kernel.util.CommandLine;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.datasource.shared.SqlStatements;
import org.corant.modules.jpa.shared.JPAExtension;
import org.corant.modules.jpa.shared.JPAUtils;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceUnitLiteral;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.ubiquity.Mutable.MutableInteger;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Throwables;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午2:15:40
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

  public static void stdoutUpdateSchema(String pu) {
    stdoutUpdateSchema(pu, ";");
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

  public static void validateNamedNativeQuery(String pu, String... integrations) {
    try (Corant corant = prepare()) {
      out(false);
      JPAExtension extension = select(JPAExtension.class).get();
      DataSourceService dataSourceService = select(DataSourceService.class).get();
      PersistenceUnitInfoMetaData pum =
          shouldNotNull(extension.getPersistenceUnitInfoMetaData(PersistenceUnitLiteral.of(pu)),
              "Can't find any metadata for persistence unit %s", pu);
      MetadataImplementor metadataImplementor = createMetadataImplementor(pu, integrations);
      DataSource ds = dataSourceService.resolve(pum.getJtaDataSourceName());
      if (metadataImplementor instanceof MetadataImpl mi) {
        List<Triple<String, String, Exception>> errors = new ArrayList<>();
        final int totals = sizeOf(mi.getNamedNativeQueryMap());
        final MutableInteger counter = new MutableInteger(1);
        mi.getNamedNativeQueryMap().forEach((k, v) -> {
          try (Connection conn = ds.getConnection();
              PreparedStatement ps = conn.prepareStatement(
                  SqlStatements.normalizeJdbcParameterPlaceHolder(v.getSqlQueryString()))) {
            System.out.printf("[VALID]: %s, columns %s, [%s/%s]%n", k,
                ps.getMetaData() != null ? ps.getMetaData().getColumnCount() : 0, counter, totals);
          } catch (Exception ex) {
            System.out.printf("[INVALID]: %s, [%s/%s]%n", k, counter, totals);
            errors.add(Triple.of(k, v.getSqlQueryString(), ex));
          } finally {
            counter.increment();
          }
        });
        System.out.println(
            "Validation completed" + (!errors.isEmpty() ? ", some errors were found" : EMPTY));
        if (!errors.isEmpty()) {
          errors.forEach(e -> {
            System.err.println("[ERROR QUERY]: " + e.left());
            System.err.println("[ERROR SQL]:\n" + e.middle());
            System.err.println("[ERROR MESSAGE]: " + e.right().getMessage());
            System.err.println("[ERROR STACK]:\n" + Throwables.stackTraceAsString(e.right()));
            System.err.println("*".repeat(100));
          });
        }
      } else {
        System.err.println("Not suppport named native query validation!");
      }
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void validateNamedQuery(String pu, String... integrations) {
    try (Corant corant = prepare()) {
      out(false);
      SessionFactoryImplementor sf =
          (SessionFactoryImplementor) createEntityManagerFactoryBuilderImpl(pu, integrations)
              .build();
      // 6.0 only HQL
      sf.getQueryEngine().validateNamedQueries();
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void validateSchema(String pu, String... integrations) {
    try (Corant corant = prepare()) {
      new SchemaValidator().validate(createMetadataImplementor(pu, integrations));
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static EntityManagerFactoryBuilderImpl createEntityManagerFactoryBuilderImpl(String pu,
      String... integrations) {
    Properties props = propertiesOf(integrations);
    HibernateJPAOrmProvider.DEFAULT_PROPERTIES.forEach(props::putIfAbsent);
    props.put(SchemaToolingSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY,
        UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
    props.put(SchemaToolingSettings.HBM2DDL_CHARSET_NAME, "UTF-8");
    props.put(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none");

    JPAExtension extension = select(JPAExtension.class).get();
    DataSourceService dataSourceService = select(DataSourceService.class).get();
    PersistenceUnitInfoMetaData pum =
        shouldNotNull(extension.getPersistenceUnitInfoMetaData(PersistenceUnitLiteral.of(pu)),
            "Can't find any metadata for persistence unit %s", pu);
    PersistenceUnitInfoMetaData usePum =
        pum.with(pum.getProperties(), PersistenceUnitTransactionType.JTA);
    usePum.configDataSource(dataSourceService::tryResolve);
    props.putAll(usePum.getProperties());
    return (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(usePum,
        props);
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
      String version = "V" + DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now());
      System.out.println("\n/**--Version: " + version + "__todo.sql");
      System.out.println("\n--<<<<<<<< Schema output end. **/\n");
    }
  }

  protected static Corant prepare() {
    LoggerFactory.disableAccessWarnings();
    LoggerFactory.disableLogger();
    CorantConfigResolver.adjust("corant.webserver.auto-start", "false",
        "corant.flyway.migrate.enable", "false", "corant.jta.transaction.auto-recovery", "false");
    return Corant.startup(HibernateOrmDeveloperKits.class,
        new String[] {Corant.DISABLE_BOOST_LINE_CMD,
            new CommandLine(Corant.DISABLE_BEFORE_START_HANDLER_CMD,
                "org.corant.modules.logging.Log4jProvider").toString()});
  }
}
