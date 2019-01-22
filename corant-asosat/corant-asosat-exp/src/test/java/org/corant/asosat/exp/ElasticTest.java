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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.asosat.exp.data.TestElasticChildDocument;
import org.corant.asosat.exp.data.TestElasticDocument;
import org.corant.asosat.exp.data.TestElasticEmbeddable;
import org.corant.asosat.exp.data.TestElasticEnum;
import org.corant.asosat.exp.data.TestElasticNested;
import org.corant.asosat.exp.data.TestElasticParentDocument;
import org.corant.suites.elastic.ElasticSchemaUtils;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午10:52:52
 *
 */
public class ElasticTest {

  public static void buildTest(String... strings) {
    TestElasticDocument doc = new TestElasticDocument();
    doc.setBool(true);
    doc.setDateRange(Instant.now());
    doc.setEmbedded(new TestElasticEmbeddable("single-embedded-kw", "single-embedded-txt"));
    doc.setEmbeddedCollection(TestElasticEmbeddable.of("col-embedded-kw1", "col-embedded-txt1",
        "col-embedded-kw2", "col-embedded-txt2"));
    doc.setEnums(TestElasticEnum.ENUM_TWO);
    doc.setEsId(UUID.randomUUID().toString());
    doc.setInstant(Instant.now());
    doc.setIntegerRange(88);
    doc.setKeyword("keyword");
    doc.setLongArr(new Long[] {1L, 2L, 3L});
    doc.setNested(new TestElasticNested("single-nested-kw", "single-nested-txt"));
    doc.setNestedCollection(TestElasticNested.of("col-nested-kw1", "col-nested-txt1",
        "col-nested-kw2", "col-nested-txt2"));
    doc.setNumber(BigDecimal.valueOf(99.99));
    doc.setText("Usually we recommend using the same analyzer at index time and at search time. "
        + "In the case of the edge_ngram tokenizer, the advice is different. "
        + "It only makes sense to use the edge_ngram tokenizer at index time, "
        + "to ensure that partial words are available for matching in the index. "
        + "At search time, just search for the terms the user has typed in, "
        + "for instance: Quick Fo.");
    // System.out.println(JsonUtils.toJsonStr(doc, true));
    System.out.println(Instant.now().toEpochMilli());
  }

  public static void buildTestPc(String... strings) {
    TestElasticParentDocument pdoc = new TestElasticParentDocument();
    pdoc.setBool(false);
    pdoc.setKeyword("p-keyword");
    pdoc.setNumber(BigDecimal.TEN);
    pdoc.setText("敏捷的狐狸跳过棕色的狗");
    pdoc.setEnums(TestElasticEnum.ENUM_TWO);
    System.out.println(JsonUtils.toJsonStr(pdoc, true));
    System.out.println("=========================");
    TestElasticChildDocument cdoc = new TestElasticChildDocument("childkey", "childtxt");
    System.out.println(JsonUtils.toJsonStr(cdoc, true));
  }

  public static void main(String... strings) {
    schemaOut();
  }

  public static void schemaOut(String... strings) {
    ElasticSchemaUtils.stdout("bingo", (in, idx) -> {
      System.out.println(in);
      System.out.println(JsonUtils.toJsonStr(idx, true));
    });
  }
}
