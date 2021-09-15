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
package org.corant.shared.resource.watch;

import java.io.File;
import java.nio.file.WatchEvent.Kind;

/**
 * corant-shared
 *
 * @author bingo 下午5:26:19
 *
 */
public class PathUpdateEvent extends FileChangeEvent {

  private static final long serialVersionUID = 2309451253764638313L;

  final File original;

  public PathUpdateEvent(Kind<?> type, File file, File original) {
    super(type, file);
    this.original = original;
  }

  public File getOriginal() {
    return original;
  }

  @Override
  public String toString() {
    return "PathUpdateEvent [type=" + type + ",file=" + getFile() + ", original=" + getOriginal()
        + "]";
  }

}
