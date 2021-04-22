package org.corant.modules.jaxrs.resteasy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;

/**
 * corant-modules-jaxrs-resteasy
 *
 * 跨域处理 使用官方CorsFilter
 *
 * @author don
 * @date 2020-05-28
 */
@Provider
@ApplicationScoped
public class ResteasyCorsProvider implements Feature {

  @Inject
  @ConfigProperty(name = "corant.rs.cors.enable", defaultValue = "false")
  protected boolean enable;
  @Inject
  @ConfigProperty(name = "corant.rs.cors.origin", defaultValue = "*")
  protected String origin;
  @Inject
  @ConfigProperty(name = "corant.rs.cors.headers", defaultValue = "")
  protected String headers;
  @Inject
  @ConfigProperty(name = "corant.rs.cors.credentials", defaultValue = "false")
  protected boolean credentials;
  @Inject
  @ConfigProperty(name = "corant.rs.cors.methods", defaultValue = "GET, POST, OPTIONS")
  protected String methods;
  @Inject
  @ConfigProperty(name = "corant.rs.cors.maxAge", defaultValue = "86400")
  protected int maxAge;

  @Override
  public boolean configure(FeatureContext context) {
    if (isEnable()) {
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

  public boolean getCredentials() {
    return credentials;
  }

  public String getHeaders() {
    return headers;
  }

  public int getMaxAge() {
    return maxAge;
  }

  public String getMethods() {
    return methods;
  }

  public String getOrigin() {
    return origin;
  }

  public boolean isEnable() {
    return enable;
  }
}
