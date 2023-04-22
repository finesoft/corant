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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;
import io.smallrye.health.SmallRyeHealth;

/**
 * corant-modules-microprofile-healthcheck
 *
 * @author bingo 下午7:05:39
 *
 */
@ApplicationScoped
@WebServlet(name = "SmallRyeStartupServlet",
    urlPatterns = "${corant.microprofile.health-check.startup.endpoint.url:/health/started}")
@RequiredConfiguration(key = "corant.microprofile.health-check.startup.endpoint.enable",
    predicate = ValuePredicate.EQ, value = "true", type = Boolean.class)
public class SmallRyeStartupServlet extends AbstractSmallRyeHealthServlet {

  private static final long serialVersionUID = 7803381146340213889L;

  @Override
  protected SmallRyeHealth getHealth(HttpServletRequest req, HttpServletResponse resp) {
    return reporter.getStartup();
  }
}
