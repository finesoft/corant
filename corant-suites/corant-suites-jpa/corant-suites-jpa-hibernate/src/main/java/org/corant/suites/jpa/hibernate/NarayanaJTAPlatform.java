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
package org.corant.suites.jpa.hibernate;

import static org.corant.kernel.util.Instances.resolveAnyway;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.corant.kernel.service.TransactionService;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午6:49:00
 *
 */
public class NarayanaJTAPlatform extends AbstractJtaPlatform {

  private static final long serialVersionUID = -6662006780960101741L;

  @Override
  protected TransactionManager locateTransactionManager() {
    return resolveAnyway(TransactionService.class).getTransactionManager();
  }

  @Override
  protected UserTransaction locateUserTransaction() {
    return resolveAnyway(TransactionService.class).getUserTransaction();
  }

}
