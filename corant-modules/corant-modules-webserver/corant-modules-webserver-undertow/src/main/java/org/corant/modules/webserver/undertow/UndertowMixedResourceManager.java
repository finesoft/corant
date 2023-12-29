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
package org.corant.modules.webserver.undertow;

import static org.corant.shared.util.Lists.immutableList;
import static org.corant.shared.util.Lists.immutableListOf;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * corant-modules-webserver-undertow
 *
 * @author bingo 下午2:55:38
 */
public class UndertowMixedResourceManager implements ResourceManager {

  private final List<ResourceManager> resourceManagers;

  public UndertowMixedResourceManager(Collection<ResourceManager> managers) {
    resourceManagers = immutableList(managers);
  }

  public UndertowMixedResourceManager(ResourceManager... managers) {
    resourceManagers = immutableListOf(managers);
  }

  @Override
  public void close() throws IOException {
    for (ResourceManager resourceManager : resourceManagers) {
      resourceManager.close();
    }
  }

  @Override
  public Resource getResource(String path) throws IOException {
    for (ResourceManager resourceManager : resourceManagers) {
      Resource resource = resourceManager.getResource(path);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }

  @Override
  public boolean isResourceChangeListenerSupported() {
    return false;
  }

  @Override
  public void registerResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
  }

  @Override
  public void removeResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
  }

}
