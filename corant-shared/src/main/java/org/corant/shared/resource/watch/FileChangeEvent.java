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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.io.File;
import java.nio.file.WatchEvent.Kind;
import java.util.EventObject;

/**
 * corant-shared
 *
 * @author bingo 下午5:26:19
 *
 */
public class FileChangeEvent extends EventObject {

  private static final long serialVersionUID = 2309451253764638313L;

  final FileChangeType type;

  public FileChangeEvent(Kind<?> type, File file) {
    super(file);
    if (type == ENTRY_CREATE) {
      this.type = FileChangeType.CREATE;
    } else if (type == ENTRY_DELETE) {
      this.type = FileChangeType.DELETE;
    } else if (type == ENTRY_MODIFY) {
      this.type = FileChangeType.MODIFY;
    } else {
      this.type = FileChangeType.UNKNOWN;
    }
  }

  public File getFile() {
    return (File) source;
  }

  @Override
  public File getSource() {
    return (File) super.getSource();
  }

  public FileChangeType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "FileChangeEvent [type=" + type + ",file=" + getFile() + "]";
  }

}
