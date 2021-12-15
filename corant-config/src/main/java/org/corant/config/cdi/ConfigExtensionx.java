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
package org.corant.config.cdi;

import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.isPrimitiveWrapper;
import static org.corant.shared.util.Primitives.unwrap;
import static org.corant.shared.util.Sets.setOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.inject.Provider;
import org.corant.config.CorantConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-config
 *
 * @author bingo 下午4:34:28
 *
 */
@Deprecated
public class ConfigExtensionx implements Extension {

  static final Logger logger = Logger.getLogger(ConfigExtensionx.class.getName());
  static final Set<TypesAndQualifier> beanDefinitions = new HashSet<>();

  static Set<Type> resolveInjectTypes(Type injectType) {
    final HashSet<Type> resolvedTypes = new HashSet<>();
    Type type = injectType;
    if (type instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) type;
      if (Provider.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
        type = paramType.getActualTypeArguments()[0];
      }
    }
    resolvedTypes.add(type);
    if (type instanceof Class) {
      Class<?> typeClass = (Class<?>) type;
      if (isPrimitiveWrapper(typeClass)) {
        Class<?> primitiveClass = unwrap(typeClass);
        resolvedTypes.add(primitiveClass);
        Object array = Array.newInstance(primitiveClass, 0);
        resolvedTypes.add(array.getClass());
      }
      Object array = Array.newInstance(typeClass, 0);
      resolvedTypes.add(array.getClass());
    }
    return resolvedTypes;
  }

  private static Annotation resolveInjectQualifier(InjectionPoint ip) {
    String className = ip.getMember().getDeclaringClass().getName().concat(".");
    Annotated annotated = ip.getAnnotated();
    if (annotated instanceof AnnotatedField) {
      return NamedLiteral
          .of(className.concat(((AnnotatedField<?>) annotated).getJavaMember().getName()));
    }
    if (annotated instanceof AnnotatedParameter) {
      AnnotatedParameter<?> ap = (AnnotatedParameter<?>) annotated;
      Member member = ip.getMember();
      if (member instanceof Method) {
        return NamedLiteral.of(className.concat(member.getName() + "_" + ap.getPosition()));
      }
      if (member instanceof Constructor) {
        return NamedLiteral.of(className.concat("new_" + ap.getPosition()));
      }
    }
    return NamedLiteral.of(className.concat(ip.getMember().getName()));
  }

  void onAfterBeanDisconvery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    Config currentConfig = ConfigProvider.getConfig();
    if (currentConfig instanceof CorantConfig) {
      AnnotatedType<ConfigProducer> beanType =
          beanManager.createAnnotatedType(ConfigProducer.class);
      BeanAttributes<?> attributes = null;
      AnnotatedMethod<? super ConfigProducer> method = null;
      for (AnnotatedMethod<?> annMethod : beanType.getMethods()) {
        if ("getConfigProperty".equals(annMethod.getJavaMember().getName())) {
          attributes = beanManager.createBeanAttributes(annMethod);
          method = forceCast(annMethod);
          break;
        }
      }
      if (attributes != null) {
        for (final TypesAndQualifier taq : beanDefinitions) {
          event.addBean(
              beanManager.createBean(TypesBeanAttrs.of(attributes, taq.qualifiers, taq.types),
                  ConfigProducer.class, beanManager.getProducerFactory(method, null)));
        }
      }
    }
  }

  void onProcessBean(@Observes ProcessBean<?> pb, BeanManager bm) {
    Bean<?> bean = pb.getBean();
    Set<InjectionPoint> beanInjectionPoints = bean.getInjectionPoints();
    if (beanInjectionPoints != null && !beanInjectionPoints.isEmpty()) {
      for (InjectionPoint beanInjectionPoint : beanInjectionPoints) {
        if (beanInjectionPoint != null) {
          Set<Annotation> qualifiers = beanInjectionPoint.getQualifiers();
          assert qualifiers != null;
          for (Annotation qualifier : qualifiers) {
            if (qualifier instanceof ConfigProperty) {
              beanDefinitions.add(TypesAndQualifier.of(beanInjectionPoint));
              break;
            }
          }
        }
      }
    }
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager bm) {
    ConfigProperty configProperty =
        pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
    if (configProperty != null) {
      InjectionPoint ip = pip.getInjectionPoint();
      pip.configureInjectionPoint().addQualifier(resolveInjectQualifier(ip));
    }
  }

  <X> void onProcessObserverMethod(@Observes ProcessObserverMethod<?, X> pob, BeanManager bm) {
    AnnotatedMethod<X> annotatedMethod = pob.getAnnotatedMethod();
    List<AnnotatedParameter<X>> annotatedParameters = annotatedMethod.getParameters();
    if (annotatedParameters != null && annotatedParameters.size() > 1) {
      for (AnnotatedParameter<?> annotatedParameter : annotatedParameters) {
        if (annotatedParameter != null && !annotatedParameter.isAnnotationPresent(Observes.class)) {
          InjectionPoint injectionPoint = bm.createInjectionPoint(annotatedParameter);
          Set<Annotation> qualifiers = injectionPoint.getQualifiers();
          assert qualifiers != null;
          for (Annotation qualifier : qualifiers) {
            if (qualifier instanceof ConfigProperty) {
              beanDefinitions.add(TypesAndQualifier.of(injectionPoint));
              break;
            }
          }
        }
      }
    }
  }

  /**
   * corant-config
   *
   * @author bingo 下午2:12:37
   *
   */
  static class TypesAndQualifier {
    final Set<Type> types;
    final Set<Annotation> qualifiers;

    TypesAndQualifier(Set<Type> types, Set<Annotation> qualifiers) {
      this.types = types;
      this.qualifiers = qualifiers;
    }

    static TypesAndQualifier of(InjectionPoint ip) {
      return new TypesAndQualifier(resolveInjectTypes(ip.getType()),
          setOf(resolveInjectQualifier(ip)));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TypesAndQualifier other = (TypesAndQualifier) obj;
      if (qualifiers == null) {
        if (other.qualifiers != null) {
          return false;
        }
      } else if (!qualifiers.equals(other.qualifiers)) {
        return false;
      }
      if (types == null) {
        if (other.types != null) {
          return false;
        }
      } else if (!types.equals(other.types)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (qualifiers == null ? 0 : qualifiers.hashCode());
      return prime * result + (types == null ? 0 : types.hashCode());
    }

  }

  static class TypesBeanAttrs<T> implements BeanAttributes<T> {

    private final BeanAttributes<?> delegate;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    TypesBeanAttrs(BeanAttributes<?> delegate, Set<Annotation> qualifiers, Set<Type> types) {
      this.delegate = delegate;
      this.types = Collections.unmodifiableSet(types);
      Set<Annotation> validQualifiers = new HashSet<>(qualifiers);
      validQualifiers.addAll(delegate.getQualifiers());
      this.qualifiers = Collections.unmodifiableSet(validQualifiers);
    }

    public static <T> TypesBeanAttrs<T> of(BeanAttributes<?> delegate, Set<Annotation> qualifiers,
        Set<Type> types) {
      return new TypesBeanAttrs<>(delegate, qualifiers, types);
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return delegate.getScope();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
      return delegate.getStereotypes();
    }

    @Override
    public Set<Type> getTypes() {
      return types;
    }

    @Override
    public boolean isAlternative() {
      return delegate.isAlternative();
    }

  }
}
