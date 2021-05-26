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
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.areEqual;
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
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.jms.shared.AbstractJMSConfig;
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
  private final AbstractJMSConfig config;
  private final String connectionFactoryId;
  private final boolean dupsOkAck;
  private final int hash;
  private volatile ConnectionFactory connectionFactory;

  public JMSContextKey(final String connectionFactoryId, final boolean dupsOkAck) {
    this.connectionFactoryId = Qualifiers.resolveName(connectionFactoryId);
    this.dupsOkAck = dupsOkAck;
    config = shouldNotNull(AbstractJMSExtension.getConfig(this.connectionFactoryId),
        "Can not find JMS connection factory config by id [%s]", this.connectionFactoryId);
    hash = Objects.hash(connectionFactoryId, dupsOkAck);
  }

  public static JMSContextKey of(final InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final JMSConnectionFactory factory = annotated.getAnnotation(JMSConnectionFactory.class);
    final JMSSessionMode mode = annotated.getAnnotation(JMSSessionMode.class);
    final boolean dupAck = mode != null && mode.value() == JMSContext.DUPS_OK_ACKNOWLEDGE;
    return new JMSContextKey(factory == null ? null : factory.value(), dupAck);
  }

  public JMSContext create(boolean xa) {
    try {
      if (xa) {
        shouldBeTrue(config.isXa(), "The connection factory %s can't support XA!",
            connectionFactoryId);
        XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
        TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
        logger.fine(() -> "Create new XAJMSContext and register it to current transaction!");
        return ctx;
      } else {
        return connectionFactory().createContext(
            dupsOkAck ? JMSContext.DUPS_OK_ACKNOWLEDGE : JMSContext.AUTO_ACKNOWLEDGE);
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
        && areEqual(dupsOkAck, key.dupsOkAck);

  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "JMSContextKey [connectionFactoryId=" + connectionFactoryId + ", dupsOkAck=" + dupsOkAck
        + "]";
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

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

}
