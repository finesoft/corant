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
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * corant-shared
 * <p>
 * Describe input stream resource, can specify a specific name.
 *
 * @author bingo 下午6:54:04
 */
public class InputStreamResource implements Resource {

  protected final SourceType sourceType = SourceType.UNKNOWN;
  protected final String name;
  protected final String location;
  protected final InputStream inputStream;
  protected final Map<String, Object> metadata;

  public InputStreamResource(InputStream inputStream) {
    this(inputStream, null);
  }

  public InputStreamResource(InputStream inputStream, String name) {
    this(inputStream, null, name);
  }

  public InputStreamResource(InputStream inputStream, String location, String name) {
    this(inputStream, location, name, null);
  }

  public InputStreamResource(InputStream inputStream, String location, String name,
      Map<String, Object> metadata) {
    this.inputStream = shouldNotNull(inputStream, "Input stream can't null");
    this.name = name;
    this.location = location;
    this.metadata = metadata == null ? null : unmodifiableMap(metadata);
  }

  public InputStreamResource(Map<String, Object> metadata, InputStream inputStream) {
    this(inputStream, getMapString(metadata, "location"), getMapString(metadata, "name"), metadata);
  }

  public InputStreamResource(URL url) throws IOException {
    this(shouldNotNull(url, "URL can't null").openStream(), url.toExternalForm(), url.getFile(),
        null);
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
