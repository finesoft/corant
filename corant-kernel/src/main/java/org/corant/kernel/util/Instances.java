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
package org.corant.kernel.util;

import static org.corant.Corant.instance;
import static org.corant.kernel.util.Qualifiers.resolveNameds;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.asClass;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.injection.InterceptionFactoryImpl;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-kernel
 *
 * @author bingo 下午2:22:40
 *
 */
public class Instances {

  public static boolean isManagedBean(Object object, Annotation... qualifiers) {
    return object != null && !instance().select(getUserClass(object), qualifiers).isUnsatisfied();
  }

  public static <T> Optional<T> resolve(Class<T> instanceClass, Annotation... qualifiers) {
    Class<T> instCls = shouldNotNull(instanceClass);
    if (instance().select(instCls, qualifiers).isResolvable()) {
      return Optional.of(instance().select(instCls).get());
    } else {
      return Optional.empty();
    }
  }

  public static <T> void resolveAccept(Class<T> instanceClass, Consumer<T> consumer) {
    shouldNotNull(consumer).accept(resolve(instanceClass).orElseThrow(
        () -> new CorantRuntimeException("Can not resolve bean class %s", instanceClass)));
  }

  public static <T> T resolveAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Class<T> instCls = shouldNotNull(instanceClass);
    if (instance().select(instCls, qualifiers).isResolvable()) {
      return instance().select(instCls).get();
    } else if (instance().select(instCls).isUnsatisfied()) {
      return UnmanageableInstance.of(instCls).produce().inject().postConstruct().get();
    } else {
      List<T> list = listOf(ServiceLoader.load(instanceClass, defaultClassLoader()));
      return list != null && list.size() == 1 ? list.get(0) : null;
    }
  }

  public static <T> T resolveAnyway(T obj, Annotation... qualifiers) {
    if (isManagedBean(obj, qualifiers)) {
      return obj;
    } else if (obj != null) {
      return UnmanageableInstance.of(obj).produce().inject().postConstruct().get();
    }
    return null;
  }

  public static <T, R> R resolveApply(Class<T> instanceClass, Function<T, R> function) {
    return shouldNotNull(function).apply(resolve(instanceClass).orElseThrow(
        () -> new CorantRuntimeException("Can not resolve bean class %s", instanceClass)));
  }

  public static <T> Optional<T> resolveNamed(Class<T> instanceClass, String name) {
    Class<T> instCls = shouldNotNull(instanceClass);
    Instance<T> inst = instance().select(instCls);
    if (inst.isUnsatisfied()) {
      return Optional.empty();
    }
    String useName = defaultTrim(name);
    if (isBlank(useName) && inst.isResolvable()) {
      return Optional.of(instance().select(instCls).get());
    } else if (inst.select(resolveNameds(useName)).isResolvable()) {
      return Optional.of(inst.select(resolveNameds(useName)).get());
    } else {
      return Optional.empty();
    }
  }

  public static <T> Instance<T> select(Class<T> instanceClass, Annotation... qualifiers) {
    Class<T> instCls = shouldNotNull(instanceClass);
    return instance().select(instCls, qualifiers);
  }

  public static <T> T tryResolve(Class<T> instanceClass, Annotation... qualifiers) {
    return resolve(instanceClass, qualifiers).orElse(null);
  }

  public static <T> T tryResolve(Instance<T> instance) {
    return instance != null && instance.isResolvable() ? instance.get() : null;
  }

  public static <T> void tryResolveAccept(Class<T> instanceClass, Consumer<T> consumer) {
    Class<T> instCls = shouldNotNull(instanceClass);
    if (instance().select(instCls).isResolvable()) {
      shouldNotNull(consumer).accept(instance().select(instCls).get());
    }
  }

  public static <T, R> R tryResolveApply(Class<T> instanceClass, Function<T, R> function) {
    Class<T> instCls = shouldNotNull(instanceClass);
    if (instance().select(instCls).isResolvable()) {
      return shouldNotNull(function).apply(instance().select(instCls).get());
    }
    return null;
  }

  /**
   * corant-kernel
   *
   * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
   * ApplicationScoped.
   *
   * When InitialContext.lookup(...), will invoke CDI.select() to retrieve the object instance.
   *
   * @author bingo 下午7:42:18
   *
   */
  public static class NamingObjectFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
        Hashtable<?, ?> environment) throws Exception {
      if (obj instanceof NamingReference) {
        NamingReference reference = (NamingReference) obj;
        Class<?> theClass = asClass(reference.getClassName());
        if (reference.qualifiers.length > 0) {
          return instance().select(theClass).select(reference.qualifiers).get();
        }
        return instance().select(theClass).get();
      } else {
        throw new CorantRuntimeException(
            "Object %s named %s is not a CDI managed bean instance reference!", obj, name);
      }
    }
  }

  /**
   * corant-kernel
   *
   * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
   * ApplicationScoped.
   *
   * @author bingo 下午7:42:38
   *
   */
  public static class NamingReference extends Reference {

    private static final long serialVersionUID = -7231737490239227558L;

    protected Annotation[] qualifiers = new Annotation[0];

    /**
     * @param objectClass
     * @param qualifiers
     */
    public NamingReference(Class<?> objectClass, Annotation... qualifiers) {
      super(objectClass.getName(), NamingObjectFactory.class.getName(), null);
      int length;
      if ((length = qualifiers.length) > 0) {
        this.qualifiers = new Annotation[length];
        System.arraycopy(qualifiers, 0, this.qualifiers, 0, length);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      NamingReference other = (NamingReference) obj;
      return Arrays.equals(qualifiers, other.qualifiers);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Arrays.hashCode(qualifiers);
      return result;
    }
  }

  /**
   * Hanle CDI unmanageable bean class or object
   *
   * corant-kernel
   *
   * @author bingo 下午11:02:59
   *
   */
  public static class UnmanageableInstance<T> implements AutoCloseable {

    private T instance;
    private final CreationalContext<T> creationalContext;
    private final WeldInjectionTarget<T> injectionTarget;
    private final AnnotatedType<T> annotatedType;
    private final T orginalInstance;
    private final WeldManager bm;
    private boolean disposed = false;

    public UnmanageableInstance(Class<T> clazz) {
      bm = Corant.me().getBeanManager();
      creationalContext = bm.createCreationalContext(null);
      annotatedType = bm.createAnnotatedType(clazz);
      injectionTarget = bm.getInjectionTargetFactory(annotatedType).createInjectionTarget(null);
      orginalInstance = null;
    }

    public UnmanageableInstance(T object) {
      shouldBeFalse(isManagedBean(object));
      bm = Corant.me().getBeanManager();
      creationalContext = bm.createCreationalContext(null);
      annotatedType = bm.createAnnotatedType(forceCast(object.getClass()));
      injectionTarget =
          bm.getInjectionTargetFactory(annotatedType).createNonProducibleInjectionTarget();
      orginalInstance = object;
    }

    public static <T> UnmanageableInstance<T> of(Class<T> clazz) {
      return new UnmanageableInstance<>(clazz);
    }

    public static <T> UnmanageableInstance<T> of(T object) {
      return new UnmanageableInstance<>(object);
    }

    @Override
    public void close() throws Exception {
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
    public UnmanageableInstance<T> dispose() {
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
    public UnmanageableInstance<T> inject() {
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
     * @throws IllegalStateException if postConstruct() is called on an instance that has already
     *         been disposed
     * @return self
     */
    public UnmanageableInstance<T> postConstruct() {
      if (instance == null) {
        throw new IllegalStateException(
            "Trying to call postConstruct() before produce() was called");
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
    public UnmanageableInstance<T> preDestroy() {
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
    public UnmanageableInstance<T> produce() {
      if (instance != null) {
        throw new IllegalStateException("Trying to call produce() on already constructed instance");
      }
      if (disposed) {
        throw new IllegalStateException("Trying to call produce() on an already disposed instance");
      }
      instance =
          InterceptionFactoryImpl.of(BeanManagerProxy.unwrap(bm), creationalContext, annotatedType)
              .createInterceptedInstance(
                  orginalInstance == null ? injectionTarget.produce(creationalContext)
                      : orginalInstance);
      return this;
    }

  }
}
