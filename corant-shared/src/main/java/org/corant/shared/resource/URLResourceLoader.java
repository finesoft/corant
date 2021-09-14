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
package org.corant.shared.resource;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.listOf;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

/**
 * corant-shared
 *
 * Unfinished yet!
 *
 * @author bingo 下午4:30:43
 *
 */
public class URLResourceLoader implements ResourceLoader {

  protected final Proxy proxy;

  public URLResourceLoader() {
    proxy = null;
  }

  public URLResourceLoader(Proxy proxy) {
    this.proxy = proxy;
  }

  @Override
  public Collection<? extends Resource> load(Object location) throws IOException {
    shouldNotNull(location);
    URL url = null;
    if (location instanceof URL) {
      url = (URL) location;
    } else if (location instanceof URI) {
      url = ((URI) location).toURL();
    } else if (location instanceof Path) {
      url = ((Path) location).toUri().toURL();
    } else {
      url = new URL(location.toString());
    }
    if (proxy == null) {
      return listOf(new InputStreamResource(url));
    } else {
      return listOf(new InputStreamResource(url.openConnection(proxy).getInputStream(),
          url.toExternalForm(), null));
    }
  }

}
