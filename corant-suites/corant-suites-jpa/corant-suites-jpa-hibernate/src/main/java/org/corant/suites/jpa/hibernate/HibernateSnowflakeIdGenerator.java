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
package org.corant.suites.jpa.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.corant.shared.util.Identifiers;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午2:06:44
 *
 */
public class HibernateSnowflakeIdGenerator implements IdentifierGenerator {
  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());
  static final String IDGEN_SF_WK_ID = "identifier.generator.snowflake.worker-id";
  static final String IDGEN_SF_DC_ID = "identifier.generator.snowflake.datacenter-id";
  static Identifiers.IdentifierGenerator GENERATOR;
  static volatile boolean ENABLED = false;
  static volatile String TSSQL = null;
  static volatile int DATA_CENTER_ID;
  static volatile int WORKER_ID;
  static {
    DATA_CENTER_ID =
        ConfigProvider.getConfig().getOptionalValue(IDGEN_SF_DC_ID, Integer.class).orElse(-1);
    WORKER_ID =
        ConfigProvider.getConfig().getOptionalValue(IDGEN_SF_WK_ID, Integer.class).orElse(0);
    logger.info(() -> String.format(
        "Use Snowflake id generator for hibernate data center id is %s, worker id is %s.",
        DATA_CENTER_ID, WORKER_ID));
  }

  public HibernateSnowflakeIdGenerator() {
    if (!ENABLED) {
      synchronized (HibernateSnowflakeIdGenerator.class) {
        if (!ENABLED) {
          if (DATA_CENTER_ID >= 0) {
            GENERATOR = Identifiers.snowflakeUUIDGenerator(DATA_CENTER_ID, WORKER_ID);
          } else {
            GENERATOR = Identifiers.snowflakeBufferUUIDGenerator(WORKER_ID, true);
          }
          ENABLED = true;
        }
      }
    }
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return GENERATOR.generate(() -> timeSeq(session));
  }

  long timeSeq(SharedSessionContractImplementor session) {
    if (TSSQL == null) {
      synchronized (HibernateSnowflakeIdGenerator.class) {
        if (TSSQL == null) {
          TSSQL = session.getFactory().getServiceRegistry().getService(JdbcServices.class)
              .getDialect().getCurrentTimestampSelectString();
        }
      }
    }
    try {
      final PreparedStatement st =
          session.getJdbcCoordinator().getStatementPreparer().prepareStatement(TSSQL);
      try {
        final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract(st);
        try {
          rs.next();
          long value = rs.getTimestamp(1).getTime();
          return value;
        } finally {
          try {
            session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(rs,
                st);
          } catch (Throwable ignore) {
          }
        }
      } finally {
        session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(st);
        session.getJdbcCoordinator().afterStatementExecution();
      }

    } catch (SQLException sqle) {
      throw session.getFactory().getServiceRegistry().getService(JdbcServices.class)
          .getSqlExceptionHelper().convert(sqle, "could not get next sequence value", TSSQL);
    }
  }
}
