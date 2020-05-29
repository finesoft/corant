package org.corant.suites.jaxrs.resteasy;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;

/**
 * 跨域处理 使用官方CorsFilter
 * @author don
 * @date 2020-05-28
 */

@Provider
@ApplicationScoped
public class ResteasyCorsProvider implements Feature {

  @Inject
  @ConfigProperty(name = "rs.cors.enabled", defaultValue = "false")
  protected boolean enabled;
  @Inject
  @ConfigProperty(name = "rs.cors.origin", defaultValue = "*")
  protected String origin;
  @Inject
  @ConfigProperty(name = "rs.cors.headers", defaultValue = "")
  protected String headers;
  @Inject
  @ConfigProperty(name = "rs.cors.credentials", defaultValue = "false")
  protected boolean credentials;
  @Inject
  @ConfigProperty(name = "rs.cors.methods", defaultValue = "GET, POST, OPTIONS")
  protected String methods;
  @Inject
  @ConfigProperty(name = "rs.cors.maxAge", defaultValue = "86400")
  protected int maxAge;

  @Override
  public boolean configure(FeatureContext context) {
    if (isEnabled()) {
      CorsFilter corsFilter = new CorsFilter();
      corsFilter.getAllowedOrigins().add(getOrigin());
      corsFilter.setAllowCredentials(getCredentials());
      corsFilter.setCorsMaxAge(getMaxAge());
      context.register(corsFilter);
      return true;
    } else {
      return false;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getOrigin() {
    return origin;
  }

  public String getHeaders() {
    return headers;
  }

  public boolean getCredentials() {
    return credentials;
  }

  public String getMethods() {
    return methods;
  }

  public int getMaxAge() {
    return maxAge;
  }
}