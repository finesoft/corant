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

import static org.corant.kernel.util.Instances.select;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import org.corant.suites.ddd.message.AggregationLifecycleMessage;
import org.corant.suites.ddd.model.Aggregation.Lifecycle;
import org.corant.suites.ddd.unitwork.UnitOfWorksManager;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午3:35:44
 *
 */
public class DefaultAggregationListener {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

  protected void handlePostLoad(AbstractAggregation o) {
    o.lifecycle(Lifecycle.LOADED).callAssistant().clearMessages();
    registerToUnitOfWork(o);
  }

  protected void handlePostPersist(AbstractAggregation o) {
    registerToUnitOfWork(new AggregationLifecycleMessage(o, Lifecycle.POST_PERSISTED));
    registerToUnitOfWork(o.lifecycle(Lifecycle.POST_PERSISTED));
  }

  protected void handlePostRemove(AbstractAggregation o) {
    registerToUnitOfWork(new AggregationLifecycleMessage(o, Lifecycle.POST_REMOVED));
    registerToUnitOfWork(o.lifecycle(Lifecycle.POST_REMOVED));
  }

  protected void handlePostUpdate(AbstractAggregation o) {
    registerToUnitOfWork(new AggregationLifecycleMessage(o, Lifecycle.POST_UPDATED));
    registerToUnitOfWork(o.lifecycle(Lifecycle.POST_UPDATED));
  }

  protected void handlePrePersist(AbstractAggregation o) {
    o.preEnable();
    registerToUnitOfWork(o.lifecycle(Lifecycle.PRE_PERSIST));
  }

  protected void handlePreRemove(AbstractAggregation o) {
    o.preDisable();
    registerToUnitOfWork(o.lifecycle(Lifecycle.PRE_REMOVE));
  }

  protected void handlePreUpdate(AbstractAggregation o) {
    o.preEnable();
    registerToUnitOfWork(o.lifecycle(Lifecycle.PRE_UPDATE));
  }

  @PostLoad
  protected void onPostLoad(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePostLoad((AbstractAggregation) o);
    }
  }

  @PostPersist
  protected void onPostPersist(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePostPersist((AbstractAggregation) o);
    }
  }

  @PostRemove
  protected void onPostRemove(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePostRemove((AbstractAggregation) o);
    }
  }

  @PostUpdate
  protected void onPostUpdate(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePostUpdate((AbstractAggregation) o);
    }
  }

  @PrePersist
  protected void onPrePersist(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePrePersist((AbstractAggregation) o);
    }
  }

  @PreRemove
  protected void onPreRemove(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePreRemove((AbstractAggregation) o);
    }
  }

  @PreUpdate
  protected void onPreUpdate(Object o) {
    if (o instanceof AbstractAggregation) {
      handlePreUpdate((AbstractAggregation) o);
    }
  }

  protected void registerToUnitOfWork(Object o) {
    Instance<UnitOfWorksManager> um = select(UnitOfWorksManager.class);
    if (um.isResolvable()) {
      um.get().getCurrentUnitOfWork().register(o);
    } else {
      logger.warning(() -> "UnitOfWorksService not found! please check the implements!");
    }
  }
}
