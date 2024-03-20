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

import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午4:49:49
 */
public class ByteArrayResource implements Resource {

  protected final byte[] byteArray;

  protected final String location;

  protected final String name;

  protected final Map<String, Object> metadata;

  public ByteArrayResource(byte[] byteArray) {
    this(byteArray, null);
  }

  public ByteArrayResource(byte[] byteArray, String location) {
    this(byteArray, location, null, null);
  }

  public ByteArrayResource(byte[] byteArray, String location, String name,
      Map<String, Object> metadata) {
    this.byteArray = shouldNotNull(byteArray, "Byte array can't null"); // non copy or clone
    this.location = location;
    this.name = name;
    this.metadata = metadata == null ? null : unmodifiableMap(metadata);
  }

  public ByteArrayResource(InputStream is) {
    this(is, null);
  }

  public ByteArrayResource(InputStream is, String location) {
    this(is, location, null, null);
  }

  public ByteArrayResource(InputStream is, String location, String name,
      Map<String, Object> metadata) {
    try (BufferedInputStream bis =
        new BufferedInputStream(shouldNotNull(is, "Input stream can't null"))) {
      byteArray = bis.readAllBytes();
      this.location = location;
      this.name = name;
      this.metadata = metadata == null ? null : unmodifiableMap(metadata);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public ByteArrayResource copy() {
    return new ByteArrayResource(Arrays.copyOf(byteArray, byteArray.length), location, name,
        metadata);
  }

  public final byte[] getByteArray() {
    return byteArray;
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public Map<String, Object> getMetadata() {
    return defaultObject(metadata, Collections::emptyMap);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.UNKNOWN;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return new ByteArrayInputStream(byteArray);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (ByteArrayResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Resource.super.unwrap(cls);
  }

  public ByteArrayResource withLocation(String location) {
    return new ByteArrayResource(byteArray, location, name, metadata);
  }

}
