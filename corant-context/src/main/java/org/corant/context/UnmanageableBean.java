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
package org.corant.context;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-context
 *
 * Handle CDI unmanageable bean class or object
 *
 * @author bingo 下午11:02:59
 *
 */
public class UnmanageableBean<T> implements AutoCloseable {

  private T instance;
  private final CreationalContext<T> creationalContext;
  private final WeldInjectionTarget<T> injectionTarget;
  private final AnnotatedType<T> annotatedType;
  private final T originalInstance;
  private final WeldManager bm;
  private boolean disposed = false;

  public UnmanageableBean(Class<T> clazz) {
    bm = (WeldManager) CDI.current().getBeanManager();
    creationalContext = bm.createCreationalContext(null);
    annotatedType = bm.createAnnotatedType(clazz);
    injectionTarget = bm.getInjectionTargetFactory(annotatedType).createInjectionTarget(null);
    originalInstance = null;
  }

  @SuppressWarnings("deprecation")
  public UnmanageableBean(T object) {
    shouldBeFalse(Beans.isManagedBean(shouldNotNull(object)));
    bm = (WeldManager) CDI.current().getBeanManager();
    creationalContext = bm.createCreationalContext(null);
    annotatedType = bm.createAnnotatedType(forceCast(object.getClass()));
    injectionTarget =
        bm.getInjectionTargetFactory(annotatedType).createNonProducibleInjectionTarget();
    originalInstance = object;
  }

  public static <T> UnmanageableBean<T> of(Class<T> clazz) {
    return new UnmanageableBean<>(clazz);
  }

  public static <T> UnmanageableBean<T> of(T object) {
    return new UnmanageableBean<>(object);
  }

  @Override
  public void close() {
    preDestroy();
    dispose();
  }

  /**
   * Dispose of the instance, doing any necessary cleanup
   *
   * @throws IllegalStateException if dispose() is called before produce() is called
   * @throws IllegalStateException if dispose() is called on an instance that has already been
   *         disposed
   * @return self
   */
  public UnmanageableBean<T> dispose() {
    if (instance == null) {
      throw new IllegalStateException("Trying to call dispose() before produce() was called");
    }
    if (disposed) {
      throw new IllegalStateException("Trying to call dispose() on already disposed instance");
    }
    disposed = true;
    injectionTarget.dispose(instance);
    creationalContext.release();
    return this;
  }

  /**
   * Get the instance
   *
   * @return the instance
   */
  public T get() {
    return instance;
  }

  /**
   * Inject the instance
   *
   * @throws IllegalStateException if inject() is called before produce() is called
   * @throws IllegalStateException if inject() is called on an instance that has already been
   *         disposed
   * @return self
   */
  public UnmanageableBean<T> inject() {
    if (instance == null) {
      throw new IllegalStateException("Trying to call inject() before produce() was called");
    }
    if (disposed) {
      throw new IllegalStateException("Trying to call inject() on already disposed instance");
    }
    injectionTarget.inject(instance, creationalContext);
    return this;
  }

  /**
   * Call the @PostConstruct callback
   *
   * @throws IllegalStateException if postConstruct() is called before produce() is called
   * @throws IllegalStateException if postConstruct() is called on an instance that has already been
   *         disposed
   * @return self
   */
  public UnmanageableBean<T> postConstruct() {
    if (instance == null) {
      throw new IllegalStateException("Trying to call postConstruct() before produce() was called");
    }
    if (disposed) {
      throw new IllegalStateException(
          "Trying to call postConstruct() on already disposed instance");
    }
    injectionTarget.postConstruct(instance);
    return this;
  }

  /**
   * Call the @PreDestroy callback
   *
   * @throws IllegalStateException if preDestroy() is called before produce() is called
   * @throws IllegalStateException if preDestroy() is called on an instance that has already been
   *         disposed
   * @return self
   */
  public UnmanageableBean<T> preDestroy() {
    if (instance == null) {
      throw new IllegalStateException("Trying to call preDestroy() before produce() was called");
    }
    if (disposed) {
      throw new IllegalStateException("Trying to call preDestroy() on already disposed instance");
    }
    injectionTarget.preDestroy(instance);
    return this;
  }

  /**
   * Create the instance
   *
   * @throws IllegalStateException if produce() is called on an already produced instance
   * @throws IllegalStateException if produce() is called on an instance that has already been
   *         disposed
   * @return self
   */
  public UnmanageableBean<T> produce() {
    if (instance != null) {
      throw new IllegalStateException("Trying to call produce() on already constructed instance");
    }
    if (disposed) {
      throw new IllegalStateException("Trying to call produce() on an already disposed instance");
    }
    instance =
        originalInstance == null ? injectionTarget.produce(creationalContext) : originalInstance;
    // InterceptionFactoryImpl.of(BeanManagerProxy.unwrap(bm), creationalContext, annotatedType)
    // .ignoreFinalMethods().createInterceptedInstance(
    // originalInstance == null ? injectionTarget.produce(creationalContext)
    // : originalInstance);
    return this;
  }

}
