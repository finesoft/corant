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
package org.corant.modules.ddd.shared.unitwork;

import static org.corant.shared.util.Sets.newConcurrentHashSet;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.annotation.AggregateType;
import org.corant.modules.ddd.shared.event.AggregateEvolutionaryEvent;
import org.corant.modules.ddd.shared.event.AggregatePersistedEvent;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 上午12:37:58
 *
 */
public class UnitOfWorkExtension implements Extension {

  protected final Set<Class<?>> observedEvolutionaryAggregateClasses = newConcurrentHashSet();
  protected final Set<Class<?>> observedPersistedAggregateClasses = newConcurrentHashSet();
  protected volatile boolean observeAllAggregateEvolutionary = false;
  protected volatile boolean observeAllAggregatePersisted = false;

  public boolean supportsEvolutionaryObserver(Class<?> cls) {
    return cls != null
        && (observeAllAggregateEvolutionary || observedEvolutionaryAggregateClasses.contains(cls));
  }

  public boolean supportsPersistedObserver(Class<?> cls) {
    return cls != null
        && (observeAllAggregatePersisted || observedPersistedAggregateClasses.contains(cls));
  }

  protected Class<? extends Aggregate> getQualifier(ObserverMethod<?> observerMethod) {
    for (Annotation qualifier : observerMethod.getObservedQualifiers()) {
      if (qualifier.annotationType().equals(AggregateType.class)) {
        return ((AggregateType) qualifier).value();
      }
    }
    return null;
  }

  protected synchronized void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    observedEvolutionaryAggregateClasses.clear();
    observedPersistedAggregateClasses.clear();
    observeAllAggregateEvolutionary = false;
    observeAllAggregatePersisted = false;
  }

  protected synchronized void onProcessObserveAggregateEvolutionaryEventMethod(
      @Observes ProcessObserverMethod<AggregateEvolutionaryEvent, ?> e) {
    if (!observeAllAggregateEvolutionary) {
      Class<? extends Aggregate> cls = getQualifier(e.getObserverMethod());
      if (cls == null) {
        observeAllAggregateEvolutionary = true;
        observedEvolutionaryAggregateClasses.clear();
      } else {
        observedEvolutionaryAggregateClasses.add(cls);
      }
    }
  }

  protected synchronized void onProcessObserveAggregatePersistedEventMethod(
      @Observes ProcessObserverMethod<AggregatePersistedEvent, ?> e) {
    if (!observeAllAggregatePersisted) {
      Class<? extends Aggregate> cls = getQualifier(e.getObserverMethod());
      if (cls == null) {
        observeAllAggregatePersisted = true;
        observedPersistedAggregateClasses.clear();
      } else {
        observedPersistedAggregateClasses.add(cls);
      }
    }
  }
}
