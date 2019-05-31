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
package org.corant.suites.jms.shared.context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.transaction.TransactionScoped;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午5:15:28
 *
 */
public abstract class JMSContextManager implements Serializable {

  private static final long serialVersionUID = 9142893804159414507L;

  final Logger logger = Logger.getLogger(getClass().getName());

  final transient Map<JMSContextKey, JMSContext> contexts = new ConcurrentHashMap<>();

  public JMSContext compute(final JMSContextKey key) {
    return contexts.computeIfAbsent(key, k -> {
      try {
        return k.create();
      } catch (JMSException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  public JMSContext get(final JMSContextKey key) {
    return contexts.get(key);
  }

  public void put(final JMSContextKey key, final JMSContext c) {
    contexts.put(key, c);
  }

  @PreDestroy
  void destroy() {
    if (contexts != null) {
      JMSRuntimeException jre = null;
      for (final JMSContext c : contexts.values()) {
        try {
          logger.fine(() -> String.format("Close JMSContext %s", c));
          c.close();
        } catch (final JMSRuntimeException e) {
          jre = e;
        }
      }
      if (jre != null) {
        throw jre;
      }
    }
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  @RequestScoped
  public static class RequestScopeContextManager extends JMSContextManager {

    private static final long serialVersionUID = 1256597268320899576L;
  }

  @TransactionScoped
  public static class TransactionScopeContextManager extends JMSContextManager {

    private static final long serialVersionUID = 5615193709079057726L;
  }
}