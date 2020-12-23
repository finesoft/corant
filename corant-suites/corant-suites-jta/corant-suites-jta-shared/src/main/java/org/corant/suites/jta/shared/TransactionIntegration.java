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
package org.corant.suites.jta.shared;

import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.transaction.xa.XAResource;
import org.corant.context.CDIs;

/**
 * corant-suites-jta-shared
 *
 * @author bingo 下午2:47:59
 *
 */
public interface TransactionIntegration {

  Logger LOGGER = Logger.getLogger(TransactionIntegration.class.getName());

  default void destroy() {}

  default TransactionConfig getConfig() {
    if (CDIs.isEnabled()) {
      Instance<TransactionExtension> txExt = CDI.current().select(TransactionExtension.class);
      if (txExt.isResolvable()) {
        return txExt.get().getConfig();
      }
    }
    return TransactionConfig.empty();
  }

  XAResource[] getRecoveryXAResources();

}
