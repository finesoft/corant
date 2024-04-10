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
package org.corant.modules.jaxrs.shared;

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Streams.copy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;
import org.corant.modules.servlet.HttpStreamOutput;
import org.corant.modules.servlet.HttpStreamOutput.HttpStreamOutputBuilder;
import org.corant.modules.servlet.HttpStreamOutput.HttpStreamOutputResult;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.FileSystemResource;
import org.corant.shared.resource.InputStreamResource;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Compressors;
import org.corant.shared.util.FileUtils;

/**
 * corant-modules-jaxrs-shared
 *
 * @author bingo 上午11:44:46
 */
public class StreamResponseBuilder {

  private final static HttpStreamOutput emptyOutput =
      new HttpStreamOutput(new HttpStreamOutputBuilder());

  protected Resource resource;
  protected File[] files = FileUtils.EMPTY_ARRAY;
  protected HttpStreamOutput output;

  protected boolean supportRange =
      getConfigValue("corant.modules.jaxrs.stream.support-range", Boolean.class, true);

  protected StreamResponseBuilder() {}

  protected StreamResponseBuilder(HttpStreamOutput output, File... files) {
    this.output = defaultObject(output, emptyOutput);
    this.files = shouldNotEmpty(files);
  }

  protected StreamResponseBuilder(HttpStreamOutput output, Resource resource) {
    this.output = defaultObject(output, emptyOutput);
    this.resource = shouldNotNull(resource, "The resource can not null!");
  }

  public static StreamResponseBuilder of(File file) {
    return of(new FileSystemResource(file, FileSystemResource.metadataOf(file)));
  }

  public static StreamResponseBuilder of(InputStream is) {
    return of(new InputStreamResource(is, null, null));
  }

  public static StreamResponseBuilder of(InputStream is, HttpStreamOutput output) {
    return of(new InputStreamResource(is, null, null), output);
  }

  public static StreamResponseBuilder of(Resource resource) {
    return of(resource, new HttpStreamOutputBuilder().fromResource(resource).build());
  }

  public static StreamResponseBuilder of(Resource resource, HttpStreamOutput output) {
    return new StreamResponseBuilder(output, resource);
  }

  public Response build() {
    if (resource != null) {
      if (output.getSize() != null && output.getSize() > 0 && supportRange) {
        HttpStreamOutputResult outputResult = output.resolveRangeOutputResult();
        ResponseBuilder responseBuilder = Response.status(outputResult.getStatus());
        outputResult.getHeaders().forEach(responseBuilder::header);
        return responseBuilder.entity((StreamingOutput) os -> {
          try (InputStream is = resource.openInputStream()) {
            outputResult.getWriter().accept(is, os);
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        }).build();
      } else {
        ResponseBuilder responseBuilder = Response.ok();
        output.resolveOutputHeaders().forEach(responseBuilder::header);
        return responseBuilder.entity((StreamingOutput) os -> {
          try (InputStream is = resource.openInputStream()) {
            copy(is, os);
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        }).build();
      }
    } else {
      ResponseBuilder responseBuilder = Response.ok();
      output.resolveOutputHeaders().forEach(responseBuilder::header);
      return responseBuilder.entity((StreamingOutput) os -> Compressors.zip(os, files)).build();
    }
  }
}
