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
package org.corant.modules.websocket;

import static org.corant.context.Beans.getBeanScope;
import static org.corant.context.Beans.manageable;
import static org.corant.context.Beans.resolve;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;
import org.corant.shared.util.Objects;

/**
 * corant-modules-websocket
 *
 * @author bingo 下午10:10:38
 *
 */
public class DefaultServerEndpointConfigurator extends Configurator {

  @Override
  public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
    if (getBeanScope(endpointClass) == null) {
      return manageable(Objects.newInstance(endpointClass));
    }
    return resolve(endpointClass);
  }

}
