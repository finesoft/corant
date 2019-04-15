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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import org.corant.Corant;
import org.corant.kernel.exception.GeneralRuntimeException;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午5:15:28
 *
 */
public abstract class JMSContextHolder implements Serializable {

  private static final long serialVersionUID = 9142893804159414507L;

  final Logger logger = Logger.getLogger(getClass().getName());

  final transient Map<JMSContextKey, JMSContext> contexts = new ConcurrentHashMap<>();

  static boolean isInTransaction() {
    if (Corant.instance().select(TransactionManager.class).isResolvable()) {
      try {
        int status = Corant.instance().select(TransactionManager.class).get().getStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING
            || status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_PREPARED
            || status == Status.STATUS_PREPARING || status == Status.STATUS_ROLLING_BACK;
      } catch (SystemException e) {
        throw new GeneralRuntimeException(e);
      }
    }
    return false;
  }

  public JMSContext compute(final JMSContextKey key) {
    return contexts.computeIfAbsent(key, k -> k.create());
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

  @RequestScoped
  public static class RequestScopeContextHolder extends JMSContextHolder {

    private static final long serialVersionUID = 1256597268320899576L;
  }

  @TransactionScoped
  public static class TransactionScopeContextHolder extends JMSContextHolder {

    private static final long serialVersionUID = 5615193709079057726L;
  }
}
