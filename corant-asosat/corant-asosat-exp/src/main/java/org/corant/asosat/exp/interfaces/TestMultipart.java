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
package org.corant.asosat.exp.interfaces;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.corant.shared.util.FileUtils;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午11:41:15
 *
 */
@Path("/testMultipart")
@ApplicationScoped
public class TestMultipart {

  @GET
  @Path("/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFileWithGet(@QueryParam("file") String file) {
    String path = System.getProperty("user.home") + File.separator + "uploads";
    File fileDownload = new File(path + File.separator + file);
    ResponseBuilder response = Response.ok(fileDownload);
    response.header("Content-Disposition", "attachment;filename=" + file);
    return response.build();
  }

  @POST
  @Path("/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Consumes("application/x-www-form-urlencoded")
  public Response downloadFileWithPost(@FormParam("file") String file) {
    String path = System.getProperty("user.home") + File.separator + "uploads";
    File fileDownload = new File(path + File.separator + file);
    ResponseBuilder response = Response.ok(fileDownload);
    response.header("Content-Disposition", "attachment;filename=" + file);
    return response.build();
  }

  @POST
  @Path("/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(MultipartFormDataInput input) throws IOException {
    Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
    List<InputPart> inputParts = uploadForm.get("attachment");
    for (InputPart inputPart : inputParts) {
      try {
        MultivaluedMap<String, String> header = inputPart.getHeaders();
        String fileName = getFileName(header);
        String path = System.getProperty("user.home") + File.separator + ".corant-test-uploads";
        File customDir = new File(path);
        if (!customDir.exists()) {
          customDir.mkdir();
        }
        fileName = customDir.getCanonicalPath() + File.separator + fileName;
        FileUtils.copyToFile(inputPart.getBody(InputStream.class, null), new File(fileName));
        return Response.status(200).entity("Uploaded file name : " + fileName).build();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private String getFileName(MultivaluedMap<String, String> header) {
    String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
    for (String filename : contentDisposition) {
      if (filename.trim().startsWith("filename")) {
        String[] name = filename.split("=");
        String finalFileName = name[1].trim().replaceAll("\"", "");
        return finalFileName;
      }
    }
    return "unknown";
  }

}
