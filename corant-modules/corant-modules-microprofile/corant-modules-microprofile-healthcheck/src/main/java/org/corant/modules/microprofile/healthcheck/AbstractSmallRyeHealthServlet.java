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
package org.corant.modules.microprofile.healthcheck;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

/**
 * corant-modules-microprofile-healthcheck
 *
 * @author bingo 下午6:39:12
 *
 */
public abstract class AbstractSmallRyeHealthServlet extends HttpServlet {

  private static final long serialVersionUID = 7023127751991208939L;

  @Inject
  protected SmallRyeHealthReporter reporter;

  @Inject
  protected Logger logger;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    SmallRyeHealth health = getHealth(req, resp);
    if (health.isDown()) {
      resp.setStatus(503);
    }
    try {
      reporter.reportHealth(resp.getOutputStream(), health);
    } catch (IOException ioe) {
      logger.log(Level.SEVERE, ioe, () -> "Health reporter occurred error!");
      resp.setStatus(500);
    }
  }

  protected abstract SmallRyeHealth getHealth(HttpServletRequest req, HttpServletResponse resp);

}
