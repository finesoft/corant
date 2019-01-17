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
package org.corant.asosat.exp;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileInputStream;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * corant-asosat-exp
 *
 * @author bingo 下午12:02:15
 *
 */
@RunWith(CorantJUnit4ClassRunner.class)
public class MultipartTest {

  @Test
  public void sendFile() throws Exception {
    ResteasyClient client = new ResteasyClientBuilder().build();
    ResteasyWebTarget target = client.target("http://localhost:7676/exp/testMultipart/upload");
    MultipartFormDataOutput mdo = new MultipartFormDataOutput();
    // Specify file name to be uploaded in pom.xml
    // Check that file exists
    mdo.addFormData("attachment", new FileInputStream(new File("e:/CorantWorkspace_bak.rar")),
        MediaType.APPLICATION_OCTET_STREAM_TYPE, "CorantWorkspace_bak.rar");

    GenericEntity<MultipartFormDataOutput> entity =
        new GenericEntity<MultipartFormDataOutput>(mdo) {};
    Response r = target.request().post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
    assertEquals(r.getStatus(), 200);
  }
}
