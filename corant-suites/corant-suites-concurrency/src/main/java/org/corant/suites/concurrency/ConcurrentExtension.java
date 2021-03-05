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
package org.corant.suites.concurrency;

import static org.corant.shared.util.Empties.isEmpty;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
import org.corant.config.declarative.ConfigInstances;
import org.corant.context.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.context.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.suites.concurrency.provider.BlockingQueueProvider;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午2:51:48
 *
 */
public class ConcurrentExtension implements Extension {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  protected volatile NamedQualifierObjectManager<ManagedExecutorConfig> executorConfigs =
      NamedQualifierObjectManager.empty();
  protected volatile NamedQualifierObjectManager<ManagedScheduledExecutorConfig> scheduledExecutorConfigs =
      NamedQualifierObjectManager.empty();

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      if (isEmpty(executorConfigs)) {
        event.<ManagedExecutorService>addBean().addTransitiveTypeClosure(ExecutorService.class)
            .beanClass(DefaultManagedExecutorService.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                ManagedExecutorConfig cfg = new ManagedExecutorConfig();
                cfg.setName(Names.CORANT);
                return produce(beans, cfg);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((exe, beans) -> exe.shutdown());
      } else {
        executorConfigs.getAllWithQualifiers().forEach((cfg, esn) -> {
          event.<ManagedExecutorService>addBean().addQualifiers(esn)
              .addTransitiveTypeClosure(ExecutorService.class)
              .beanClass(DefaultManagedExecutorService.class).scope(ApplicationScoped.class)
              .produceWith(beans -> {
                try {
                  return produce(beans, cfg);
                } catch (NamingException e) {
                  throw new CorantRuntimeException(e);
                }
              }).disposeWith((exe, beans) -> exe.shutdown());
        });
      }
      scheduledExecutorConfigs.getAllWithQualifiers().forEach((cfg, esn) -> {
        event.<ManagedScheduledExecutorService>addBean().addQualifiers(esn)
            .addTransitiveTypeClosure(ScheduledExecutorService.class)
            .beanClass(DefaultManagedScheduledExecutorService.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return produce(beans, cfg);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((exe, beans) -> exe.shutdown());
      });
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    executorConfigs = new DefaultNamedQualifierObjectManager<>(
        ConfigInstances.resolveMulti(ManagedExecutorConfig.class).values());
    if (executorConfigs.isEmpty()) {
      logger.info(() -> "Can not find any managed executor configurations.");
    } else {
      logger.fine(() -> String.format("Find %s managed executor named [%s].",
          executorConfigs.size(), String.join(", ", executorConfigs.getAllDisplayNames())));
    }
    scheduledExecutorConfigs = new DefaultNamedQualifierObjectManager<>(
        ConfigInstances.resolveMulti(ManagedScheduledExecutorConfig.class).values());
    if (scheduledExecutorConfigs.isEmpty()) {
      logger.info(() -> "Can not find any managed scheduled executor configurations.");
    } else {
      logger.fine(() -> String.format("Find %s managed scheduled executor named [%s].",
          scheduledExecutorConfigs.size(),
          String.join(", ", scheduledExecutorConfigs.getAllDisplayNames())));
    }
  }

  protected DefaultManagedExecutorService produce(Instance<Object> instance,
      ManagedExecutorConfig cfg) throws NamingException {
    ManagedThreadFactoryImpl mtf = new DefaultManagedThreadFactory(cfg.getThreadName());
    String name = cfg.getName();
    Instance<BlockingQueueProvider> ques =
        instance.select(BlockingQueueProvider.class, NamedLiteral.of(name));
    if (ques.isResolvable()) {
      return new DefaultManagedExecutorService(cfg.getName(), mtf, cfg.getHungTaskThreshold(),
          cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getMaxPoolSize(),
          cfg.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
          cfg.getThreadLifeTime().toMillis(), null, cfg.getRejectPolicy(), ques.get().provide(cfg));
    } else {
      return new DefaultManagedExecutorService(cfg.getName(), mtf, cfg.getHungTaskThreshold(),
          cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getMaxPoolSize(),
          cfg.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
          cfg.getThreadLifeTime().toMillis(), cfg.getQueueCapacity(), null, cfg.getRejectPolicy());
    }
  }

  protected DefaultManagedScheduledExecutorService produce(Instance<Object> instance,
      ManagedScheduledExecutorConfig cfg) throws NamingException {
    return new DefaultManagedScheduledExecutorService(cfg.getName(),
        new DefaultManagedThreadFactory(cfg.getThreadName()), cfg.getHungTaskThreshold(),
        cfg.isLongRunningTasks(), cfg.getCorePoolSize(), cfg.getKeepAliveTime().toMillis(),
        TimeUnit.MILLISECONDS, cfg.getThreadLifeTime().toMillis(), null, cfg.getRejectPolicy());
  }
}
