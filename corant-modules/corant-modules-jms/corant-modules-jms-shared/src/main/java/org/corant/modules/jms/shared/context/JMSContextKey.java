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
package org.corant.modules.jms.shared.context;

import static org.corant.context.Instances.findNamed;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.defaultTrim;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import javax.transaction.Status;
import javax.transaction.Synchronization;
import org.corant.modules.jms.shared.AbstractJMSExtension;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Objects;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:15:57
 *
 */
public class JMSContextKey implements Serializable {

  private static final long serialVersionUID = -9143619854361396089L;

  private static final Logger logger = Logger.getLogger(JMSContextKey.class.getName());
  private final boolean xa;
  private final String connectionFactoryId;
  private final Integer sessionMode;
  private final int hash;
  private volatile ConnectionFactory connectionFactory;

  public JMSContextKey(final String connectionFactoryId, final Integer sessionMode) {
    this.connectionFactoryId = defaultTrim(connectionFactoryId);
    this.sessionMode = sessionMode;
    xa = shouldNotNull(AbstractJMSExtension.getConfig(this.connectionFactoryId),
        "Can not find JMS connection factory config by id [%s]", this.connectionFactoryId).isXa();
    hash = Objects.hash(connectionFactoryId, sessionMode);
  }

  public static JMSContextKey of(final InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final JMSConnectionFactory factory = annotated.getAnnotation(JMSConnectionFactory.class);
    final JMSSessionMode sessionMode = annotated.getAnnotation(JMSSessionMode.class);
    return new JMSContextKey(factory == null ? null : factory.value(),
        sessionMode == null ? JMSContext.AUTO_ACKNOWLEDGE : sessionMode.value());
  }

  public JMSContext create() {
    try {
      if (xa && TransactionService.isCurrentTransactionActive()) {
        XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
        TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
        logger.fine(() -> "Create new XAJMSContext and register it to current transaction!");
        return ctx;
      } else {
        if (sessionMode != null && TransactionService.isCurrentTransactionActive()) {
          JMSContext ctx = connectionFactory().createContext(sessionMode);
          // FIXME will be changed in next iteration
          return registerToLocaleTransactionSynchronization(ctx);
        }
        return connectionFactory().createContext();
      }
    } catch (Exception ex) {
      throw new CorantRuntimeException(ex);
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
    final JMSContextKey key = (JMSContextKey) o;
    return areEqual(connectionFactoryId, key.connectionFactoryId)
        && areEqual(sessionMode, key.sessionMode);

  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  public Integer getSessionMode() {
    return sessionMode;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  public boolean isXa() {
    return xa;
  }

  @Override
  public String toString() {
    return "JMSContextKey [connectionFactoryId=" + connectionFactoryId + ", sessionMode="
        + sessionMode + "]";
  }

  ConnectionFactory connectionFactory() {
    if (connectionFactory != null) {
      return connectionFactory;
    }
    synchronized (this) {
      if (connectionFactory != null) {
        return connectionFactory;
      }
      return connectionFactory =
          findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
              () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
                  connectionFactoryId));
    }
  }

  // TODO In NO XA
  JMSContext registerToLocaleTransactionSynchronization(JMSContext jmscontext) {
    if (sessionMode == JMSContext.SESSION_TRANSACTED) {
      try {
        TransactionService.registerSynchronizationToCurrentTransaction(
            new LocalTransactionSynchronization(jmscontext));
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return jmscontext;
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 下午8:29:15
   *
   */
  static final class LocalTransactionSynchronization implements Synchronization {
    private final JMSContext jmscontext;

    /**
     * @param jmscontext
     */
    LocalTransactionSynchronization(JMSContext jmscontext) {
      this.jmscontext = jmscontext;
    }

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
  }

}