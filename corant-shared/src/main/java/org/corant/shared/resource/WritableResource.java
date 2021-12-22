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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import org.corant.shared.util.Resources;

/**
 * corant-shared
 *
 * @author bingo 下午3:33:58
 *
 */
public interface WritableResource extends Resource {

  OutputStream openOutputStream() throws IOException;

  default WritableByteChannel openWritableChannel() throws IOException {
    return Channels.newChannel(openOutputStream());
  }

  default OutputStream tryOpenOutputStream() {
    try {
      return openOutputStream();
    } catch (IOException e) {
      Resources.logger.log(Level.WARNING, e,
          () -> String.format("Can't not open stream from %s.", getLocation()));
    }
    return null;
  }

  @Override
  default <T> T unwrap(Class<T> cls) {
    if (WritableResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Resource.super.unwrap(cls);
  }
}
