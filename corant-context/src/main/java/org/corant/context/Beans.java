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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.forceCast;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Unnamed;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Services;
import org.jboss.weld.bean.proxy.ProxyObject;

/**
 * corant-context
 *
 * <p>
 * A convenient context bean object retrieval class for retrieving bean instances
 *
 * @author bingo 下午2:22:40
 *
 */
public class Beans {

  public static final String JAVAX_EJB_STATELESS = "javax.ejb.Stateless";
  public static final String JAVAX_EJB_SINGLETON = "javax.ejb.Singleton";
  public static final String JAKARTA_EJB_STATELESS = "jakarta.ejb.Stateless";
  public static final String JAKARTA_EJB_SINGLETON = "jakarta.ejb.Singleton";

  public static <T> T create(Class<T> clazz, Annotation... qualifiers) {
    if (clazz != null && CDIs.isEnabled()) {
      BeanManager bm = CDI.current().getBeanManager();
      Set<Bean<?>> beans = bm.getBeans(clazz, qualifiers);
      if (isNotEmpty(beans)) {
        if (beans.size() > 1) {
          beans = beans.stream().filter(b -> (b.getBeanClass().equals(clazz) || b.isAlternative()))
              .collect(Collectors.toSet());
        }
        Bean<?> bean = bm.resolve(beans);
        if (bean != null) {
          CreationalContext<?> context = bm.createCreationalContext(bean);
          return context != null ? clazz.cast(bm.getReference(bean, clazz, context)) : null;
        }
      }
    }
    return null;
  }

