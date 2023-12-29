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
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * corant-shared
 *
 * @author bingo 上午11:20:32
 */
public class PathResource implements WritableResource {

  protected final Path path;

  protected final boolean readOnly;

  public PathResource(Path path, boolean readOnly) {
    this.path = shouldNotNull(path).normalize();
    this.readOnly = readOnly;
  }

  public PathResource(String path, boolean readOnly) {
    this(Paths.get(path).normalize(), readOnly);
  }

  public PathResource(URI path, boolean readOnly) {
    this(Paths.get(path).normalize(), readOnly);
  }

  @Override
  public boolean exists() {
    return Files.exists(path);
  }

  @Override
  public String getLocation() {
    return path.toString();
  }

  @Override
  public String getName() {
    return path.getFileName().toString();
  }

  public Path getPath() {
    return path;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.UNKNOWN;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    if (!Files.exists(path) || Files.isDirectory(path)) {
      throw new IOException(path + " doesn't exist or is a directory.");
    }
    return Files.newInputStream(path, StandardOpenOption.READ);
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    if (Files.isDirectory(path)) {
      throw new IOException(path + " is a directory.");
    }
    if (isReadOnly()) {
      throw new IOException(path + " is read only.");
    }
    return Files.newOutputStream(path);
  }

  @Override
  public ReadableByteChannel openReadableChannel() throws IOException {
    return Files.newByteChannel(path, StandardOpenOption.READ);
  }

  @Override
  public WritableByteChannel openWritableChannel(OpenOption... openOptions) throws IOException {
    if (isReadOnly()) {
      throw new IOException(path + " is read only.");
    }
    return Files.newByteChannel(path, openOptions);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (PathResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return WritableResource.super.unwrap(cls);
  }

}
