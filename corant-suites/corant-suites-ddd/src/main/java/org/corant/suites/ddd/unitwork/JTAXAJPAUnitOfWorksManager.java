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
package org.corant.suites.ddd.unitwork;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transaction;
import org.corant.suites.ddd.annotation.qualifier.JTAXA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;

/**
 * corant-suites-ddd
 *
 * <p>
 * The JTA JPA unit of works manager, use for create and destroy the JTAXAJPAUnitOfWork provide the
 * necessary message dispatch service for the unit of work.
 * </p>
 *
 * @author bingo 下午2:14:21
 *
 */
@JTAXA
@ApplicationScoped
@InfrastructureServices
public class JTAXAJPAUnitOfWorksManager extends AbstractJTAJPAUnitOfWorksManager {

  @Override
  protected JTAXAJPAUnitOfWork buildUnitOfWork(Transaction transaction) {
    return new JTAXAJPAUnitOfWork(this, transaction);
  }

}
