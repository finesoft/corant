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
package org.corant.kernel.service;

import java.io.File;
import java.util.Set;

/**
 * corant-kernel
 *
 * @author bingo 下午12:01:35
 *
 */
public interface CompilationService {

  Set<String> acceptExtensions();

  void compile(Context context);

  public static class Context {
    private final String id;
    private final Set<File> classpaths;
    private final Set<File> sourceDirectories;
    private final File outputDirectory;

    /**
     * @param id
     * @param classpaths
     * @param sourceDirectories
     * @param outputDirectory
     */
    public Context(String id, Set<File> classpaths, Set<File> sourceDirectories,
        File outputDirectory) {
      super();
      this.id = id;
      this.classpaths = classpaths;
      this.sourceDirectories = sourceDirectories;
      this.outputDirectory = outputDirectory;
    }

    public Set<File> getClasspaths() {
      return classpaths;
    }

    public String getId() {
      return id;
    }

    public File getOutputDirectory() {
      return outputDirectory;
    }

    public Set<File> getSourceDirectories() {
      return sourceDirectories;
    }

  }
}
