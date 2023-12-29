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
package org.corant.modules.jta.narayana;

import javax.transaction.xa.XAResource;
import org.corant.modules.jta.shared.TransactionIntegration;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

/**
 * corant-modules-jta-narayana
 *
 * @author bingo 下午2:37:43
 */
public class NarayanaXAResourceRecoveryHelper implements XAResourceRecoveryHelper {

  private final TransactionIntegration integration;

  public NarayanaXAResourceRecoveryHelper(TransactionIntegration integration) {
    this.integration = integration;
  }

  public void destroy() {
    if (integration != null) {
      integration.destroy();
    }
  }

  @Override
  public XAResource[] getXAResources() throws Exception {
    return integration.getRecoveryXAResources();
  }

  @Override
  public boolean initialise(String p) throws Exception {
    return true;
  }
}