  /**
   * <p>
   * When called, the container destroys the instance if the active context object for the scope
   * type of the bean supports destroying bean instances. All normal scoped built-in contexts
   * support destroying bean instances.
   * </p>
   *
   * <p>
   * The instance passed should either be a dependent scoped bean instance obtained from the same
   * {@link Instance} object, or the client proxy for a normal scoped bean instance.
   * </p>
   *
   *
   * @param instance the instance to destroy
   * @throws UnsupportedOperationException if the active context object for the scope type of the
   *         bean does not support destroying bean instances
   */
  public static void destroy(Object instance) {
    if (instance != null) {
      if (!CDIs.isEnabled()) {
        throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
      }
      CDI.current().destroy(instance);
    }
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance class and
   * qualifiers, ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * <p>
   * Use with care, there may be a memory leak.
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> Optional<T> find(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance type and
   * qualifiers, ambiguous and unsatisfied return an empty {@link Optional}.
   * <p>
   * Use with care, there may be a memory leak.
   *
   * @param <T> the bean type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> Optional<T> find(TypeLiteral<T> instanceType, Annotation... qualifiers) {
    Instance<T> inst = select(instanceType, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} bean instance from CDI or ServiceLoader that matches the given
   * instance class and qualifiers.
   * <p>
   * Note: First lookup in CDI through the given instance class and qualifiers, if find a hit, it
   * will return immediately; If it is not found and the qualifiers is empty, it will be loaded from
   * {@link org.corant.shared.util.Services#findRequired(Class)}. If there are multiple instances
   * and the given instance class is {@link Sortable}, then return the one with the highest
   * priority.
   * </p>
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   *
   * @see #findService(Class)
   */
  public static <T> Optional<T> findAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else if (!inst.isUnsatisfied() && Sortable.class.isAssignableFrom(instanceClass)) {
      return forceCast(
          inst.stream().map(Sortable.class::cast).sorted(Sortable::compare).findFirst());
    } else {
      return isEmpty(qualifiers) ? findService(instanceClass) : Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance class and name,
   * ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * <p>
   * <b>Note:</b> <br>
   * The Named Qualifier here extends the Named Qualifier mechanism of CDI. If the given name is not
   * null, use CDI's Named Qualifier for lookup first, if found, it will return immediately. If the
   * given name is empty, then directly do not add any qualifiers for lookup, if found, it will
   * return immediately, otherwise it will use {@link Unnamed} qualifier for lookup.
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param name the bean name for CDI bean lookup
   *
   * @see Unnamed
   */
  public static <T> Optional<T> findNamed(Class<T> instanceClass, String name) {
    Instance<T> inst = select(instanceClass, Any.Literal.INSTANCE);
    return findNamed(inst, name);
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance type and name,
   * ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * <p>
   * <b>Note:</b> <br>
   * The Named Qualifier here extends the Named Qualifier mechanism of CDI. If the given name is not
   * null, use CDI's Named Qualifier for lookup first, if found, it will return immediately. If the
   * given name is empty, then directly do not add any qualifiers for lookup, if found, it will
   * return immediately, otherwise it will use {@link Unnamed} qualifier for lookup.
   *
   * @param <T> the bean type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param name the bean name for CDI bean lookup
   *
   * @see Unnamed
   */
  public static <T> Optional<T> findNamed(TypeLiteral<T> instanceType, String name) {
    Instance<T> inst = select(instanceType, Any.Literal.INSTANCE);
    return findNamed(inst, name);
  }

  /**
   * Returns an {@link Optional} instance that matches the given instance class from ServiceLoader
   * or return an empty {@link Optional} if not found or found multiple instances or not meet the
   * conditions.
   * <p>
   * Note: If there are multiple service instances found by ServiceLoader and given instance class
   * is {@link Sortable} then return the highest priority instance.
   * </p>
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   *
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> findService(Class<T> instanceClass) {
    return Services.findRequired(instanceClass);
  }

  /**
   * Returns the CDI bean scope of the given type and qualifiers or null if the given type is not a
   * CDI bean type.
   *
   * @param type the bean class
   * @param qualifiers the type instance qualifiers
   * @return the CDI bean scope of the given type and qualifiers or null if the given type is not a
   *         CDI bean type.
   * @throws AmbiguousResolutionException if the ambiguous dependency resolution rules fail
   * @throws IllegalStateException if called during application initialization, before the
   *         {@link AfterBeanDiscovery} event is fired or CDI container be closed.
   */
  public static Class<? extends Annotation> getBeanScope(final Class<?> type,
      Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    if (type != null) {
      BeanManager bm = CDI.current().getBeanManager();
      Bean<?> bean = bm.resolve(bm.getBeans(type, qualifiers));
      if (bean != null) {
        return bean.getScope();
      }
    }
    return null;
  }

  @Deprecated
  public static boolean isManagedBean(Object object) {
    if (object instanceof ProxyObject) {
      return true; // for WELD only
    } else if (object != null && object.getClass().getAnnotation(Singleton.class) != null) {
      Instance<?> inst = select(getUserClass(object));
      if (inst.isResolvable()) {
        return inst.get().equals(object);
      }
    }
    return false;
  }

  /**
   * Test the given annotation type with CDI current bean manager to determine if it is a scope
   * type.
   *
   * @param annotatedType the annotated type to check
   * @return true if the annotation type is a scope type
   */
  public static boolean isScopeDefined(AnnotatedType<?> annotatedType) {
    return isScopeDefined(annotatedType, CDI.current().getBeanManager());
  }

  /**
   * Test the given annotation type to determine if it is a scope type.
   *
   * @param annotatedType the annotated type to check
   * @param manager the bean manager
   * @return true if the annotation type is a scope type
   */
  public static boolean isScopeDefined(AnnotatedType<?> annotatedType, BeanManager manager) {
    for (Annotation annotation : annotatedType.getAnnotations()) {
      if (manager.isScope(annotation.annotationType())) {
        return true;
      }
      if (manager.isStereotype(annotation.annotationType())
          && isScopeDefined(annotation.annotationType(), manager)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the given annotated type is an EJB session bean
   *
   * @param annotatedType the given annotated type to check
   * @return true if the given annotated type is EJB session bean
   */
  public static boolean isSessionBean(AnnotatedType<?> annotatedType) {
    for (Annotation annotation : annotatedType.getAnnotations()) {
      Class<?> annotationType = annotation.annotationType();
      if (JAVAX_EJB_STATELESS.equals(annotationType.getName())
          || JAVAX_EJB_SINGLETON.equals(annotationType.getName())
          || JAKARTA_EJB_STATELESS.equals(annotationType.getName())
          || JAKARTA_EJB_SINGLETON.equals(annotationType.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Make given object manageable, if given object is already managed in CDI then return it
   * directly.
   *
   * @param <T> the manageable bean type
   * @param obj the object that will be managed
   */
  public static <T> T manageable(T obj) {
    if (isManagedBean(obj)) {
      return obj;
    } else if (obj != null) {
      return UnmanageableBean.of(obj).produce().inject().postConstruct().get();
    }
    return null;
  }

  /**
   * Returns CDI bean instance, throws exception when it cannot be resolved.
   * <p>
   * Use with care, there may be a memory leak.
   * </p>
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> T resolve(Class<T> instanceClass, Annotation... qualifiers) {
    return select(instanceClass, qualifiers).get();
  }

  /**
   * Returns CDI bean instance, throws exception when it cannot be resolved.
   * <p>
   * Use with care, there may be a memory leak.
   * </p>
   *
   * @param <T> the instance type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> T resolve(TypeLiteral<T> instanceType, Annotation... qualifiers) {
    return select(instanceType, qualifiers).get();
  }

  /**
   * Resolve CDI bean instance and consumer it, throws exception when it cannot be resolved.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param consumer the consumer that consumer the bean instance
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> void resolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    shouldNotNull(consumer).accept(resolve(instanceClass, qualifiers));
  }

  /**
   * Returns bean instance from CDI or Service Loader or throws exception if not found.
   *
   * <p>
   * Note: First lookup in CDI through the given instance class and qualifiers, when find a hit, it
   * will return immediately; If it is not found and the qualifiers is empty, it will be loaded from
   * {@link org.corant.shared.util.Services#findRequired(Class)}. If there are multiple instances
   * and the given instance class is {@link Sortable}, then return the one with the highest
   * priority; If none of the above searches are found, an exception is thrown.
   * <p>
   * Use with care, there may be a memory leak.
   * </p>
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   * @param qualifiers the instance qualifiers that use to resolve
   *
   * @see #findAnyway(Class, Annotation...)
   */
  public static <T> T resolveAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    return findAnyway(instanceClass, qualifiers).orElseThrow(
        () -> new CorantRuntimeException("Can not resolve bean class %s.", instanceClass));
  }

  /**
   * Resolve CDI bean instance and returns the result using the given function interface, if the
   * bean instance can't resolve then throw exception.
   *
   * @param <T> the bean instance type to be resolved
   * @param <R> the function return type
   * @param instanceClass the bean instance class to be resolved
   * @param function the function that bean instance will be applied
   * @param qualifiers the bean qualifiers to be resolved
   */
  public static <T, R> R resolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    return shouldNotNull(function).apply(resolve(instanceClass, qualifiers));
  }

  /**
   * Returns an instance that matches the given instance class and qualifiers or throws an exception
   * if CDI is disabled.
   *
   * @param <T> the instance type to be selected
   * @param instanceClass the instance class to be selected
   * @param qualifiers the qualifiers to be selected
   */
  public static <T> Instance<T> select(Class<T> instanceClass, Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(instanceClass), qualifiers);
  }

  /**
   * Returns an instance that matches the given instance type and qualifiers or throws an exception
   * if CDI is disabled.
   *
   * @param <T> the instance type to be selected
   * @param <U> the instance type to be selected
   * @param subtype the required type
   * @param qualifiers the qualifiers to be selected
   */
  public static <T, U extends T> Instance<U> select(TypeLiteral<U> subtype,
      Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(subtype), qualifiers);
  }

  public static void touch(Class<?>... cas) {
    if (cas.length > 0 && CDIs.isEnabled()) {
      for (Class<?> ca : cas) {
        Instance<?> inst = CDI.current().select(ca);
        if (!inst.isUnsatisfied()) {
          inst.stream().forEach(Object::toString);
        }
      }
    }
  }

  @SafeVarargs
  public static void touch(Pair<Class<?>, Annotation[]>... cas) {
    if (cas.length > 0 && CDIs.isEnabled()) {
      for (Pair<Class<?>, Annotation[]> ca : cas) {
        Instance<?> inst = CDI.current().select(ca.first(), ca.second());
        if (!inst.isUnsatisfied()) {
          inst.stream().forEach(Object::toString);
        }
      }
    }
  }

  /**
   * Returns a bean instance from CDI that matches the given instance class and qualifiers, if not
   * matches or CDI isn't enabled returns null.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   * @param qualifiers the qualifiers to be resolved
   */
  public static <T> T tryResolve(Class<T> instanceClass, Annotation... qualifiers) {
    return CDIs.isEnabled() ? find(instanceClass, qualifiers).orElse(null) : null;
  }

  public static <T> void tryResolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    T instance = tryResolve(instanceClass, qualifiers);
    if (instance != null) {
      shouldNotNull(consumer).accept(instance);
    }
  }

  public static <T, R> R tryResolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    T instance = tryResolve(instanceClass, qualifiers);
    if (instance != null) {
      return shouldNotNull(function).apply(instance);
    }
    return null;
  }

  static <T> Optional<T> findNamed(Instance<T> instance, String name) {
    Instance<T> inst = instance;
    if (inst.isUnsatisfied()) {
      return Optional.empty();
    }
    // supports the normal Named qualifier
    if (name != null) {
      Instance<T> normal = inst.select(NamedLiteral.of(name));
      if (normal.isResolvable()) {
        return Optional.of(normal.get());
      }
    }
    // supports the extended Named qualifier
    String useName = Qualifiers.resolveName(name);
    if (Qualifiers.EMPTY_NAME.equals(useName) && inst.isResolvable()
        || (inst = inst.select(Unnamed.INST)).isResolvable()) {
      return Optional.of(inst.get());
    }
    return Optional.empty();
  }

  static boolean isScopeDefined(Class<?> clazz, BeanManager manager) {
    for (Annotation annotation : clazz.getAnnotations()) {
      if (manager.isScope(annotation.annotationType())) {
        return true;
      }
      if (manager.isStereotype(annotation.annotationType())
          && isScopeDefined(annotation.annotationType(), manager)) {
        return true;
      }
    }
    return false;
  }
}
