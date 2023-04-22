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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.corant.context.concurrent.AsynchronousReference;
import org.jboss.weld.inject.WeldInstance;
import org.jboss.weld.inject.WeldInstance.Handler;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.jboss.weld.logging.BeanManagerLogger;
import org.jboss.weld.util.ForwardingCompletionStage;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;
import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 *
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * Implementation notes:
 *
 * <ul>
 * <ol>
 * the set of qualifiers of this bean is enhanced so that it satisfies all injection points with
 * required type {@link AsynchronousReference} - see
 * {@link VertxExtension#processAsyncReferenceInjectionPoints(jakarta.enterprise.inject.spi.ProcessInjectionPoint))}
 * </ol>
 * <ol>
 * the set of bean types of this bean is restricted
 * </ol>
 * </ul>
 *
 * <p>
 * If there is a producer method whose return type is {@link CompletionStage} where the result type
 * matches the required type and has all the required qualifiers (according to type-safe resolution
 * rules) then {@link CompletionStage#whenComplete(java.util.function.BiConsumer)} is used to
 * process the reference. Otherwise, a worker thread is used so that the processing does not block
 * some loop thread.
 * </p>
 *
 * <p>
 * For a normal scoped bean the contextual instance is initialized eagerly (unlike when performing
 * normal dependency injection).
 * </p>
 *
 * @author Martin Kouba
 * @param <T>
 */
@Typed(AsynchronousReference.class)
@Dependent
class VertxAsynchronousReference<T> extends ForwardingCompletionStage<T>
    implements AsynchronousReference<T> {

  private final WeldInstance<Object> instance;

  private final AtomicBoolean isDone;

  private final VertxCompletableFuture<T> future;

  private volatile T reference;

  private volatile Throwable cause;

  @Inject
  public VertxAsynchronousReference(InjectionPoint injectionPoint, Vertx vertx,
      BeanManager beanManager, @Any WeldInstance<Object> instance) {
    isDone = new AtomicBoolean(false);
    future = new VertxCompletableFuture<>(vertx);
    this.instance = instance;

    ParameterizedType parameterizedType = (ParameterizedType) injectionPoint.getType();
    Type requiredType = parameterizedType.getActualTypeArguments()[0];
    Annotation[] qualifiers = injectionPoint.getQualifiers().toArray(new Annotation[] {});

    // First check if there is a relevant async producer method available
    WeldInstance<Object> completionStage =
        instance.select(new ParameterizedTypeImpl(CompletionStage.class, requiredType), qualifiers);

    if (completionStage.isAmbiguous()) {
      failure(new AmbiguousResolutionException("Ambiguous async producer methods for type "
          + requiredType + " with qualifiers " + injectionPoint.getQualifiers()));
    } else if (!completionStage.isUnsatisfied()) {
      // Use the produced CompletionStage
      initWithCompletionStage(completionStage.getHandler());
    } else {
      // Use Vertx worker thread
      initWithWorker(requiredType, qualifiers, vertx, beanManager);
    }
  }

  @Override
  public Throwable cause() {
    return cause;
  }

  @Override
  public T get() {
    return reference;
  }

  @Override
  public boolean isDone() {
    return isDone.get();
  }

  @Override
  public String toString() {
    return "VertxAsynchronousReference [isDone=" + isDone + ", reference=" + reference + ", cause="
        + cause + "]";
  }

  @Override
  protected CompletionStage<T> delegate() {
    return future;
  }

  private void complete(T result, Throwable cause) {
    if (isDone.compareAndSet(false, true)) {
      if (cause != null) {
        this.cause = cause;
        future.completeExceptionally(cause);
      } else {
        reference = result;
        future.complete(result);
      }
    }
  }

  private void failure(Throwable cause) {
    complete(null, cause);
  }

  @SuppressWarnings("unchecked")
  private void initWithCompletionStage(Handler<Object> completionStage) {
    Object possibleStage = completionStage.get();
    if (possibleStage instanceof CompletionStage) {
      ((CompletionStage<T>) possibleStage).whenComplete((result, throwable) -> {
        if (throwable != null) {
          failure(throwable);
        } else {
          success(result);
        }
      });
    } else {
      throw new IllegalStateException(
          "The contextual reference of " + completionStage.getBean() + " is not a CompletionStage");
    }
  }

  @SuppressWarnings("unchecked")
  private void initWithWorker(Type requiredType, Annotation[] qualifiers, Vertx vertx,
      BeanManager beanManager) {
    vertx.executeBlocking((promise -> {
      WeldInstance<Object> asyncInstance = instance.select(requiredType, qualifiers);
      if (asyncInstance.isUnsatisfied()) {
        promise.fail(BeanManagerLogger.LOG.injectionPointHasUnsatisfiedDependencies(
            Arrays.toString(qualifiers), requiredType, ""));
        return;
      } else if (asyncInstance.isAmbiguous()) {
        promise.fail(BeanManagerLogger.LOG
            .injectionPointHasAmbiguousDependencies(Arrays.toString(qualifiers), requiredType, ""));
        return;
      }
      Handler<Object> handler = asyncInstance.getHandler();
      Object beanInstance = handler.get();
      if (beanManager.isNormalScope(handler.getBean().getScope())
          && beanInstance instanceof TargetInstanceProxy) {
        // Initialize normal scoped bean instance eagerly
        ((TargetInstanceProxy<?>) beanInstance).weld_getTargetInstance();
      }
      promise.complete(beanInstance);
    }), r -> {
      if (r.succeeded()) {
        success((T) r.result());
      } else {
        failure(r.cause());
      }
    });
  }

  private void success(T result) {
    complete(result, null);
  }

}
