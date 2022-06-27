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
import static org.corant.shared.util.Maps.getMapLong;
import static org.corant.shared.util.Maps.getMapString;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.modules.servlet.HttpStreamOutput;
import org.corant.modules.servlet.HttpStreamOutput.HttpStreamOutputResult;
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

  protected StreamOutputBuilder() {}

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

  public static StreamOutputBuilder of(Resource resource) {
    return new StreamOutputBuilder(resource).fileName(resource.getName()).name(resource.getName())
        .size(getMapLong(resource.getMetadata(), Resource.META_CONTENT_LENGTH))
        .contentType(getMapString(resource.getMetadata(), Resource.META_CONTENT_TYPE));
  }

  public static Response responseOf(boolean supportRange, InputStream content, String contentType,
      ContentDisposition contentDisposition, boolean autoCloseInputStream,
      HttpHeaders requestHeadlers) {
    HttpStreamOutput output = new HttpStreamOutput(requestHeadlers::getHeaderString, contentType,
        contentDisposition, autoCloseInputStream, supportRange);
    HttpStreamOutputResult outputResult = output.handle();
    ResponseBuilder responseBuilder = Response.status(outputResult.getStatus());
    outputResult.getHeaders().forEach(responseBuilder::header);
    return responseBuilder
        .entity((StreamingOutput) os -> outputResult.getWriter().accept(content, os)).build();
  }

  public static Response responseOf(boolean supportRange, InputStream input, String contentType,
      String type, String filename, Charset charset, Long size, ZonedDateTime modificationDate,
      boolean loose, boolean autoCloseInputStream, HttpHeaders requestHeadlers) {
    HttpStreamOutput output = new HttpStreamOutput(requestHeadlers::getHeaderString, contentType,
        type, filename, charset, size, modificationDate, loose, autoCloseInputStream, supportRange);
    HttpStreamOutputResult outputResult = output.handle();
    ResponseBuilder responseBuilder = Response.status(outputResult.getStatus());
    outputResult.getHeaders().forEach(responseBuilder::header);
    return responseBuilder.entity((StreamingOutput) os -> {
      try (InputStream is = input) {
        outputResult.getWriter().accept(is, os);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }).build();
  }

  public static Response responseOf(boolean supportRange, Resource resource, boolean inline,
      HttpHeaders requestHeadlers) {
    HttpStreamOutput output = new HttpStreamOutput(requestHeadlers::getHeaderString, resource,
        inline, true, supportRange);
    HttpStreamOutputResult outputResult = output.handle();
    ResponseBuilder responseBuilder = Response.status(outputResult.getStatus());
    outputResult.getHeaders().forEach(responseBuilder::header);
    return responseBuilder.entity((StreamingOutput) os -> {
      try (InputStream is = resource.openInputStream()) {
        outputResult.getWriter().accept(is, os);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }).build();
  }

  public static StreamOutputBuilder zipFiles(File... files) {
    return new StreamOutputBuilder(files);
  }

  public Response build() {
    final StreamingOutput sop;
    if (resource != null) {
      sop = op -> {
        try (InputStream input = resource.openInputStream()) {
          Streams.copy(input, op);
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      };
    } else {
      sop = op -> Compressors.zip(op, files);
    }
    return super.handle(sop);
  }

  @Override
  protected StreamOutputBuilder me() {
    return this;
  }
}
