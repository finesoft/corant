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
package org.corant.modules.servlet.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午7:01:09
 */
@ApplicationScoped
public abstract class AbstractCorsFilter implements Filter {

  @Inject
  @ConfigProperty(name = "corant.servlet.cors.enable", defaultValue = "false")
  protected boolean enable;
  @Inject
  @ConfigProperty(name = "corant.servlet.cors.origin", defaultValue = "*")
  protected String origin;
  @Inject
  @ConfigProperty(name = "corant.servlet.cors.headers", defaultValue = "")
  protected String headers;
  @Inject
  @ConfigProperty(name = "corant.servlet.cors.credentials", defaultValue = "false")
  protected String credentials;
  @Inject
  @ConfigProperty(name = "corant.servlet.cors.methods", defaultValue = "GET, POST, OPTIONS")
  protected String methods;
  @Inject
  @ConfigProperty(name = "corant.servlet.cors.max-age", defaultValue = "")
  protected String maxAge;

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    if (isEnable()) {
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
  public void init(FilterConfig filterConfig) {

  }

  public boolean isEnable() {
    return enable;
  }
}
