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
package org.corant.suites.jta.narayana;

import static org.corant.Corant.instance;
import static org.corant.shared.util.Empties.isEmpty;
import java.util.Collection;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Singleton;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import javax.transaction.UserTransaction;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午7:19:18
 *
 */
public class NarayanaExtension implements Extension {

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
    if (event != null) {
      final Collection<? extends Bean<?>> userTransactionBeans =
          beanManager.getBeans(UserTransaction.class);
      if (isEmpty(userTransactionBeans)) {
        // For OpenWebBeans
        event.addBean().types(UserTransaction.class)
            .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).scope(Dependent.class)
            .createWith(cc -> com.arjuna.ats.jta.UserTransaction.userTransaction());
      }
      event.addBean().id(Transaction.class.getName())
          .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).types(Transaction.class)
          .scope(TransactionScoped.class).createWith(cc -> {
            try {
              return instance().select(TransactionManager.class).get().getTransaction();
            } catch (final SystemException systemException) {
              throw new CreationException(systemException.getMessage(), systemException);
            }
          });
      event.addBean().addTransitiveTypeClosure(JTAEnvironmentBean.class)
          .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).scope(Singleton.class)
          .createWith(cc -> BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class));
    }
  }

}
