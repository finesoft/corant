/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.servlet.abstraction;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-servlet
 *
 * @author bingo 下午7:01:09
 *
 */
@ApplicationScoped
public abstract class AbstractCorsFilter implements Filter {

  @Inject
  @ConfigProperty(name = "servlet.cors.enabled", defaultValue = "false")
  protected boolean enabled;
  @Inject
  @ConfigProperty(name = "servlet.cors.origin", defaultValue = "*")
  protected String origin;
  @Inject
  @ConfigProperty(name = "servlet.cors.headers", defaultValue = "")
  protected String headers;
  @Inject
  @ConfigProperty(name = "servlet.cors.credentials", defaultValue = "false")
  protected String credentials;
  @Inject
  @ConfigProperty(name = "servlet.cors.methods", defaultValue = "GET, POST, OPTIONS")
  protected String methods;
  @Inject
  @ConfigProperty(name = "servlet.cors.maxAge", defaultValue = "")
  protected String maxAge;

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain filterChain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    if (isEnabled()) {
      response.addHeader("Access-Control-Allow-Origin", getOrigin());
      response.addHeader("Access-Control-Allow-Headers", getHeaders());
      response.addHeader("Access-Control-Allow-Credentials", getCredentials());
      response.addHeader("Access-Control-Allow-Methods", getMethods());
      response.addHeader("Access-Control-Max-Age", getMaxAge());
    }
  }

  /**
   *
   * @return the credentials
   */
  public String getCredentials() {
    return credentials;
  }

  /**
   *
   * @return the headers
   */
  public String getHeaders() {
    return headers;
  }

  /**
   *
   * @return the maxAge
   */
  public String getMaxAge() {
    return maxAge;
  }

  /**
   *
   * @return the methods
   */
  public String getMethods() {
    return methods;
  }

  /**
   *
   * @return the origin
   */
  public String getOrigin() {
    return origin;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  /**
   *
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

}
