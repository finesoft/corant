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
package org.corant.suites.jndi;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import org.corant.Corant;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.normal.Names.JndiNames;

/**
 * corant-suites-jndi
 *
 * @author bingo 下午7:19:21
 *
 */
@ApplicationScoped
public class InitialContextProvider {

  private InitialContext context;

  @Inject
  private BeanManager beanManager;

  @Inject
  Logger logger;

  @Inject
  Corant corant;

  // touch, when corant ready service
  public void initialize(@Observes PostCorantReadyEvent event) {}

  @PostConstruct
  protected void onPostConstruct() {
    // if we can not found open context.
    boolean useCorantContext = false;
    try {
      if (!NamingManager.hasInitialContextFactoryBuilder()) {
        NamingManager.setInitialContextFactoryBuilder(e -> NamingContext::new);
        useCorantContext = true;
      }
    } catch (IllegalStateException | NamingException e) {
      logger.log(Level.WARNING, null, e);
    }

    try {
      context = new InitialContext();
      context.createSubcontext(JndiNames.JNDI_ROOT_NME);
      context.createSubcontext(JndiNames.JNDI_COMP_NME);
      context.createSubcontext(JndiNames.JNDI_APPS_NME);
      context.createSubcontext(JndiNames.JNDI_DATS_NME);
      context.bind(JndiNames.JNDI_COMP_NME + "/BeanManager", beanManager);
      beanManager.getEvent().fire(new PostCorantJndiReadyEvent(useCorantContext, context));
    } catch (NamingException e) {
      logger.log(Level.WARNING, null, e);
    }
  }

  @Produces
  @ApplicationScoped
  InitialContext initialContext() throws NamingException {
    return context;
  }
}
