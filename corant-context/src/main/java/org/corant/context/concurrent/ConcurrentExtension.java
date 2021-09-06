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

import static org.corant.shared.util.Lists.newArrayList;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import org.corant.config.Configs;
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
  public static final boolean ENABLE_DFLT_MES = Configs
      .getValue("corant.concurrent.enable-default-managed-executor", Boolean.class, Boolean.TRUE);
  public static final boolean ENABLE_DFLT_MSES = Configs.getValue(
      "corant.concurrent.enable-default-managed-scheduled-executor", Boolean.class, Boolean.TRUE);
  public static final boolean ENABLE_DFLT_CS = Configs
      .getValue("corant.concurrent.enable-default-context-service", Boolean.class, Boolean.TRUE);
  public static final boolean ENABLE_HUNG_TASK_LOGGER =
      Configs.getValue("corant.concurrent.enable-hung-task-logger", Boolean.class, Boolean.FALSE);

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  protected volatile NamedQualifierObjectManager<ManagedExecutorConfig> executorConfigs =
      NamedQualifierObjectManager.empty();
  protected volatile NamedQualifierObjectManager<ManagedScheduledExecutorConfig> scheduledExecutorConfigs =
      NamedQualifierObjectManager.empty();
  protected volatile NamedQualifierObjectManager<ContextServiceConfig> contextServiceConfigs =
      NamedQualifierObjectManager.empty();

  public NamedQualifierObjectManager<ManagedExecutorConfig> getExecutorConfigs() {
    return executorConfigs;
  }

  public NamedQualifierObjectManager<ManagedScheduledExecutorConfig> getScheduledExecutorConfigs() {
    return scheduledExecutorConfigs;
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    // Don't use transitive type closure, the CDI can't differentiate, since the
    // ScheduledExecutorService extends ExecutorService
    if (event != null) {
      executorConfigs.getAllWithQualifiers().forEach((cfg, esn) -> {
        event.<ManagedExecutorService>addBean().addQualifiers(esn)
            .addType(ManagedExecutorService.class).addType(ExecutorService.class)
            .beanClass(ManagedExecutorServiceAdapter.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return register(beans, produce(beans, cfg), cfg);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            });
        logger.info(() -> String.format("Resolved managed executor %s %s", cfg.getName(), cfg));
      });

      contextServiceConfigs.getAllWithQualifiers().forEach((cfg, esn) -> {
        event.<ContextService>addBean().addTransitiveTypeClosure(ContextService.class)
            .addQualifiers(esn).beanClass(DefaultContextService.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              try {
                return produce(beans, cfg);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            });
        logger.info(() -> String.format("Resolved context service %s %s", cfg.getName(), cfg));
      });

      // TODO FIXME, since the ManagedScheduledExecutorService extends ManagedExecutorService
      scheduledExecutorConfigs.getAllWithQualifiers().forEach((cfg, esn) -> {
        event.<ManagedScheduledExecutorService>addBean().addQualifiers(esn)
            .addType(ScheduledExecutorService.class).addType(ManagedScheduledExecutorService.class)
            .beanClass(ManagedScheduledExecutorServiceAdapter.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return register(beans, produce(beans, cfg), cfg);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            });
        logger.info(
            () -> String.format("Resolved managed scheduled executor %s %s", cfg.getName(), cfg));
      });
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    Collection<ManagedExecutorConfig> mecs =
        newArrayList(ConfigInstances.resolveMulti(ManagedExecutorConfig.class).values());
    if (mecs.isEmpty() && ENABLE_DFLT_MES) {
      logger.info(() -> String.format("Use default managed executor configuration %s",
          ManagedExecutorConfig.DFLT_INST));
      mecs.add(ManagedExecutorConfig.DFLT_INST);
    }

    Collection<ManagedScheduledExecutorConfig> msecs =
        newArrayList(ConfigInstances.resolveMulti(ManagedScheduledExecutorConfig.class).values());
    if (msecs.isEmpty() && ENABLE_DFLT_MSES) {
      logger.info(() -> String.format("Use default managed scheduled executor configuration %s",
          ManagedScheduledExecutorConfig.DFLT_INST));
      msecs.add(ManagedScheduledExecutorConfig.DFLT_INST);
    }

    Collection<ContextServiceConfig> cscs =
        newArrayList(ConfigInstances.resolveMulti(ContextServiceConfig.class).values());
    if (cscs.isEmpty() && ENABLE_DFLT_CS) {
      logger.info(() -> String.format("Use default context service configuration %s",
          ContextServiceConfig.DFLT_INST));
      cscs.add(ContextServiceConfig.DFLT_INST);
    }

    executorConfigs = new DefaultNamedQualifierObjectManager<>(mecs);
    scheduledExecutorConfigs = new DefaultNamedQualifierObjectManager<>(msecs);
    contextServiceConfigs = new DefaultNamedQualifierObjectManager<>(cscs);
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
    DefaultManagedThreadFactory mtf = new DefaultManagedThreadFactory(cfg.getThreadName());
    TransactionManager tm = resolveTransactionManager(instance);
    DefaultContextService contextService = new DefaultContextService(cfg.getName(),
        new ContextSetupProviderImpl(cfg.getContextInfos()), new TransactionSetupProviderImpl(tm));
    Instance<BlockingQueueProvider> ques =
        instance.select(BlockingQueueProvider.class, NamedLiteral.of(cfg.getName()));
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
        new DefaultManagedThreadFactory(cfg.getThreadName()), cfg.getHungTaskThreshold(),
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
