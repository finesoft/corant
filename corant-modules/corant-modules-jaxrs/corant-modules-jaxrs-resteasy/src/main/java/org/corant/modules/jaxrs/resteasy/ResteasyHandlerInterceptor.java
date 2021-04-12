/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jaxrs.resteasy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * corant-modules-jaxrs-resteasy
 *
 * <p>
 * Workflow interface that allows for customized handler execution chains. Applications can declare
 * any number of existing or custom interceptors for certain groups of handlers, to add common
 * preprocessing behavior without needing to modify each handler implementation.
 *
 * @author bingo 下午4:12:53
 *
 */
public interface ResteasyHandlerInterceptor {

  /**
   * Callback after completion of the JAXRS worker servlet (dispatcher servlet) do service. Will be
   * called on any outcome of handler execution, thus allows for proper resource cleanup.
   *
   * @param success whether the request was successfully processed
   * @param httpMethod the HTTP method currently called
   * @param request the HTTP request currently called
   * @param response the HTTP response currently called
   */
  void afterCompletion(boolean success, String httpMethod, HttpServletRequest request,
      HttpServletResponse response);

  /**
   * Intercept the execution of a handler. Called after the JAXRS worker servlet (dispatcher
   * servlet) do service.
   *
   * <p>
   * Note: If the the JAXRS worker servlet (dispatcher servlet) execution service throws an
   * exception, this method will not be called
   *
   *
   * @param httpMethod the HTTP method currently called
   * @param request the HTTP request currently called
   * @param response the HTTP response currently called
   *
   * @see #afterCompletion(String, HttpServletRequest, HttpServletResponse)
   */
  void postHandle(String httpMethod, HttpServletRequest request, HttpServletResponse response);

  /**
   * Intercept the execution of a handler. Called before the JAXRS worker servlet (dispatcher
   * servlet) do service.
   *
   * @param httpMethod the HTTP method currently called
   * @param request the HTTP request currently called
   * @param response the HTTP response currently called
   * @return {@code true} if the execution chain should proceed with the next interceptor or the
   *         handler itself. Else, the JAXRS worker servlet (dispatcher servlet) assumes that this
   *         interceptor has already dealt with the response itself.
   */
  default boolean preHandle(String httpMethod, HttpServletRequest request,
      HttpServletResponse response) {
    return true;
  }

  /**
   * corant-modules-jaxrs-resteasy
   *
   * <p>
   * Convenient adapter class, mainly used to be inherited. the {@code preHandle} method returns
   * {@code true}! by default.
   *
   *
   * @author bingo 下午5:53:33
   *
   */
  abstract class HandlerInterceptorAdapter implements ResteasyHandlerInterceptor {

    @Override
    public void afterCompletion(boolean success, String httpMethod, HttpServletRequest request,
        HttpServletResponse response) {}

    @Override
    public void postHandle(String httpMethod, HttpServletRequest request,
        HttpServletResponse response) {}

    @Override
    public boolean preHandle(String httpMethod, HttpServletRequest request,
        HttpServletResponse response) {
      return true;
    }

  }

}
