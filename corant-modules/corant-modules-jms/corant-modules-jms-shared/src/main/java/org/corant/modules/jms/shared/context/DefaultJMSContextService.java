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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import org.corant.modules.jms.context.JMSContextService;
import org.corant.modules.jms.shared.context.JMSContextManager.RsJMSContextManager;
import org.corant.modules.jms.shared.context.JMSContextManager.TsJMSContextManager;

/**
 * corant-modules-jms-shared
 *
 * TODO: Use pooled connection
 *
 * @author bingo 下午5:36:38
 *
 */
@ApplicationScoped
public class DefaultJMSContextService implements JMSContextService {

  @Inject
  @Any
  protected RsJMSContextManager rsJMSContextManager;

  @Inject
  @Any
  protected TsJMSContextManager tsJMSContextManager;

  @Override
  public JMSContext getJMSContext(final String connectionFactoryId, final boolean dupsOkAck) {
    return new ExtendedJMSContext(new JMSContextKey(connectionFactoryId, dupsOkAck),
        getRsJMSContextManager(), getTsJMSContextManager());
  }

  /**
   *
   * @return the rsJMSContextManager
   */
  public RsJMSContextManager getRsJMSContextManager() {
    return rsJMSContextManager;
  }

  /**
   *
   * @return the tsJMSContextManager
   */
  public TsJMSContextManager getTsJMSContextManager() {
    return tsJMSContextManager;
  }

  @Produces
  @Dependent
  protected JMSContext produce(final InjectionPoint ip) {
    return new ExtendedJMSContext(JMSContextKey.of(ip), getRsJMSContextManager(),
        getTsJMSContextManager());
  }

}
