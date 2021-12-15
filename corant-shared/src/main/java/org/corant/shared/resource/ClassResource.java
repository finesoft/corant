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

import java.net.URL;
import org.corant.shared.util.Classes;

/**
 * corant-shared
 *
 * Describe class resource, but doesn't load it right away.
 *
 * @author bingo 下午2:04:09
 *
 */
public class ClassResource extends ClassPathResource {

  protected final String className;

  public ClassResource(String classPath, ClassLoader classLoader, URL url) {
    super(classPath, classLoader, url);
    String useClassPath = classPath;
    if (useClassPath.contains(ClassPathResourceScanner.JAR_URL_SEPARATOR)) {
      useClassPath =
          useClassPath.substring(classPath.indexOf(ClassPathResourceScanner.JAR_URL_SEPARATOR)
              + ClassPathResourceScanner.JAR_URL_SEPARATOR.length());
    }
    if (useClassPath.contains(ClassPathResourceScanner.CLASSES_FOLDER)) {
      useClassPath =
          useClassPath.substring(useClassPath.indexOf(ClassPathResourceScanner.CLASSES_FOLDER)
              + ClassPathResourceScanner.CLASSES_FOLDER.length());
    }
    int classNameEnd = useClassPath.length() - Classes.CLASS_FILE_NAME_EXTENSION.length();
    className = useClassPath.substring(0, classNameEnd)
        .replace(ClassPathResourceScanner.PATH_SEPARATOR, Classes.PACKAGE_SEPARATOR_CHAR);
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
    ClassResource other = (ClassResource) obj;
    if (className == null) {
      return other.className == null;
    } else {
      return className.equals(other.className);
    }
  }

  public String getClassName() {
    return className;
  }

  public String getPackageName() {
    return Classes.getPackageName(className);
  }

  public String getSimpleName() {
    return Classes.getShortClassName(className);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    return prime * result + (className == null ? 0 : className.hashCode());
  }

  public Class<?> load() {
    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

}
