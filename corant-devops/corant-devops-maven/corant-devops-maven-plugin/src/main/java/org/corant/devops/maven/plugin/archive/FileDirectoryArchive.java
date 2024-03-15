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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 下午3:10:46
 */
public class FileDirectoryArchive implements Archive {

  private final Path path;
  private final File file;
  private final List<Archive> children = new ArrayList<>();
  private final List<Entry> entries = new ArrayList<>();

  FileDirectoryArchive(File file, Archive parent) {
    this.file = file;
    if (parent != null) {
      path = parent.getPath().resolve(file.getName());
      parent.getChildren().add(this);
    } else {
      path = Paths.get(file.getName());
    }
    File[] subFiles = file.listFiles();
    if (subFiles != null) {
      for (File subFile : subFiles) {
        if (subFile.exists() && subFile.canRead()) {
          if (subFile.isDirectory()) {
            new FileDirectoryArchive(subFile, this);
          } else if (subFile.isFile()) {
            entries.add(FileEntry.of(subFile));
          }
        }
      }
    }
  }

  public static FileDirectoryArchive of(File file, Archive parent) {
    return new FileDirectoryArchive(file, parent);
  }

  @Override
  public void addEntry(Entry entry) {
    throw new IllegalAccessError();
  }

  @Override
  public List<Archive> getChildren() {
    return children;
  }

  @Override
  public List<Entry> getEntries(Predicate<Entry> filter) throws IOException {
    if (filter != null) {
      return entries.stream().filter(filter).collect(Collectors.toList());
    } else {
      return entries;
    }
  }

  @Override
  public String getName() {
    return file.getName();
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public Iterator<Entry> iterator() {
    return entries.iterator();
  }

  @Override
  public void removeEntry(Entry entry) {
    throw new IllegalAccessError();
  }

}
