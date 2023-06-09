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
package org.corant.modules.vertx.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
import org.corant.config.Configs;
import org.corant.context.concurrent.AsynchronousReference;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.util.reflection.Reflections;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * The central point of integration. Its task is to find all CDI observer methods that should be
 * notified when a message is sent via {@link io.vertx.core.eventbus.EventBus}. See also
 * {@link VertxEvent} and {@link VertxConsumer}.
 * <p>
 * If a {@link Vertx} instance is available:
 * <ul>
 * <li>also add custom beans for {@link Vertx} and {@link Context},</li>
 * <li>and register consumers for all the addresses found.</li>
 * </ul>
 * </p>
 * <p>
 * {@link #registerConsumers(Vertx, Event)} could be also used after the bootstrap, e.g. when a
 * Vertx instance is only available after a CDI container is initialized.
 * </p>
 *
 * @author Martin Kouba
 * @see VertxEvent
 * @see VertxConsumer
 */
public class VertxExtension implements Extension {

  public static final String CONSUMER_REGISTRATION_TIMEOUT_KEY =
      "corant.vertx.shared.consumer.register.timeout";

  public static final long DEFAULT_CONSUMER_REGISTRATION_TIMEOUT = 10000L;

  private static final Logger LOGGER = Logger.getLogger(VertxExtension.class.getName());

  private final Set<String> consumerAddresses;

  private final Set<Annotation> asyncReferenceQualifiers;

  private final Vertx vertx;

  private final Context context;

  public VertxExtension() {
    this(null, null);
  }

  public VertxExtension(Vertx vertx) {
    this(vertx, vertx.getOrCreateContext());
  }

  public VertxExtension(Vertx vertx, Context context) {
    consumerAddresses = new HashSet<>();
    asyncReferenceQualifiers = new HashSet<>();
    this.vertx = vertx;
    this.context = context;
  }

  public void processVertxEventObserver(@Observes ProcessObserverMethod<VertxEvent, ?> event) {
    String vertxAddress = getVertxAddress(event.getObserverMethod());
    if (vertxAddress == null) {
      LOGGER.warning(String.format("VertxEvent observer found but no @VertxConsumer declared: %s",
          event.getObserverMethod()));
      return;
    }
    LOGGER.fine(String.format("Vertx message consumer found: %s", event.getObserverMethod()));
    consumerAddresses.add(vertxAddress);
  }

  public void registerBeansAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
    if (vertx == null) {
      return;
    }
    event.addBean().types(getBeanTypes(vertx.getClass(), Vertx.class))
        .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE)
        .scope(ApplicationScoped.class).createWith(c -> vertx);
    event.addBean().types(getBeanTypes(context.getClass(), Context.class))
        .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE)
        .scope(ApplicationScoped.class).createWith(c -> context);
  }

  public void registerConsumers(Vertx vertx, Event<Object> event) {
    shouldNotNull(vertx);
    CountDownLatch latch = new CountDownLatch(consumerAddresses.size());
    for (String address : consumerAddresses) {
      MessageConsumer<?> consumer =
          vertx.eventBus().consumer(address, VertxHandler.from(vertx, event, address));
      consumer.completionHandler(ar -> {
        if (ar.succeeded()) {
          LOGGER.fine(String.format("Successfully registered event consumer for %s", address));
          latch.countDown();
        } else {
          LOGGER.log(Level.SEVERE, String.format("Cannot register event consumer for %s", address),
              ar.cause());
        }
      });
    }
    Context context = this.context;
    if (context == null) {
      context = vertx.getOrCreateContext();
    }
    long timeout =
        context != null
            ? context.config().getLong(CONSUMER_REGISTRATION_TIMEOUT_KEY,
                DEFAULT_CONSUMER_REGISTRATION_TIMEOUT)
            : DEFAULT_CONSUMER_REGISTRATION_TIMEOUT;
    try {
      if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
        throw new IllegalStateException(String.format(
            "Message consumers not registered within %s ms [registered: %s, total: %s]", timeout,
            latch.getCount(), consumerAddresses.size()));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  public void registerConsumersAfterDeploymentValidation(
      @Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager beanManager) {
    if (vertx != null) {
      registerConsumers(vertx, BeanManagerProxy.unwrap(beanManager).event());
    }
    asyncReferenceQualifiers.clear();
  }

  @SuppressWarnings("rawtypes")
  void addAsyncReferenceQualifiers(
      @Observes ProcessBeanAttributes<VertxAsynchronousReference> event) {
    // Add all discovered qualifiers to AsyncReferenceImpl bean attributes
    if (!asyncReferenceQualifiers.isEmpty()) {
      LOGGER.fine(String.format("Adding additional AsyncReference qualifiers: %s",
          asyncReferenceQualifiers));
      event.configureBeanAttributes().addQualifiers(asyncReferenceQualifiers);
    }
  }

  @SuppressWarnings("rawtypes")
  void processAsyncReferenceInjectionPoints(
      @Observes ProcessInjectionPoint<?, ? extends AsynchronousReference> event) {
    asyncReferenceQualifiers.addAll(event.getInjectionPoint().getQualifiers());
  }

  private Set<Type> getBeanTypes(Class<?> implClazz, Type... types) {
    Set<Type> beanTypes = new HashSet<>();
    Collections.addAll(beanTypes, types);
    beanTypes.add(implClazz);
    // Add all the interfaces (and extended interfaces) implemented directly by the impl class
    beanTypes.addAll(Reflections.getInterfaceClosure(implClazz));
    return beanTypes;
  }

  private Annotation getQualifier(ObserverMethod<?> observerMethod) {
    for (Annotation qualifier : observerMethod.getObservedQualifiers()) {
      if (qualifier.annotationType().equals(VertxConsumer.class)) {
        return qualifier;
      }
    }
    return null;
  }

  private String getVertxAddress(ObserverMethod<?> observerMethod) {
    Annotation qualifier = getQualifier(observerMethod);
    String address = qualifier != null ? ((VertxConsumer) qualifier).value() : null;
    return Configs.assemblyStringConfigProperty(address);
  }

}
