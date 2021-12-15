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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * Describe class path resource include class resource.
 *
 * @author bingo 下午2:04:58
 *
 */
public class ClassPathResource extends URLResource {

  protected final ClassLoader classLoader;
  protected final String classPath;

  public ClassPathResource(String classPath, ClassLoader classLoader, URL url) {
    super(url, SourceType.CLASS_PATH);
    this.classLoader = shouldNotNull(classLoader);
    this.classPath = shouldNotNull(classPath);
  }

  public static ClassPathResource of(String classPath, ClassLoader classLoader, URL url) {
    if (classPath.endsWith(Classes.CLASS_FILE_NAME_EXTENSION)) {
      return new ClassResource(classPath, classLoader, url);
    } else {
      return new ClassPathResource(classPath, classLoader, url);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ClassPathResource other = (ClassPathResource) obj;
    if (classLoader == null) {
      if (other.classLoader != null) {
        return false;
      }
    } else if (!classLoader.equals(other.classLoader)) {
      return false;
    }
    if (classPath == null) {
      return other.classPath == null;
    } else {
      return classPath.equals(other.classPath);
    }
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public String getClassPath() {
    return classPath;
  }

  @Override
  public String getLocation() {
    return classPath;
  }

  /*
   * @Override public final URL getURL() { if (url == null) { synchronized (this) { if (url == null)
   * { url = classLoader.getResource(classPath); } } } return url; }
   */

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (classLoader == null ? 0 : classLoader.hashCode());
    return prime * result + (classPath == null ? 0 : classPath.hashCode());
  }

  @Override
  public final InputStream openInputStream() throws IOException {
    URLConnection conn = getURL().openConnection();
    if (Systems.getOsName().toLowerCase(Locale.getDefault()).startsWith("window")) {
      conn.setUseCaches(false);
    }
    return conn.getInputStream();
  }

}
