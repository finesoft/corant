package org.corant.suites.microprofile.opentracing;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.util.GlobalTracer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.logging.Logger;

/**
 *
 * @auther sushuaihao 2020/1/2
 * @since
 */
@ApplicationScoped
public class TracerProducer {
  private static final Logger logger = Logger.getLogger(TracerProducer.class.toString());

  @Produces
  @ApplicationScoped
  public Tracer produceTracer() {
    Tracer tracer = TracerResolver.resolveTracer();
    if (tracer == null) {
      tracer = GlobalTracer.get();
    }
    logger.info(
        String.format("Registering %s to GlobalTracer and providing it as CDI bean.", tracer));
    GlobalTracer.register(tracer);
    return tracer;
  }
}
