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
package org.corant.suites.jms.artemis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSContext;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午7:55:53
 *
 */
@ApplicationScoped
public class ArtemisJmsContextFactory extends BasePooledObjectFactory<JMSContext> {

  @Inject
  ActiveMQConnectionFactory activeMQConnectionFactory;

  @Inject
  JMSContext globalContext;

  @Inject
  @ConfigProperty(name = "jms.artemis.context-pooled", defaultValue = "true")
  Boolean pooled;

  volatile GenericObjectPool<JMSContext> pool;

  @Override
  public JMSContext create() throws Exception {
    return activeMQConnectionFactory.createContext();
  }

  @Override
  public void destroyObject(PooledObject<JMSContext> p) throws Exception {
    p.getObject().close();
  }

  public JMSContext get() {
    if (!pooled) {
      return globalContext;
    }
    try {
      return pool.borrowObject();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public void release(JMSContext jmsContext) {
    if (pooled) {
      pool.returnObject(jmsContext);
    }
  }

  @Override
  public PooledObject<JMSContext> wrap(JMSContext obj) {
    return new DefaultPooledObject<>(obj);
  }

  @PostConstruct
  void onPostConstruct() {
    pool = new GenericObjectPool<>(this);
  }

  @PreDestroy
  void onPreDestroy() {
    if (pooled) {
      if (pool != null) {
        pool.close();
      }
    }
  }
}
