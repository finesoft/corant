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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.isEquals;
import java.io.Serializable;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSSessionMode;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午5:15:57
 *
 */
public class JMSContextKey implements Serializable {
  private static final long serialVersionUID = -9143619854361396089L;
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
    final JMSConnectionFactory jmsConnectionFactory =
        annotated.getAnnotation(JMSConnectionFactory.class);
    final JMSSessionMode sessionMode = annotated.getAnnotation(JMSSessionMode.class);
    return new JMSContextKey(jmsConnectionFactory.value(),
        sessionMode == null ? 1 : sessionMode.value());
  }

  public JMSContext create() {
    if (session != null) {
      return connectionFactory().createContext(session);
    }
    return connectionFactory().createContext();
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
    return result = 31 * result + (session != null ? session.hashCode() : 0);
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
}
