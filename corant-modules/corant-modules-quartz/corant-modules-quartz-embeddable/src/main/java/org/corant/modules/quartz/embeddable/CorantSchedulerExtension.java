package org.corant.modules.quartz.embeddable;

import static java.util.Collections.newSetFromMap;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.required.Required;
import org.corant.shared.normal.Priorities;

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
    if (Required.shouldVeto(pat.getAnnotatedType())) {
      return;
    }
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    ContextualMethodHandler.fromDeclared(beanClass, m -> m.isAnnotationPresent(CorantTrigger.class))
        .stream().map(CorantDeclarativeJobMetaData::of).forEach(declarativeJobMetaDatas::add);
  }
}
