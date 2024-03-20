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

import static java.util.Collections.emptyMap;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.linkedHashMapOf;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;

/**
 * corant-shared
 * <p>
 * Describe system file
 *
 * @author bingo 下午6:53:19
 */
public class FileSystemResource extends URLResource implements WritableResource {

  protected final File file;

  public FileSystemResource(File file) {
    this(file, null);
  }

  public FileSystemResource(File file, Map<String, Object> metadata) {
    super(getFileUrl(file), SourceType.FILE_SYSTEM, metadata);
    this.file = shouldNotNull(file);
  }

  public FileSystemResource(Path path) {
    this(shouldNotNull(path).toFile());
  }

  public FileSystemResource(String path) {
    this(new File(shouldNotNull(path)));
  }

  public FileSystemResource(String path, Map<String, Object> metadata) {
    this(new File(shouldNotNull(path)), metadata);
  }

  public FileSystemResource(URL url) {
    this(shouldNotNull(url).getFile());
  }

  public static Map<String, Object> metadataOf(File file) {
    return linkedHashMapOf(META_SOURCE_TYPE, SourceType.FILE_SYSTEM.name(), META_NAME,
        file.getName(), META_LAST_MODIFIED, file.lastModified(), META_CONTENT_LENGTH, file.length(),
        META_CONTENT_TYPE, FileUtils.getContentType(file.getAbsolutePath()));
  }

  private static URL getFileUrl(File file) {
    try {
      return shouldNotNull(file).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new CorantRuntimeException(e);
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
    FileSystemResource other = (FileSystemResource) obj;
    if (file == null) {
      return other.file == null;
    } else {
      return file.equals(other.file);
    }
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  public File getFile() {
    return file;
  }

  public String getFileContentType() {
    return FileUtils.getContentType(getLocation());
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }

  @Override
  public String getName() {
    return file.getName();
  }

  public Map<String, String> getUserDefinedAttributes() {
    try {
      return FileUtils.getUserDefinedAttributes(file.toPath());
    } catch (IOException e) {
      return emptyMap();
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    return prime * result + (file == null ? 0 : file.hashCode());
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return Files.newInputStream(getFile().toPath());
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    return Files.newOutputStream(getFile().toPath());
  }

  @Override
  public FileChannel openReadableChannel() throws IOException {
    return FileChannel.open(file.toPath(), StandardOpenOption.READ);
  }

  @Override
  public FileChannel openWritableChannel(OpenOption... openOptions) throws IOException {
    return FileChannel.open(file.toPath(), openOptions);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (FileSystemResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return super.unwrap(cls);
  }
}
