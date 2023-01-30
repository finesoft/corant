package org.corant.modules.microprofile.healthcheck;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * A HealthIndicator that checks the ReadinessState of the application.
 * @author don
 * @date 2023/1/29
 */
@ApplicationScoped
public class ReadinessStateHealthIndicator {

  protected final String name = "ReadinessState";
  protected boolean ready = false;

  @Produces
  @Readiness
  HealthCheck check2() {
    return () -> HealthCheckResponse.named(name).status(ready).build();
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    ready = true;
  }

  void onPreContainerStopEvent(@Observes PreContainerStopEvent adv) {
    ready = false;
  }
}
