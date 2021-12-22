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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.defaultString;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
import java.util.Map;
import java.util.UUID;
import org.corant.shared.normal.Defaults;
import org.corant.shared.normal.Names;
import org.corant.shared.resource.ThresholdingOutputStream.SimpleDeferredFileOutputStream;
import org.corant.shared.util.Maps;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 * <p>
 * Note: Some codes come from GIXON.COM
 *
 * @author bingo 下午5:27:33
 *
 */
public class TemporaryResource implements WritableResource {

  protected static final String systemTempDir = Systems.getTempDir();
  protected static final String unknownFileName = Names.CORANT.concat("_temp");

  protected final String filename;
  protected final int memoryThreshold;
  protected final String tempDir;
  protected final Map<String, Object> metaData;
  protected SimpleDeferredFileOutputStream dfos;

  public TemporaryResource() {
    this(Defaults.FOUR_KB);
  }

  public TemporaryResource(int memoryThreshold) {
    this(null, memoryThreshold, null);
  }

  public TemporaryResource(String filename) {
    this(filename, Defaults.FOUR_KB, null);
  }

  public TemporaryResource(String filename, int memoryThreshold, String tempDir,
      Object... properties) {
    this.filename = defaultString(filename, unknownFileName);
    this.memoryThreshold = max(memoryThreshold, 0);
    this.tempDir = defaultString(tempDir, systemTempDir);
    metaData = Maps.immutableMapOf(properties);
    shouldBeTrue(new File(this.tempDir).isDirectory());
  }

  public long contentLength() throws IOException {
    return dfos != null ? dfos.getByteCount() : 0;
  }

  public String getFilename() {
    return filename;
  }

  @Override
  public String getLocation() {
    return dfos.isInMemory() ? null : dfos.getFile().getAbsolutePath();
  }

  @Override
  public Map<String, Object> getMetadata() {
    return metaData;
  }

  @Override
  public String getName() {
    return filename;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.UNKNOWN;
  }

  public long lastModified() throws IOException {
    return dfos.isInMemory() ? -1 : dfos.getFile().lastModified();// FIXME
  }

  @Override
  public InputStream openInputStream() throws IOException {
    InputStream input = null;
    if (dfos != null) {
      if (dfos.isInMemory()) {
        input = new ByteArrayInputStream(dfos.getData());
      } else {
        input = new FileInputStream(dfos.getFile());
      }
    }
    return input;
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    return dfos = new SimpleDeferredFileOutputStream(memoryThreshold, this::createTempFile);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (TemporaryResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return WritableResource.super.unwrap(cls);
  }

  protected File createTempFile() {
    File tempDir = new File(this.tempDir);
    String tempFileName = filename.concat("_").concat(UUID.randomUUID().toString());
    File tempFile = new File(tempDir, tempFileName);
    Cleaner.create().register(this, () -> {
      if (tempFile.exists()) {
        tempFile.delete();
      }
    });
    return tempFile;
  }
}
