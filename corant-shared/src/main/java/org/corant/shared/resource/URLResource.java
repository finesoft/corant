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
import static org.corant.shared.util.Maps.immutableMapOf;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;

/**
 * corant-shared
 * <p>
 * A representation of a resource that load from URL
 *
 * @author bingo 下午4:06:15
 *
 */
public class URLResource implements Resource {
  protected final SourceType sourceType;
  protected final URL url;

  public URLResource(String url) throws MalformedURLException {
    this(new URL(url), SourceType.URL);
  }

  public URLResource(URL url) {
    this(url, SourceType.URL);
  }

  URLResource(URL url, SourceType sourceType) {
    this.url = shouldNotNull(url);
    this.sourceType = shouldNotNull(sourceType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    URLResource other = (URLResource) obj;
    return getURI().equals(other.getURI()); // FIXME URI hq
  }

  @Override
  public String getLocation() {
    return url.toExternalForm();
  }

  @Override
  public Map<String, Object> getMetadata() {
    return immutableMapOf(META_NAME, getName(), META_SOURCE_TYPE,
        sourceType == null ? null : sourceType.name());
  }

  @Override
  public String getName() {
    return FileUtils.getFileName(url.getPath());
  }

  @Override
  public SourceType getSourceType() {
    return sourceType;
  }

  public URI getURI() {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public URL getURL() {
    return url;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + getURI().hashCode();
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return url.openStream();
  }

  @Override
  public ReadableByteChannel openReadableChannel() throws IOException {
    URI uri = getURI();
    if ("file".equals(uri.getScheme())) {
      File file = new File(uri.getSchemeSpecificPart());
      if (file.exists() && file.canRead()) {
        return FileChannel.open(file.toPath(), StandardOpenOption.READ);
      }
    }
    return Resource.super.openReadableChannel();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [sourceType=" + sourceType + ", url=" + url + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (URLResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Resource.super.unwrap(cls);
  }

}
