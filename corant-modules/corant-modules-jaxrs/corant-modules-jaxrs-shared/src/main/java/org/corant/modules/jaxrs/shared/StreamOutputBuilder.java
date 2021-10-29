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
package org.corant.modules.jaxrs.shared;

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.FileSystemResource;
import org.corant.shared.resource.InputStreamResource;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Compressors;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Streams;

/**
 * corant-modules-jaxrs-shared
 *
 * @author bingo 下午2:16:40
 */
public class StreamOutputBuilder extends AbstractStreamOutputHandler<StreamOutputBuilder> {

  protected Resource resource;
  protected File[] files = FileUtils.EMPTY_ARRAY;

  protected StreamOutputBuilder(File... files) {
    this.files = shouldNotEmpty(files);
  }

  protected StreamOutputBuilder(Resource resource) {
    this.resource = shouldNotNull(resource, "The resource can not null!");
  }

  public static StreamOutputBuilder of(File file) {
    return of(new FileSystemResource(file));
  }

  public static StreamOutputBuilder of(InputStream is) {
    return of(new InputStreamResource(is, null, null));
  }

  public static StreamOutputBuilder of(Resource resources) {
    return new StreamOutputBuilder(resources).fileName(resources.getName());
  }

  public static StreamOutputBuilder zipFiles(File... files) {
    return new StreamOutputBuilder(files);
  }

  public Response build() {
    return build(false);
  }

  public Response build(boolean loose) {
    final StreamingOutput stm;
    if (resource != null) {
      stm = output -> {
        try (InputStream input = resource.openInputStream()) {
          Streams.copy(input, output);
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      };
    } else {
      stm = output -> Compressors.zip(output, files);
    }
    return super.handle(stm, loose);
  }

  @Override
  protected StreamOutputBuilder me() {
    return this;
  }
}
