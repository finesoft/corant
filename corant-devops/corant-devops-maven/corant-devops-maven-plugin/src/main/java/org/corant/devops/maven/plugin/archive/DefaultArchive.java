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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * corant-devops-maven
 *
 * @author bingo 下午3:10:46
 *
 */
public class DefaultArchive implements Archive {

  private final List<Entry> entries = new ArrayList<>();
  private final List<Archive> children = new ArrayList<>();
  private final String pathName;

  DefaultArchive(String name, Archive parent) {
    if (parent != null) {
      parent.getChildren().add(this);
      String _pathName = parent.getPathName() + name + "/";
      pathName = _pathName.startsWith("/") ? _pathName.substring(1) : _pathName;
    } else {
      pathName = name + "/";
    }
  }

  public static DefaultArchive of(String name, Archive parent) {
    return new DefaultArchive(name, parent);
  }

  public static DefaultArchive root() {
    return new DefaultArchive("", null);
  }

  public void addEntries(List<Entry> entries) {
    if (entries != null) {
      this.entries.addAll(entries);
    }
  }

  @Override
  public void addEntry(Entry entry) {
    entries.add(entry);
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
  public String getPathName() {
    return pathName;
  }

  @Override
  public Iterator<Entry> iterator() {
    return entries.iterator();
  }

  @Override
  public void removeEntry(Entry entry) {
    entries.remove(entry);
  }
}
