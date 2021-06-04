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
package org.corant.modules.ddd.shared.unitwork;

import static org.corant.context.Beans.find;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import org.corant.context.CDIs;
import org.corant.modules.ddd.Event;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.UnitOfWork;
import org.corant.modules.ddd.UnitOfWorksManager;
import org.corant.modules.ddd.annotation.InfrastructureServices;
import org.corant.modules.ddd.shared.annotation.JTARL;
import org.corant.modules.ddd.shared.annotation.JTAXA;
import org.corant.modules.jta.shared.SynchronizationAdapter;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * Used to provide the current unit of work manager or unit of work, If you do not apply the
 * existing mechanism, return to the custom unit of work instance by overriding this class.
 * </p>
 *
 * @author bingo 12:17:01
 *
 */
@ApplicationScoped
@InfrastructureServices
public class UnitOfWorks {

  @Inject
  @ConfigProperty(name = "corant.ddd.unitofwork.use-xa", defaultValue = "true")
  protected boolean useJtaXa;

  public Optional<AbstractJTAJPAUnitOfWork> currentDefaultUnitOfWork() {
    Optional<AbstractJTAJPAUnitOfWorksManager> uowm = currentDefaultUnitOfWorksManager();
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public Optional<AbstractJTAJPAUnitOfWorksManager> currentDefaultUnitOfWorksManager() {
    return find(AbstractJTAJPAUnitOfWorksManager.class, useJtaXa ? JTAXA.INSTANCE : JTARL.INSTANCE);
  }

  public Optional<UnitOfWork> currentUnitOfWork(Annotation... qualifiers) {
    Optional<UnitOfWorksManager> uowm = currentUnitOfWorksManager(qualifiers);
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public Optional<UnitOfWorksManager> currentUnitOfWorksManager(Annotation... qualifiers) {
    return find(UnitOfWorksManager.class, qualifiers);
  }

  public void deregisterMessage(Message... messages) {
    if (messages.length > 0) {
      AbstractJTAJPAUnitOfWork uow = curUow();
      for (Message message : messages) {
        uow.deregister(message);
      }
    }
  }

  public void deregisterVariable(Object key, Object value) {
    curUow().deregister(Pair.of(key, value));
  }

  public <U extends Event> CompletionStage<U> fireAsyncEvent(U event, Annotation... qualifiers) {
    shouldNotNull(event);
    return CDIs.fireAsyncEvent(event, qualifiers);
  }

  public void fireEvent(Event event, Annotation... qualifiers) {
    shouldNotNull(event);
    CDIs.fireEvent(event, qualifiers);
  }

  public int getTxStatus() {
    try {
      return curUow().transaction.getStatus();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public boolean isUseJtaXa() {
    return useJtaXa;
  }

  public void makeTxRollbackOnly() {
    try {
      curUow().transaction.setRollbackOnly();
    } catch (IllegalStateException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public void registerAfterCompletion(final Consumer<Boolean> consumer) {
    if (consumer != null) {
      registerTxSynchronization(new SynchronizationAdapter() {
        @Override
        public void afterCompletion(int status) {
          consumer.accept(status == Status.STATUS_COMMITTED);
        }
      });
    }
  }

  public void registerBeforeCompletion(final Runnable runner) {
    if (runner != null) {
      registerTxSynchronization(new SynchronizationAdapter() {
        @Override
        public void beforeCompletion() {
          runner.run();
        }
      });
    }
  }

  public void registerMessage(Message... messages) {
    if (messages.length > 0) {
      AbstractJTAJPAUnitOfWork uow = curUow();
      for (Message message : messages) {
        uow.register(message);
      }
    }
  }

  public void registerTxSynchronization(Synchronization sync) {
    try {
      curUow().transaction.registerSynchronization(sync);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public void registerVariable(Object key, Object value) {
    curUow().register(Pair.of(key, value));
  }

  protected AbstractJTAJPAUnitOfWork curUow() {
    return currentDefaultUnitOfWork()
        .orElseThrow(() -> new CorantRuntimeException("Can't find any unit of works"));
  }
}
