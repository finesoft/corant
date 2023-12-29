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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.trim;
import java.io.IOException;
import java.net.URL;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;

/**
 * corant-modules-webserver-undertow
 *
 * @author bingo 下午4:04:47
 */
public class UndertowClassPathResourceManager implements ResourceManager {

  private final ClassLoader classLoader;
  private final String servPrefix;
  private final String resoPrefix;

  public UndertowClassPathResourceManager(ClassLoader classLoader, String servPrefix,
      String resoPrefix) {
    this.classLoader = classLoader;
    this.servPrefix = trim(shouldNotBlank(servPrefix));
    this.resoPrefix = trim(shouldNotBlank(resoPrefix));
  }

  @Override
  public void close() throws IOException {}

  @Override
  public Resource getResource(String path) throws IOException {
    if (isBlank(path)) {
      return null;
    }
    String usePath = path;
    if (!usePath.startsWith(servPrefix)) {
      return null;
    }
    usePath = path.substring(servPrefix.length());
    String reqPath = isEmpty(usePath) ? "/" : usePath;
    if (usePath.startsWith("/")) {
      usePath = path.substring(1);
    }
    usePath = resoPrefix + usePath;
    URL resource = classLoader.getResource(usePath);
    if (resource != null) {
      return new URLResource(resource, reqPath);
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
