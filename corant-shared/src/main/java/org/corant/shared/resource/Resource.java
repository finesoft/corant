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

import static org.corant.shared.util.Maps.immutableMapOf;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.logging.Level;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Streams;

/**
 * corant-shared
 *
 * <p>
 * Object that representation of a resource that can be loaded from URL, class, file system or input
 * stream.
 * </p>
 *
 * @author bingo 下午3:19:30
 *
 */
public interface Resource {

  String META_CONTENT_TYPE = "Content-Type";
  String META_CONTENT_LENGTH = "Content-Length";
  String META_LAST_MODIFIED = "Last-Modified";
  String META_SOURCE_TYPE = "Source-Type";
  String META_NAME = "Name";

  /**
   * Determine whether this resource actually exists in physical form.
   */
  default boolean exists() {
    return true;
  }

  /**
   * Return a byte array for the content of this resource, please evaluate the size of the resource
   * when using it to avoid OOM.
   *
   * NOTE: the stream will be closed after reading.
   *
   * @throws IOException If I/O errors occur
   */
  default byte[] getBytes() throws IOException {
    try (InputStream is = openInputStream()) {
      return Streams.readAllBytes(is);
    }
  }

  /**
   * Return the location of this resource, depends on original source. Depending on source type,
   * this may be:
   * <ul>
   * <li>FILE_SYSTEM - absolute path to the file</li>
   * <li>CLASS_PATH - class resource path</li>
   * <li>URL - string of the URI</li>
   * <li>UNKNOWN - whatever location was provided to {@link InputStreamResource}</li>
   * </ul>
   */
  String getLocation();

  /**
   * Return the meta information of this resource. For example: author, date created and date
   * modified,size etc.
   *
   * @return getMetadata
   */
  default Map<String, Object> getMetadata() {
    return immutableMapOf(META_SOURCE_TYPE, getSourceType() == null ? null : getSourceType().name(),
        META_NAME, getName());
  }

  /**
   * The name of this resource. this may be:
   * <ul>
   * <li>FILE_SYSTEM - the underlying file name</li>
   * <li>CLASS_PATH - the underlying class path resource name</li>
   * <li>URL - the file name of this URL {@link URL#getFile()}</li>
   * <li>UNKNOWN - whatever name was provided to {@link InputStreamResource}</li>
   * </ul>
   *
   * @return getName
   */
  default String getName() {
    return null;
  }

  /**
   * Return the original source type
   *
   * @return getSourceType
   */
  SourceType getSourceType();

  /**
   * Return an {@link InputStream} for the content of this resource
   */
  InputStream openInputStream() throws IOException;

  /**
   * Return a channel that reads bytes from {@link #openInputStream()}.
   */
  default ReadableByteChannel openReadableChannel() throws IOException {
    return Channels.newChannel(openInputStream());
  }

  /**
   * Return an {@link InputStream} for the content of the resource, do not throw any exceptions.
   *
   */
  default InputStream tryOpenInputStream() {
    try {
      return openInputStream();
    } catch (IOException e) {
      Resources.logger.log(Level.WARNING, e,
          () -> String.format("Can't not open stream from %s.", getLocation()));
    }
    return null;
  }

  default <T> T unwrap(Class<T> cls) {
    if (Resource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap resource to " + cls);
  }
}
