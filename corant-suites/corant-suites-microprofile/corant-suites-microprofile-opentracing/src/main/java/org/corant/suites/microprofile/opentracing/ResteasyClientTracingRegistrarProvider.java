package org.corant.suites.microprofile.opentracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.smallrye.opentracing.SmallRyeClientTracingFeature;

/**
 * @auther sushuaihao 2020/1/2
 * @since
 */
public class ResteasyClientTracingRegistrarProvider implements ClientTracingRegistrarProvider {

  @Override
  public ClientBuilder configure(ClientBuilder clientBuilder) {
    // Make sure executor is the same as a default in resteasy ClientBuilder
    return configure(clientBuilder, Executors.newFixedThreadPool(10));
  }

  @Override
  public ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
    Tracer tracer = CDI.current().select(Tracer.class).get();
    return clientBuilder.executorService(new TracedExecutorService(executorService, tracer))
        .register(new SmallRyeClientTracingFeature(tracer));
  }
}
