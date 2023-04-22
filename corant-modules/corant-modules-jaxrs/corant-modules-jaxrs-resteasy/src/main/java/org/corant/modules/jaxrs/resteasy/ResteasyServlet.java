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

import static org.corant.shared.util.Classes.getUserClass;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.corant.shared.util.Iterables;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 下午4:02:25
 *
 */
@WebServlet(asyncSupported = true, value = "/RESTEASY_HttpServlet30Dispatcher")
public class ResteasyServlet extends HttpServlet30Dispatcher {

  private static final long serialVersionUID = 4193833295294785852L;

  protected static final Logger logger = Logger.getLogger(ResteasyServlet.class.getName());

  @Override
  public void service(String httpMethod, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    boolean success = false;
    try {
      if (preService(httpMethod, request, response)) {
        super.service(httpMethod, request, response);
        postService(httpMethod, request, response);
        success = true;
      }
    } finally {
      completeService(success, httpMethod, request, response);
    }
  }

  protected void completeService(boolean success, String httpMethod, HttpServletRequest request,
      HttpServletResponse response) {
    resolveHanders().forEach(h -> {
      try {
        h.afterCompletion(success, httpMethod, request, response);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e,
            () -> String.format("%s handle after service completely occurred error!",
                getUserClass(h).getName()));
      }
    });
  }

  protected void postService(String httpMethod, HttpServletRequest request,
      HttpServletResponse response) {
    resolveHanders().forEach(h -> {
      try {
        h.postHandle(httpMethod, request, response);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, () -> String.format("%s handle post service occurred error!",
            getUserClass(h).getName()));
      }
    });
  }

  protected boolean preService(String httpMethod, HttpServletRequest request,
      HttpServletResponse response) {
    // return resolveHanders().map(h -> h.preHandle(httpMethod, request, response))
    // .reduce(Boolean::logicalAnd).orElse(Boolean.TRUE);
    boolean next = true;
    for (ResteasyHandlerInterceptor h : resolveHanders()) {
      try {
        next = h.preHandle(httpMethod, request, response);
      } catch (Exception e) {
        next = false;
        logger.log(Level.SEVERE, e, () -> String.format("%s handle pre service occurred error!",
            getUserClass(h).getName()));
      } finally {
        if (!next) {
          break;
        }
      }
    }
    return next;
  }

  protected Iterable<ResteasyHandlerInterceptor> resolveHanders() {
    try {
      Instance<ResteasyHandlerInterceptor> handlers =
          CDI.current().select(ResteasyHandlerInterceptor.class);
      if (!handlers.isUnsatisfied()) {
        return handlers;
      }
    } catch (IllegalStateException e) {
      logger.log(Level.WARNING, e,
          () -> "The CDI is not enabled, so can't resolve the ResteasyHandlerInterceptors!");
    }
    return Iterables.emptyIterable();
  }
}
