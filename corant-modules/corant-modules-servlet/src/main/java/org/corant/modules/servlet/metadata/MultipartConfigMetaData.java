/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.servlet.metadata;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.EMPTY;
import jakarta.servlet.annotation.MultipartConfig;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午4:19:48
 *
 */
public class MultipartConfigMetaData {

  private String location = EMPTY;

  private long maxFileSize = -1L;

  private long maxRequestSize = -1L;

  private int fileSizeThreshold = 0;

  public MultipartConfigMetaData(MultipartConfig ann) {
    this(shouldNotNull(ann).location(), ann.maxFileSize(), ann.maxRequestSize(),
        ann.fileSizeThreshold());
  }

  /**
   * @param location
   * @param maxFileSize
   * @param maxRequestSize
   * @param fileSizeThreshold
   */
  public MultipartConfigMetaData(String location, long maxFileSize, long maxRequestSize,
      int fileSizeThreshold) {
    setLocation(location);
    setMaxFileSize(maxFileSize);
    setMaxRequestSize(maxRequestSize);
    setFileSizeThreshold(fileSizeThreshold);
  }

  public int getFileSizeThreshold() {
    return fileSizeThreshold;
  }

  public String getLocation() {
    return location;
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public long getMaxRequestSize() {
    return maxRequestSize;
  }

  protected void setFileSizeThreshold(int fileSizeThreshold) {
    this.fileSizeThreshold = fileSizeThreshold;
  }

  protected void setLocation(String location) {
    this.location = location;
  }

  protected void setMaxFileSize(long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  protected void setMaxRequestSize(long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
  }

}
