package org.corant.modules.quartz.embeddable;

import static java.util.Collections.newSetFromMap;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.annotation.Priority;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.context.proxy.ProxyBuilder;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Services;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
public class CorantSchedulerExtension implements Extension {

  protected final Set<CorantDeclarativeJobMetaData> declarativeJobMetaDatas =
      newSetFromMap(new ConcurrentHashMap<>());

  public Set<CorantDeclarativeJobMetaData> getDeclarativeJobMetaDatas() {
    return Collections.unmodifiableSet(declarativeJobMetaDatas);
  }

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) PreContainerStopEvent bs) {
    declarativeJobMetaDatas.clear();
  }

  protected void onProcessAnnotatedType(
      @Observes @WithAnnotations({CorantTrigger.class}) ProcessAnnotatedType<?> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    ProxyBuilder.buildDeclaredMethods(beanClass, m -> m.isAnnotationPresent(CorantTrigger.class))
        .stream().map(CorantDeclarativeJobMetaData::of).forEach(declarativeJobMetaDatas::add);
  }
}
