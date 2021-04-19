/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.context.concurrent;

import static org.corant.shared.util.Strings.isNotBlank;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import org.corant.config.declarative.ConfigInstances;
import org.corant.context.concurrent.ContextServiceConfig.ContextInfo;
import org.corant.context.concurrent.executor.DefaultContextService;
import org.corant.context.concurrent.executor.DefaultManagedExecutorService;
import org.corant.context.concurrent.executor.DefaultManagedScheduledExecutorService;
import org.corant.context.concurrent.executor.DefaultManagedThreadFactory;
import org.corant.context.concurrent.executor.ExecutorServiceManager;
import org.corant.context.concurrent.provider.BlockingQueueProvider;
import org.corant.context.concurrent.provider.ContextSetupProviderImpl;
import org.corant.context.concurrent.provider.TransactionSetupProviderImpl;
import org.corant.context.qualifier.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.JndiNames;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceAdapter;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceAdapter;

/**
 * corant-context
 *
 * @author bingo 下午2:51:48
 *
 */
public class ConcurrentExtension implements Extension {

  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/concurrent";
  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  protected volatile NamedQualifierObjectManager<ManagedExecutorConfig> executorConfigs =
      NamedQualifierObjectManager.empty();
  protected volatile NamedQualifierObjectManager<ManagedScheduledExecutorConfig> scheduledExecutorConfigs =
      NamedQualifierObjectManager.empty();
  protected volatile NamedQualifierObjectManager<ContextServiceConfig> contextServiceConfigs =
      NamedQualifierObjectManager.empty();

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      if (executorConfigs.isEmpty()) {
        event.<ManagedExecutorService>addBean().addTransitiveTypeClosure(ExecutorService.class)
            .addTransitiveTypeClosure(ManagedExecutorService.class)
            .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE)
            .beanClass(ManagedExecutorServiceAdapter.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return register(beans, produce(beans, ManagedExecutorConfig.DFLT_INST),
                    ManagedExecutorConfig.DFLT_INST);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            });
      } else {
        executorConfigs.getAllWithQualifiers()
            .forEach((cfg, esn) -> event.<ManagedExecutorService>addBean().addQualifiers(esn)
                .addTransitiveTypeClosure(ManagedExecutorService.class)
                .addTransitiveTypeClosure(ExecutorService.class)
                .beanClass(ManagedExecutorServiceAdapter.class).scope(ApplicationScoped.class)
                .produceWith(beans -> {
                  try {
                    return register(beans, produce(beans, cfg), cfg);
                  } catch (NamingException e) {
                    throw new CorantRuntimeException(e);
                  }
                }));
      }

      if (contextServiceConfigs.isEmpty()) {
        event.<ContextService>addBean().addTransitiveTypeClosure(ContextService.class)
            .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE)
            .beanClass(DefaultContextService.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return produce(beans, ContextServiceConfig.DFLT_INST);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            });
      }

      scheduledExecutorConfigs.getAllWithQualifiers()
          .forEach((cfg, esn) -> event.<ManagedScheduledExecutorService>addBean().addQualifiers(esn)
              .addTransitiveTypeClosure(ScheduledExecutorService.class)
              .addTransitiveTypeClosure(ManagedScheduledExecutorService.class)
              .beanClass(ManagedScheduledExecutorServiceAdapter.class)
              .scope(ApplicationScoped.class).produceWith(beans -> {
                try {
                  return register(beans, produce(beans, cfg), cfg);
                } catch (NamingException e) {
                  throw new CorantRuntimeException(e);
                }
              }));
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    executorConfigs = new DefaultNamedQualifierObjectManager<>(
        ConfigInstances.resolveMulti(ManagedExecutorConfig.class).values());
    if (executorConfigs.isEmpty()) {
      logger.info(() -> "Use default managed executor config.");
    } else {
      logger.fine(() -> String.format("Found %s managed executor configs named [%s].",
          executorConfigs.size(), String.join(", ", executorConfigs.getAllDisplayNames())));
    }
    scheduledExecutorConfigs = new DefaultNamedQualifierObjectManager<>(
        ConfigInstances.resolveMulti(ManagedScheduledExecutorConfig.class).values());
    if (!scheduledExecutorConfigs.isEmpty()) {
      logger.fine(() -> String.format("Found %s managed scheduled executor configs named [%s].",
          scheduledExecutorConfigs.size(),
          String.join(", ", scheduledExecutorConfigs.getAllDisplayNames())));
    }
    contextServiceConfigs = new DefaultNamedQualifierObjectManager<>(
        ConfigInstances.resolveMulti(ContextServiceConfig.class).values());
    if (contextServiceConfigs.isEmpty()) {
      logger.info(() -> "Use default context service config.");
    } else {
      logger.fine(() -> String.format("Found %s context service configs named [%s].",
          contextServiceConfigs.size(),
          String.join(", ", contextServiceConfigs.getAllDisplayNames())));
    }
  }

  protected DefaultContextService produce(Instance<Object> instance, ContextServiceConfig cfg)
      throws NamingException {
    TransactionManager tm = resolveTransactionManager(instance);
    logger.fine(() -> String.format("Create context service %s with %s.", cfg.getName(), cfg));
    DefaultContextService service = new DefaultContextService(cfg.getName(),
        new ContextSetupProviderImpl(cfg.getContextInfos().toArray(ContextInfo[]::new)),
        new TransactionSetupProviderImpl(tm));
    if (cfg.isEnableJndi() && isNotBlank(cfg.getName())) {
      // TODO register to JNDI
    }
    return service;
  }

  protected DefaultManagedExecutorService produce(Instance<Object> instance,
      ManagedExecutorConfig cfg) throws NamingException {
    DefaultManagedThreadFactory mtf = new DefaultManagedThreadFactory(cfg.getName());
    String name = cfg.getName();
    Instance<BlockingQueueProvider> ques =
        instance.select(BlockingQueueProvider.class, NamedLiteral.of(name));
    TransactionManager tm = resolveTransactionManager(instance);
    DefaultContextService contextService = new DefaultContextService(cfg.getName(),
        new ContextSetupProviderImpl(cfg.getContextInfos()), new TransactionSetupProviderImpl(tm));

    if (ques.isResolvable()) {
      logger.fine(
          () -> String.format("Create managed executor service %s with customer blocking queue %s.",
              cfg.getName(), cfg));
      return new DefaultManagedExecutorService(cfg.getName(), mtf, cfg.getHungTaskThreshold(),
          cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getMaxPoolSize(),
          cfg.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
          cfg.getThreadLifeTime().toMillis(), cfg.getAwaitTermination(), contextService,
          cfg.getRejectPolicy(), cfg.getRetryDelay(), ques.get().provide(cfg));
    } else {
      logger.fine(
          () -> String.format("Create managed executor service %s with %s.", cfg.getName(), cfg));
      return new DefaultManagedExecutorService(cfg.getName(), mtf, cfg.getHungTaskThreshold(),
          cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getMaxPoolSize(),
          cfg.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
          cfg.getThreadLifeTime().toMillis(), cfg.getAwaitTermination(), cfg.getQueueCapacity(),
          contextService, cfg.getRejectPolicy(), cfg.getRetryDelay());
    }
  }

  protected DefaultManagedScheduledExecutorService produce(Instance<Object> instance,
      ManagedScheduledExecutorConfig cfg) throws NamingException {
    DefaultContextService contextService = new DefaultContextService(cfg.getName(),
        new ContextSetupProviderImpl(cfg.getContextInfos()),
        new TransactionSetupProviderImpl(instance.select(TransactionManager.class).get()));
    logger.fine(() -> String.format("Create managed scheduled executor service %s with %s.",
        cfg.getName(), cfg));
    return new DefaultManagedScheduledExecutorService(cfg.getName(),
        new DefaultManagedThreadFactory(cfg.getName()), cfg.getHungTaskThreshold(),
        cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getKeepAliveTime().toMillis(),
        TimeUnit.MILLISECONDS, cfg.getThreadLifeTime().toMillis(), cfg.getAwaitTermination(),
        contextService, cfg.getRejectPolicy(), cfg.getRetryDelay());
  }

  protected ManagedExecutorServiceAdapter register(Instance<Object> instance,
      DefaultManagedExecutorService service, ManagedExecutorConfig cfg) {
    instance.select(ExecutorServiceManager.class).get().register(service);
    if (cfg.isEnableJndi() && isNotBlank(cfg.getName())) {
      // TODO register to JNDI
    }
    return service.getAdapter();
  }

  protected ManagedScheduledExecutorServiceAdapter register(Instance<Object> instance,
      DefaultManagedScheduledExecutorService service, ManagedScheduledExecutorConfig cfg) {
    instance.select(ExecutorServiceManager.class).get().register(service);
    if (cfg.isEnableJndi() && isNotBlank(cfg.getName())) {
      // TODO register to JNDI
    }
    return service.getAdapter();
  }

  protected TransactionManager resolveTransactionManager(Instance<Object> instance) {
    Instance<TransactionManager> it;
    if ((it = instance.select(TransactionManager.class)).isResolvable()) {
      return it.get();
    }
    return null;
  }

}
