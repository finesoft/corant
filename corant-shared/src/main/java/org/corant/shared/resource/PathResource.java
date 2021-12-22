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
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.sizeOf;
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
import java.util.Arrays;

/**
 * corant-shared
 *
 * @author bingo 上午11:20:32
 *
 */
public class PathResource implements WritableResource {

  public static final OpenOption[] EMPTY_ARRAY = {};

  protected final OpenOption[] openOptions;

  protected final Path path;

  public PathResource(Path path, OpenOption... ops) {
    this.path = shouldNotNull(path).normalize();
    openOptions = isEmpty(ops) ? EMPTY_ARRAY : Arrays.copyOf(ops, ops.length);
  }

  public PathResource(String path, OpenOption... ops) {
    this(Paths.get(path).normalize(), ops);
  }

  public PathResource(URI path, OpenOption... ops) {
    this(Paths.get(path).normalize(), ops);
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

  @Override
  public InputStream openInputStream() throws IOException {
    if (!Files.exists(path) || Files.isDirectory(path)) {
      throw new IOException(path + " doesn't exist or is a directory.");
    }
    return Files.newInputStream(path, openOptions);
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
  public WritableByteChannel openWritableChannel() throws IOException {
    if (isReadOnly()) {
      throw new IOException(path + " is read only.");
    }
    return Files.newByteChannel(path, StandardOpenOption.WRITE);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (PathResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return WritableResource.super.unwrap(cls);
  }

  protected boolean isReadOnly() {
    return sizeOf(openOptions) == 1 && openOptions[0] == StandardOpenOption.READ;
  }
}
