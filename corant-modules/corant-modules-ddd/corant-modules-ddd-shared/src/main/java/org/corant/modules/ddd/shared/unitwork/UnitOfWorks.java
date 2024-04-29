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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Configurations.getConfigValue;
import java.lang.annotation.Annotation;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import org.corant.context.CDIs;
import org.corant.modules.ddd.Event;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.annotation.InfrastructureServices;
import org.corant.modules.ddd.shared.annotation.JTARL;
import org.corant.modules.ddd.shared.annotation.JTAXA;
import org.corant.modules.jta.shared.SynchronizationAdapter;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * Used to provide the current unit of work manager or unit of work, If you do not apply the
 * existing mechanism, return to the custom unit of work instance by overriding this class.
 * </p>
 *
 * @author bingo 12:17:01
 */
@ApplicationScoped
@InfrastructureServices
public class UnitOfWorks {

  protected static final boolean useJtaXa =
      getConfigValue("corant.ddd.unitofwork.use-xa", Boolean.class, true);

  @Inject
  @Any
  protected Instance<AbstractJTAJPAUnitOfWorksManager> managers;

  public AbstractJTAJPAUnitOfWork currentDefaultUnitOfWork() {
    return currentDefaultUnitOfWorksManager().getCurrentUnitOfWork();
  }

  public AbstractJTAJPAUnitOfWorksManager currentDefaultUnitOfWorksManager() {
    if (useJtaXa) {
      return managers.select(JTAXA.INSTANCE).get();
    } else {
      return managers.select(JTARL.INSTANCE).get();
    }
  }

  public void deregisterMessage(Message... messages) {
    if (messages.length > 0) {
      AbstractJTAJPAUnitOfWork uow = currentDefaultUnitOfWork();
      for (Message message : messages) {
        uow.deregister(message);
      }
    }
  }

  public void deregisterVariable(Object key, Object value) {
    currentDefaultUnitOfWork().deregister(Pair.of(key, value));
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
      return currentDefaultUnitOfWork().transaction.getStatus();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public boolean isUseJtaXa() {
    return useJtaXa;
  }

  public void makeTxRollbackOnly() {
    try {
      currentDefaultUnitOfWork().transaction.setRollbackOnly();
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
      AbstractJTAJPAUnitOfWork uow = currentDefaultUnitOfWork();
      for (Message message : messages) {
        uow.register(message);
      }
    }
  }

  public void registerTxSynchronization(Synchronization sync) {
    try {
      currentDefaultUnitOfWork().transaction.registerSynchronization(sync);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public void registerVariable(Object key, Object value) {
    currentDefaultUnitOfWork().register(Pair.of(key, value));
  }

}
