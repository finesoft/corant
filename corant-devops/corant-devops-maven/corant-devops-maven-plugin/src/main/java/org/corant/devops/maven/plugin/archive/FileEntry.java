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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Objects;
import org.corant.devops.maven.plugin.archive.Archive.Entry;

/**
 * corant-devops-maven
 *
 * @author bingo 下午3:43:43
 *
 */
public class FileEntry implements Entry {

  private final File file;

  FileEntry(File file) {
    this.file = Objects.requireNonNull(file);
  }

  public static FileEntry of(File file) {
    return new FileEntry(file);
  }

  public File getFile() {
    return file;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public FileTime getLastModifiedTime() {
    try {
      return Files.getLastModifiedTime(file.toPath());
    } catch (IOException e) {
      return FileTime.from(Instant.now());
    }
  }

  @Override
  public String getName() {
    return file.getName();
  }

}
