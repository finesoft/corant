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
package org.corant.suites.ddd.model;

import static org.corant.context.Instances.resolve;
import java.util.Optional;
import java.util.logging.Logger;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;
import org.corant.suites.ddd.unitwork.UnitOfWork;
import org.corant.suites.ddd.unitwork.UnitOfWorks;

/**
 * corant-suites-ddd
 *
 * <p>
 * Entity listener, working with the {@link UnitOfWork}. This class is annotated on
 * {@link #AbstractAggregate}, if you want to rewrite it use {@link ExcludeDefaultListeners}
 * <p>
 *
 * @author bingo 下午3:35:44
 *
 */
public class DefaultAggregateListener {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

  protected void handlePostLoad(AbstractAggregate o) {
    o.setLifecycle(Lifecycle.LOADED).callAssistant().clearMessages();
    registerToUnitOfWork(o);
  }

  protected void handlePostPersist(AbstractAggregate o) {
    o.buildLifecycleMessage(Lifecycle.POST_PERSISTED).ifPresent(this::registerToUnitOfWork);
    registerToUnitOfWork(o.setLifecycle(Lifecycle.POST_PERSISTED));
  }

  protected void handlePostRemove(AbstractAggregate o) {
    o.buildLifecycleMessage(Lifecycle.POST_REMOVED).ifPresent(this::registerToUnitOfWork);
    registerToUnitOfWork(o.setLifecycle(Lifecycle.POST_REMOVED));
  }

  protected void handlePostUpdate(AbstractAggregate o) {
    o.buildLifecycleMessage(Lifecycle.POST_UPDATED).ifPresent(this::registerToUnitOfWork);
    registerToUnitOfWork(o.setLifecycle(Lifecycle.POST_UPDATED));
  }

  protected void handlePrePersist(AbstractAggregate o) {
    o.onPrePreserve();
    registerToUnitOfWork(o.setLifecycle(Lifecycle.PRE_PERSIST));
  }

  protected void handlePreRemove(AbstractAggregate o) {
    o.onPreDestroy();
    registerToUnitOfWork(o.setLifecycle(Lifecycle.PRE_REMOVE));
  }

  protected void handlePreUpdate(AbstractAggregate o) {
    o.onPrePreserve();
    registerToUnitOfWork(o.setLifecycle(Lifecycle.PRE_UPDATE));
  }

  @PostLoad
  protected void onPostLoad(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePostLoad((AbstractAggregate) o);
    }
  }

  @PostPersist
  protected void onPostPersist(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePostPersist((AbstractAggregate) o);
    }
  }

  @PostRemove
  protected void onPostRemove(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePostRemove((AbstractAggregate) o);
    }
  }

  @PostUpdate
  protected void onPostUpdate(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePostUpdate((AbstractAggregate) o);
    }
  }

  @PrePersist
  protected void onPrePersist(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePrePersist((AbstractAggregate) o);
    }
  }

  @PreRemove
  protected void onPreRemove(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePreRemove((AbstractAggregate) o);
    }
  }

  @PreUpdate
  protected void onPreUpdate(Object o) {
    if (o instanceof AbstractAggregate) {
      handlePreUpdate((AbstractAggregate) o);
    }
  }

  protected void registerToUnitOfWork(Object o) {
    Optional<? extends UnitOfWork> uow = resolve(UnitOfWorks.class).currentDefaultUnitOfWork();
    if (uow.isPresent()) {
      uow.get().register(o);
    } else {
      logger.warning(() -> "The UnitOfWork not found! please check the implements!");
    }
  }
}
