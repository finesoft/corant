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

import static java.lang.String.format;
import jakarta.enterprise.context.RequestScoped;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.transaction.TransactionScoped;
import org.corant.context.ComponentManager.AbstractComponentManager;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:15:28
 */
public abstract class JMSContextManager
    extends AbstractComponentManager<JMSContextKey, JMSContext> {

  private static final long serialVersionUID = 4705029747035011785L;

  @Override
  protected void preDestroy() {
    JMSRuntimeException jre = null;
    for (final JMSContext c : components.values()) {
      try {
        logger.fine(() -> format("Close JMSContext %s.", c));
        c.close();
      } catch (final JMSRuntimeException e) {
        jre = e;
      }
    }
    if (jre != null) {
      throw jre;
    }
  }

  @RequestScoped
  public static class RsJMSContextManager extends JMSContextManager {
    private static final long serialVersionUID = -3536990470379061678L;

  }
  @TransactionScoped
  public static class TsJMSContextManager extends JMSContextManager {
    private static final long serialVersionUID = -3536990470379061678L;
  }
}
