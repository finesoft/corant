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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.Checksum;

/**
 * corant-shared
 *
 * @author bingo 下午8:53:04
 */
public class ChecksumInputStream extends FilterInputStream {

  protected final Checksum checksum;
  protected volatile boolean on = true;

  public ChecksumInputStream(InputStream delegate, Checksum checksum) {
    super(delegate);
    this.checksum = shouldNotNull(checksum);
  }

  public static DigestInputStream digestInputStreamOf(InputStream is, MessageDigest md) {
    return new DigestInputStream(is, md);
  }

  public void on(boolean on) {
    this.on = on;
  }

  @Override
  public int read() throws IOException {
    int ret = in.read();
    if (on && ret != -1) {
      checksum.update(ret);
    }
    return ret;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int ret = in.read(b, off, len);
    if (on && ret != -1) {
      checksum.update(b, off, ret);
    }
    return ret;
  }
}
