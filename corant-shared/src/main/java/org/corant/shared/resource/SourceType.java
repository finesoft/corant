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

import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.SLASH;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.util.Optional;

/**
 * corant-shared
 *
 * <p>
 * Object that representation of a original source of a Resource
 * </p>
 *
 * @author bingo 下午3:31:29
 *
 */
public enum SourceType {

  /**
   * load resource from file system
   */
  FILE_SYSTEM("filesystem:"),

  /**
   * load resource from class path
   */
  CLASS_PATH("classpath:"),

  /**
   * load resource from URL
   */
  URL("url:"),

  /**
   * load resource from input stream
   */
  UNKNOWN("unknown:");

  private final String prefix;
  private final int prefixLength;

  SourceType(String prefix) {
    this.prefix = prefix;
    prefixLength = prefix.length();
  }

  public static Optional<SourceType> decide(String path) {
    SourceType ps = null;
    if (isNotBlank(path)) {
      for (SourceType p : SourceType.values()) {
        if (p.match(path) && path.length() > p.prefixLength) {
          ps = p;
          break;
        }
      }
    }
    return Optional.ofNullable(ps);
  }

  public static String decideSeparator(String path) {
    return decide(path).orElse(UNKNOWN).getSeparator();
  }

  public String getPrefix() {
    return prefix;
  }

  public int getPrefixLength() {
    return prefixLength;
  }

  public String getSeparator() {
    if (this == CLASS_PATH || this == URL) {
      return SLASH;
    } else if (this == FILE_SYSTEM) {
      return File.separator;
    } else {
      return EMPTY;
    }
  }

  public boolean match(String path) {
    if (isBlank(path)) {
      return false;
    }
    return path.startsWith(prefix);
  }

  public String regulate(String path) {
    if (path != null && !path.startsWith(prefix)) {
      return prefix + path;
    }
    return path;
  }

  public String resolve(String path) {
    if (path != null && path.startsWith(prefix)) {
      return path.substring(prefixLength);
    }
    return path;
  }
}
