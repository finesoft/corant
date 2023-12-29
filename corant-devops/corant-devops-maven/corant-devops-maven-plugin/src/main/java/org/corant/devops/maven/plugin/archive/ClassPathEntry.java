/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.devops.maven.plugin.archive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.corant.devops.maven.plugin.archive.Archive.Entry;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 下午3:47:04
 */
public class ClassPathEntry implements Entry {

  private final String name;

  private final String resourcePath;

  ClassPathEntry(String resourcePath, String name) {
    this.name = Objects.requireNonNull(name);
    this.resourcePath = Objects.requireNonNull(resourcePath);
  }

  public static ClassPathEntry of(String resourcePath, String name) {
    return new ClassPathEntry(resourcePath, name);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return getClass().getClassLoader().getResourceAsStream(resourcePath);
  }

  @Override
  public String getName() {
    return name;
  }

}
