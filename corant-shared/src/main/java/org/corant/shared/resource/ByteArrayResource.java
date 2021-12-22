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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午4:49:49
 *
 */
public class ByteArrayResource implements Resource {

  protected final byte[] byteArray;

  protected final String location;

  public ByteArrayResource(byte[] byteArray) {
    this(byteArray, null);
  }

  public ByteArrayResource(byte[] byteArray, String location) {
    this.byteArray = byteArray; // non copy or clone
    this.location = location;
  }

  public ByteArrayResource(InputStream is) {
    this(is, null);
  }

  public ByteArrayResource(InputStream is, String location) {
    try (BufferedInputStream bis = new BufferedInputStream(is)) {
      byteArray = bis.readAllBytes();
      this.location = location;
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public ByteArrayResource clone() throws CloneNotSupportedException {
    return new ByteArrayResource(Arrays.copyOf(byteArray, byteArray.length), location);
  }

  public final byte[] getByteArray() {
    return byteArray;
  }

  @Override
  public String getLocation() {
    return location;
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
    return new ByteArrayResource(byteArray, location);
  }

}
