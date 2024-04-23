/*
 * JBoss, Home of Professional Open Source Copyright 2016, Red Hat, Inc., and individual
 * contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.corant.modules.vertx.serviceproxy;

import static java.lang.String.format;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.util.AnnotationLiteral;
import org.corant.modules.vertx.serviceproxy.ServiceProxy.ServiceProxyLiteral;
import io.vertx.codegen.annotations.ProxyGen;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * This extension attempts to find all service proxy interfaces and for each one register a custom
 * bean implementation with {@link ServiceProxy} qualifier.
 *
 * @author Martin Kouba
 */
public class ServiceProxyExtension implements Extension {

  private static final Logger LOGGER = Logger.getLogger(ServiceProxyExtension.class.getName());

  private Set<Class<?>> serviceInterfaces;

  void findServiceInterfaces(
      @Observes @WithAnnotations(ProxyGen.class) ProcessAnnotatedType<?> event,
      BeanManager beanManager) {
    AnnotatedType<?> annotatedType = event.getAnnotatedType();
    if (annotatedType.isAnnotationPresent(ProxyGen.class)
        && annotatedType.getJavaClass().isInterface()) {
      LOGGER.fine(format("Service interface %s discovered", annotatedType.getJavaClass()));
      serviceInterfaces.add(annotatedType.getJavaClass());
    }
  }

  void init(@Observes BeforeBeanDiscovery event) {
    serviceInterfaces = new HashSet<>();
  }

  void registerServiceProxyBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    for (Class<?> serviceInterface : serviceInterfaces) {
      event.addBean().id(ServiceProxyExtension.class.getName() + "_" + serviceInterface.getName())
          .scope(Dependent.class).types(serviceInterface, Object.class)
          .qualifiers(Any.Literal.INSTANCE, ServiceProxyLiteral.EMPTY).createWith(ctx -> {
            // First obtain the injection point metadata
            InjectionPoint injectionPoint = (InjectionPoint) beanManager
                .getInjectableReference(new InjectionPointMetadataInjectionPoint(), ctx);
            // And obtain the address on which the service is published
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            String address = null;
            for (Annotation qualifier : qualifiers) {
              if (ServiceProxy.class.equals(qualifier.annotationType())) {
                ServiceProxy serviceProxy = (ServiceProxy) qualifier;
                address = serviceProxy.value();
                break;
              }
            }
            if (address == null) {
              throw new IllegalStateException("Service proxy address is not declared");
            }
            Instance<ServiceProxySupport> supportInstance =
                CDI.current().select(ServiceProxySupport.class);
            if (!supportInstance.isResolvable()) {
              throw new IllegalStateException("ServiceProxySupport cannot be resolved");
            }
            ServiceProxySupport serviceProxySupport = supportInstance.get();
            return Proxy.newProxyInstance(ServiceProxyExtension.class.getClassLoader(),
                new Class[] {serviceInterface},
                new ServiceProxyInvocationHandler(serviceProxySupport, serviceInterface, address));
          });

      LOGGER.info(format("Custom bean for service interface %s registered", serviceInterface));
    }
  }

  private static class InjectionPointMetadataInjectionPoint implements InjectionPoint {

    @Override
    public Annotated getAnnotated() {
      return null;
    }

    @Override
    public Bean<?> getBean() {
      return null;
    }

    @Override
    public Member getMember() {
      return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return Collections.singleton(new AnnotationLiteral<Default>() {
        private static final long serialVersionUID = -8665616511776537386L;
      });
    }

    @Override
    public Type getType() {
      return InjectionPoint.class;
    }

    @Override
    public boolean isDelegate() {
      return false;
    }

    @Override
    public boolean isTransient() {
      return false;
    }

  }

}
