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
package org.corant.modules.ddd.shared.unitwork;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.modules.ddd.shared.annotation.CMDS.CMDSLiteral;
import org.corant.shared.normal.Priorities;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午10:08:26
 *
 */
public class CommanderExtension implements Extension {

  static final boolean USE_COMMAND_PATTERN = ConfigProvider.getConfig()
      .getOptionalValue("corant.ddd.unitofwork.command-pattern.enable", Boolean.class)
      .orElse(Boolean.FALSE);

  public void arrange(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) @WithAnnotations({
      org.corant.modules.ddd.annotation.Commands.class}) ProcessAnnotatedType<?> event) {
    if (USE_COMMAND_PATTERN) {
      Class<?> cls = event.getAnnotatedType().getJavaClass();
      if (CommandHandler.class.isAssignableFrom(cls)) {
        Type[] gs = cls.getGenericInterfaces();
        if (gs.length > 0 && gs[0] instanceof ParameterizedType) {
          event.configureAnnotatedType().add(
              CMDSLiteral.of((Class<?>) ((ParameterizedType) gs[0]).getActualTypeArguments()[0]));
        }
      }
    }
  }
}
