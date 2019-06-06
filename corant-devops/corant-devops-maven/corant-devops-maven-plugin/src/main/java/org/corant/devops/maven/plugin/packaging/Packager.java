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
package org.corant.devops.maven.plugin.packaging;

import java.io.File;
import java.nio.file.Path;
import bin.JarLauncher;

/**
 * corant-devops-maven
 *
 * @author bingo 下午12:00:22
 *
 */
public interface Packager {

  String META_INF_DIR = "META-INF";
  String MF_NME = "MANIFEST.MF";

  PackageMojo getMojo();

  void pack() throws Exception;

  default String resolveApplicationName() {
    return getMojo().getFinalName() == null ? JarLauncher.DFLT_APP_NAME : getMojo().getFinalName();
  }

  default String resolveArchivePath(Path path, String... others) {
    if (path == null) {
      return null;
    }
    Path usePath = path;
    for (String other : others) {
      usePath = path.resolve(other);
    }
    return usePath.toString().replace(File.separatorChar, '/');
  }
}
