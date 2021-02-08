package org.corant.suites.quartz.embeddable;

import org.corant.context.proxy.SerializableContextualMethodHandler;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
public class CorantJobExtension implements Extension {

  protected final Set<SerializableContextualMethodHandler> jobMethods =
      newSetFromMap(new ConcurrentHashMap<>());

  public Set<SerializableContextualMethodHandler> getJobMethods() {
    return Collections.unmodifiableSet(jobMethods);
  }

  protected void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    jobMethods.clear();
  }

  protected void onProcessAnnotatedType(
      @Observes @WithAnnotations({CorantTrigger.class}) ProcessAnnotatedType<?> pat) {
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    SerializableContextualMethodHandler.fromDeclared(
            beanClass, m -> m.isAnnotationPresent(CorantTrigger.class), NamedLiteral.of("hello"))
        .forEach(cm -> jobMethods.add(cm));
  }
}
