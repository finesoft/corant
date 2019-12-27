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

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.jms.XAConnectionFactory;
import javax.transaction.xa.XAResource;
import org.corant.suites.jta.shared.TransactionIntegration;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午5:12:28
 *
 */
public class JMSTransactionIntegration implements TransactionIntegration {

  private static final Logger logger = Logger.getLogger(JMSTransactionIntegration.class.getName());

  @Override
  public XAResource[] getRecoveryXAResources() {
    logger.info(() -> "Resolving JMS XAResources for JTA recovery processes.");
    Instance<AbstractJMSExtension> extensions = CDI.current().select(AbstractJMSExtension.class);
    List<XAResource> resources = new ArrayList<>();
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().forEach((k, v) -> {
        if (v.isXa() && v.isEnable()) {
          resolveNamed(XAConnectionFactory.class, k)
              .ifPresent(xacf -> resources.add(xacf.createXAContext().getXAResource()));
          logger
              .info(() -> String.format("Added JMS[%s] XAResource to JTA recovery processes.", k));
        }
      }));
    }
    return isNotEmpty(resources) ? resources.toArray(new XAResource[resources.size()])
        : new XAResource[0];
  }

}
