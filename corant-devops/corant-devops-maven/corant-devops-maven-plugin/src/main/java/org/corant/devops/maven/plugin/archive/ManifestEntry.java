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
package org.corant.devops.maven.plugin.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.corant.devops.maven.plugin.archive.Archive.Entry;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 上午10:03:17
 */
public class ManifestEntry implements Entry {

  private final Manifest manifest;

  ManifestEntry(Manifest manifest) {
    this.manifest = Objects.requireNonNull(manifest);
  }

  public static ManifestEntry of(Consumer<Attributes> consumer) {
    Manifest manifest = new Manifest();
    Attributes atts = manifest.getMainAttributes();
    atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    consumer.accept(atts);
    return new ManifestEntry(manifest);
  }

  public static ManifestEntry of(Manifest manifest) {
    return new ManifestEntry(manifest);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    manifest.write(out);
    return new ByteArrayInputStream(out.toByteArray());
  }

  @Override
  public String getName() {
    return "MANIFEST.MF";
  }

}
