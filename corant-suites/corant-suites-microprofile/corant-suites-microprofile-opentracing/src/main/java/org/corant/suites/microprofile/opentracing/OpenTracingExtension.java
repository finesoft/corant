package org.corant.suites.microprofile.opentracing;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import io.opentracing.contrib.interceptors.OpenTracingInterceptor;

/**
 * @author sushuaihao 2020/1/2
 * @since
 */
public class OpenTracingExtension implements Extension {

  public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
    String extensionName = OpenTracingExtension.class.getName();
    for (Class<?> clazz : new Class<?>[] {OpenTracingInterceptor.class}) {
      bbd.addAnnotatedType(manager.createAnnotatedType(clazz),
          extensionName + "_" + clazz.getName());
    }
  }
}
