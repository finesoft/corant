package org.corant.suites.microprofile.opentracing;

import java.util.EnumSet;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.smallrye.opentracing.SmallRyeTracingDynamicFeature;

/**
 *
 * @auther sushuaihao 2019/12/31
 * @since
 */
@ApplicationScoped
@WebListener
public class ServletContextTracingInstaller implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ServletContext servletContext = servletContextEvent.getServletContext();
    servletContext.setInitParameter("resteasy.providers",
        SmallRyeTracingDynamicFeature.class.getName());

    Dynamic filterRegistration =
        servletContext.addFilter("tracingFilter", new SpanFinishingFilter());
    filterRegistration.setAsyncSupported(true);
    filterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "*");
  }
}
