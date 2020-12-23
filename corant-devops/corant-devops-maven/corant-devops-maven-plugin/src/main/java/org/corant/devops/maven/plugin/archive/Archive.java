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
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;

/**
 * corant-devops-maven
 *
 * @author bingo 下午2:48:41
 *
 */
public interface Archive extends Iterable<Archive.Entry> {

  void addEntry(Entry entry);

  List<Archive> getChildren();

  List<Entry> getEntries(Predicate<Entry> filter) throws IOException;

  Path getPath();

  void removeEntry(Entry entry);

  interface Entry {

    InputStream getInputStream() throws IOException;

    default FileTime getLastModifiedTime() {
      return FileTime.from(Instant.now());
    }

    String getName();
  }

}
