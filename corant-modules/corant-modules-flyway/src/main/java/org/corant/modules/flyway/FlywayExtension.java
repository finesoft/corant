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
package org.corant.modules.flyway;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.normal.Priorities.MODULES_HIGHER;
import java.util.logging.Logger;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.annotation.Priority;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-flyway
 *
 * @author bingo 上午9:56:54
 *
 */
public class FlywayExtension implements Extension {

  Logger logger = Logger.getLogger(FlywayExtension.class.getName());

  void after(@Observes @Priority(MODULES_HIGHER) AfterDeploymentValidation adv) {
    try {
      resolve(FlywayMigrator.class).migrate();
    } catch (Exception e) {
      adv.addDeploymentProblem(
          new CorantRuntimeException(e, "Flyway database migration occurred error!"));
    }
  }
}
