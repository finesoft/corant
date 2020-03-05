package org.corant.suites.microprofile.opentracing;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.util.GlobalTracer;

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
    final Tracer tracer = defaultObject(TracerResolver.resolveTracer(), GlobalTracer.get());
    GlobalTracer.register(tracer);
    logger.fine(() -> String.format("Registering %s to GlobalTracer and providing it as CDI bean.",
        tracer));
    return tracer;
  }
}
