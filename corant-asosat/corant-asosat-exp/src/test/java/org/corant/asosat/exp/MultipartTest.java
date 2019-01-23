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

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.MapUtils.asMap;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.asosat.exp.provider.TestElasticIndicesService;
import org.corant.asosat.exp.provider.TestEsNamedQuery;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
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

  @Inject
  TestElasticIndicesService es;

  @Inject
  TestEsNamedQuery eq;

  @Test
  public void elasticQuery() throws IOException {
    Map<String, Object> list = eq.get("test.searchAll", asMap("keyword", "1548145685337"));
    System.out.println(JsonUtils.toJsonStr(list, true));
    // String query = " {\"term\": { \"enums\": \"enum_one\" } } ";
    // System.out.println(query);
    // WrapperQueryBuilder qb = QueryBuilders.wrapperQuery(query);
    // es.getTransportClient().prepareSearch("test").setQuery(qb).get();
  }

  // @Test
  public void elasticTest() throws IOException {
    TransportClient tc = es.getTransportClient();
    SearchResponse sr = tc.prepareSearch("test").setQuery(QueryBuilders.matchAllQuery()).get();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(3);
    XContentBuilder xbuilder = new XContentBuilder(XContentType.JSON.xContent(), baos,
        asSet("hits.hits._source.embeddedCollection.emText"));
    XContentBuilder builder = sr.toXContent(xbuilder, ToXContent.EMPTY_PARAMS);
    // String extractPath = ifBlank(getMapString(hints, HIT_RS_ETR_PATH_NME), DFLT_RS_ETR_PATH);
    // builder = builder.contentType().xContent().
    BytesReference bytes = BytesReference.bytes(builder);
    Map<String, Object> result = XContentHelper.convertToMap(bytes, false, XContentType.JSON).v2();
    System.out.println(JsonUtils.toJsonStr(result, true));
  }

  // @Test
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
