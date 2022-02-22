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
package org.corant.modules.jndi;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.JndiNames;

/**
 * corant-modules-jndi
 *
 * @author bingo 下午7:19:21
 *
 */
public class InitialContextExtension implements Extension {

  static final String[] DFLT_SUB_CTX =
      {JndiNames.JNDI_ROOT_NME, JndiNames.JNDI_COMP_NME, JndiNames.JNDI_APPS_NME};
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private boolean useCorantContext = false;
  private InitialContext context;

  public InitialContext getContext() {
    return context;
  }

  public boolean isUseCorantContext() {
    return useCorantContext;
  }

  void onAfterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    try {
      getContext().bind(JndiNames.JNDI_COMP_NME + "/BeanManager", bm);
      abd.<InitialContext>addBean().addQualifier(Default.Literal.INSTANCE)
          .addTransitiveTypeClosure(InitialContext.class).beanClass(InitialContext.class)
          .scope(ApplicationScoped.class).produceWith(beans -> getContext())
          .disposeWith((jndi, beans) -> {
            try {
              jndi.close();
            } catch (NamingException e) {
              logger.log(Level.WARNING, "An error occurred while closing the context.", e);
            }
          });
    } catch (NamingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery bbd, BeanManager bm) {
    try {
      if (!NamingManager.hasInitialContextFactoryBuilder()) {
        NamingManager.setInitialContextFactoryBuilder(e -> DefaultInitialContextFactory::build);
        // NamingManager.setObjectFactoryBuilder((o, e) -> DefaultObjectFactory.build(o, e));
        useCorantContext = true;
      }
      context = new InitialContext();
    } catch (IllegalStateException | NamingException e) {
      logger.log(Level.WARNING, "An error occurred initializing the context.", e);
    }

    if (context == null) {
      return;
    }

    for (String subCtx : DFLT_SUB_CTX) {
      try {
        context.createSubcontext(subCtx);
      } catch (NamingException e) {
        logger.log(Level.WARNING, null, e);
      }
    }
    if (useCorantContext) {
      logger.info(() -> String.format("Initial corant naming context, create subcontexts with %s.",
          String.join(", ", DFLT_SUB_CTX)));
    } else {
      logger.info(() -> String.format("Initial naming context, create subcontexts with %s.",
          String.join(", ", DFLT_SUB_CTX)));
    }
    bm.getEvent().fire(new PostCorantJNDIReadyEvent(useCorantContext, context));
  }

  void onBeforeShutdown(@Observes BeforeShutdown bs) {
    if (DefaultInitialContextFactory.initialContext != null) {
      ((NamingContext) DefaultInitialContextFactory.initialContext).release();
    }
  }

}
