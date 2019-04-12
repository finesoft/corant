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
package org.corant.suites.jms.shared;

import static org.corant.Corant.instance;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.isEquals;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSSessionMode;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午5:15:57
 *
 */
public class JMSContextKey implements Serializable {
  private static final long serialVersionUID = -9143619854361396089L;
  private static final Logger logger = Logger.getLogger(JMSContextKey.class.getName());
  private volatile ConnectionFactory connectionFactoryInstance;
  private final String connectionFactory;
  private final Integer session;
  private final int hash;

  public JMSContextKey(final String connectionFactory, final Integer session) {
    this.connectionFactory = connectionFactory;
    this.session = session;
    hash = calHash(connectionFactory, session);
  }

  public static JMSContextKey of(final InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final JMSConnectionFactory factory = annotated.getAnnotation(JMSConnectionFactory.class);
    final JMSSessionMode sessionMode = annotated.getAnnotation(JMSSessionMode.class);
    final String factoryName = factory == null ? null : factory.value();
    final int sesMod = sessionMode == null ? JMSContext.AUTO_ACKNOWLEDGE : sessionMode.value();
    return new JMSContextKey(factoryName, sesMod);
  }

  public JMSContext create() {
    ConnectionFactory cf = connectionFactory();
    if (cf instanceof XAConnectionFactory && JMSContextHolder.isInTransaction()) {
      XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
      try {
        instance().select(TransactionManager.class).get().getTransaction()
            .enlistResource(ctx.getXAResource());
        logger.info(() -> "Create new XAJMSContext and register it to current transaction!");
      } catch (IllegalStateException | RollbackException | SystemException e) {
        throw new CorantRuntimeException(e);
      }
      return ctx;
    } else {
      if (session != null) {
        JMSContext ctx = connectionFactory().createContext(session);
        // FIXME will be changed in next iteration
        return registerToLocaleTransactionSynchronization(ctx);
      }
      return connectionFactory().createContext();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JMSContextKey key = JMSContextKey.class.cast(o);
    return isEquals(connectionFactory, key.connectionFactory) && isEquals(session, key.session);

  }

  public String getConnectionFactory() {
    return connectionFactory;
  }

  public Integer getSession() {
    return session;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "JMSContextKey [connectionFactoryName=" + connectionFactory + ", session=" + session
        + "]";
  }

  int calHash(final String name, final Integer session) {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (session != null ? session.hashCode() : 0);
    return result;
  }

  ConnectionFactory connectionFactory() {
    if (connectionFactoryInstance != null) {
      return connectionFactoryInstance;
    }
    synchronized (this) {
      if (connectionFactoryInstance != null) {
        return connectionFactoryInstance;
      }
      return shouldNotNull(connectionFactoryInstance =
          AbstractJMSExtension.retriveConnectionFactory(connectionFactory));
    }
  }

  // TODO In NO XA
  JMSContext registerToLocaleTransactionSynchronization(JMSContext jmscontext) {
    if (JMSContextHolder.isInTransaction() && session == JMSContext.SESSION_TRANSACTED) {
      try {
        instance().select(TransactionManager.class).get().getTransaction()
            .registerSynchronization(new Synchronization() {
              @Override
              public void afterCompletion(int status) {
                if (status != Status.STATUS_COMMITTED) {
                  jmscontext.rollback();
                }
              }

              @Override
              public void beforeCompletion() {
                try {
                  jmscontext.commit();
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                }
              }
            });
      } catch (IllegalStateException | RollbackException | SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return jmscontext;
  }
}
