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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.MapUtils.asProperties;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.tryCast;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassPaths;
import org.corant.suites.jpa.shared.JpaUtils;
import org.corant.suites.jpa.shared.PersistenceUnitMetaData;
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
import io.agroal.pool.DataSource;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午2:15:40
 *
 */
public class HibernateSchemaUtils {

  public static void stdoutPersistClasses(String pkg, Consumer<String> out) {
    new ArrayList<>(JpaUtils.getPersistenceClasses(pkg)).stream().map(Class::getName)
        .sorted(String::compareTo)
        .map(x -> new StringBuilder("<class>").append(x).append("</class>").toString())
        .forEach(out);
  }

  public static void stdoutPersistes(String pkg, Consumer<String> out) {
    System.out.println("<!-- mapping files -->");
    stdoutPersistJpaOrmXml(pkg, out);
    System.out.println("<!-- mapping classes -->");
    stdoutPersistClasses(pkg, out);
  }

  public static void stdoutPersistJpaOrmXml(String pkg, Consumer<String> out) {
    String packageNameToUse = shouldNotNull(pkg).replaceAll("\\.", "/");
    try {
      ClassPaths.from(packageNameToUse).getResources()
          .filter(f -> f.getResourceName().endsWith("JpaOrm.xml")).map(f -> f.getResourceName())
          .sorted(String::compareTo).forEach(s -> {
            if (s.contains(packageNameToUse) && s.endsWith("JpaOrm.xml")) {
              out.accept(new StringBuilder().append("<mapping-file>")
                  .append(s.substring(s.indexOf(packageNameToUse))).append("</mapping-file>")
                  .toString());
            }
          });
    } catch (Exception e) {

    }
  }

  public static void stdoutRebuildSchema(String pu) {
    out(false);
    new SchemaExport().setFormat(true).setDelimiter(";").execute(EnumSet.of(TargetType.STDOUT),
        Action.BOTH, createMetadataImplementor(pu));
    out(true);
  }

  public static void stdoutUpdateSchema(String pu) {
    out(false);
    new SchemaUpdate().setFormat(true).setDelimiter(";").execute(EnumSet.of(TargetType.STDOUT),
        createMetadataImplementor(pu));
    out(true);
  }

  @SuppressWarnings("deprecation")
  public static void validateNamedQuery(String pu) {
    out(false);
    SessionFactoryImplementor sf =
        SessionFactoryImplementor.class.cast(createMetadataImplementor(pu));
    sf.getNamedQueryRepository().checkNamedQueries(sf.getQueryPlanCache());
    out(true);
  }

  public static void validateSchema(String pu, String pkg) {
    new SchemaValidator().validate(createMetadataImplementor(pu));
  }

  static MetadataImplementor createMetadataImplementor(String pu, String... integrations) {
    System.setProperty("corant.temp.webserver.auto-start", "false");
    Properties props = asProperties(integrations);
    props.put(AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY,
        UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
    props.put(AvailableSettings.HBM2DDL_CHARSET_NAME, "UTF-8");
    Logger.getGlobal().setLevel(Level.OFF);
    Handler[] handlers = Logger.getGlobal().getHandlers();
    for (Handler handler : handlers) {
      Logger.getGlobal().removeHandler(handler);
    }
    Corant corant = new Corant(HibernateSchemaUtils.class);
    corant.start();
    InitialContext jndi = Corant.cdi().select(InitialContext.class).get();
    HibernateJpaExtension extension = Corant.cdi().select(HibernateJpaExtension.class).get();
    PersistenceUnitMetaData pum = shouldNotNull(extension.getPersistenceUnitMetaDatas().get(pu));
    PersistenceUnitMetaData usePum =
        pum.with(pum.getProperties(), PersistenceUnitTransactionType.JTA);
    usePum.configDataSource((dn) -> {
      try {
        return tryCast(jndi.lookup(dn), DataSource.class);
      } catch (NamingException e1) {
        throw new CorantRuntimeException(e1);
      }
    });
    props.putAll(usePum.getProperties());
    try {
      EntityManagerFactoryBuilderImpl emfb = EntityManagerFactoryBuilderImpl.class
          .cast(Bootstrap.getEntityManagerFactoryBuilder(usePum, props));
      emfb.build();
      return emfb.getMetadata();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static void out(boolean end) {
    if (!end) {
      System.out.println("\n/**-->>>>>>>> Schema output start**/");
    } else {
      System.out.println("\n/**--Version: V1_0_" + System.currentTimeMillis()
          + "\n\n--<<<<<<<< Schema output end. **/\n");
    }
  }
}
