package org.corant.suites.quartz.embeddable;

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

/**
 * corant-suites-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
public class CorantJobExtension implements Extension {

  protected final Set<CorantJobMetaData> jobMetaDatas = newSetFromMap(new ConcurrentHashMap<>());

  public Set<CorantJobMetaData> getJobMetaDatas() {
    return Collections.unmodifiableSet(jobMetaDatas);
  }

  protected void onBeforeShutdown(@Observes @Priority(0) PreContainerStopEvent bs) {
    jobMetaDatas.clear();
  }

  protected void onProcessAnnotatedType(
      @Observes @WithAnnotations({CorantTrigger.class}) ProcessAnnotatedType<?> pat) {
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    ContextualMethodHandler.fromDeclared(beanClass, m -> m.isAnnotationPresent(CorantTrigger.class))
        .stream().map(CorantJobMetaData::of).forEach(cm -> jobMetaDatas.add(cm));
  }
}
