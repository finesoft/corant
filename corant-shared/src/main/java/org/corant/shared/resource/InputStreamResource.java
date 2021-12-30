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

import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.immutableMapOf;
import static org.corant.shared.util.Maps.mapOf;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * corant-shared
 *
 * Describe input stream resource, can specify a specific name.
 *
 * @author bingo 下午6:54:04
 *
 */
public class InputStreamResource implements Resource {

  protected final SourceType sourceType = SourceType.UNKNOWN;
  protected final String name;
  protected final String location;
  protected final InputStream inputStream;
  protected final Map<String, Object> metadata;

  public InputStreamResource(InputStream inputStream, String name) {
    this.name = name;
    this.inputStream = inputStream;
    location = null;
    metadata = resolveMetadata(name, null);
  }

  public InputStreamResource(InputStream inputStream, String location, String name) {
    this.name = name;
    this.location = location;
    this.inputStream = inputStream;
    metadata = resolveMetadata(name, null);
  }

  public InputStreamResource(InputStream inputStream, String location, String name,
      Map<String, Object> metadata) {
    this.inputStream = inputStream;
    this.name = name;
    this.location = location;
    this.metadata = resolveMetadata(name, metadata);
  }

  public InputStreamResource(Map<String, Object> metadata, InputStream inputStream) {
    name = getMapString(metadata, "name");
    location = getMapString(metadata, "location");
    this.metadata = resolveMetadata(name, metadata);
    this.inputStream = inputStream;
  }

  public InputStreamResource(URL url) throws IOException {
    location = url.toExternalForm();
    inputStream = url.openStream();
    name = url.getFile();
    metadata = immutableMapOf(META_NAME, url.getFile());
  }

  protected static Map<String, Object> resolveMetadata(String name, Map<String, Object> metadata) {
    Map<String, Object> temp = mapOf(META_NAME, name);
    if (metadata != null) {
      temp.putAll(metadata);
    }
    return Collections.unmodifiableMap(temp);
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceType getSourceType() {
    return sourceType;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return inputStream;
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (InputStreamResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Resource.super.unwrap(cls);
  }

}
