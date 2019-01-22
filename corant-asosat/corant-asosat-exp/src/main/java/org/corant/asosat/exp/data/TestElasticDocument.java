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
package org.corant.asosat.exp.data;

import java.util.List;
import org.corant.suites.elastic.metadata.annotation.EsArray;
import org.corant.suites.elastic.metadata.annotation.EsDocument;
import org.corant.suites.elastic.metadata.annotation.EsEmbedded;
import org.corant.suites.elastic.metadata.annotation.EsKeyword;
import org.corant.suites.elastic.metadata.annotation.EsMultiFields;
import org.corant.suites.elastic.metadata.annotation.EsMultiFieldsEntry;
import org.corant.suites.elastic.metadata.annotation.EsMultiFieldsPair;
import org.corant.suites.elastic.metadata.annotation.EsNested;
import org.corant.suites.elastic.metadata.annotation.EsText;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午9:57:22
 *
 */
@EsDocument(indexName = "Test", number_of_replicas = 1, number_of_shards = 5)
public class TestElasticDocument extends AbstractElasticDocument {

  private static final long serialVersionUID = 270292539040242697L;

  @EsKeyword(ignore_above = 512)
  private String esId;

  @EsEmbedded
  private TestElasticEmbeddable embedded;

  @EsNested
  private TestElasticNested nested;

  @EsNested
  private List<TestElasticNested> nestedCollection;

  @EsEmbedded
  private List<TestElasticEmbeddable> embeddedCollection;

  @EsArray(eleType = "long")
  private Long[] longArr;

  @EsText(analyzer = "standard", search_analyzer = "standard",
      fields = @EsMultiFields(entries = {@EsMultiFieldsEntry(fieldName = "value",
          pairs = {@EsMultiFieldsPair(key = "type", value = "keyword")})}))
  private String multi;

  /**
   *
   * @return the embedded
   */
  public TestElasticEmbeddable getEmbedded() {
    return embedded;
  }

  /**
   *
   * @return the embeddedCollection
   */
  public List<TestElasticEmbeddable> getEmbeddedCollection() {
    return embeddedCollection;
  }

  @Override
  public String getEsId() {
    return esId;
  }

  /**
   *
   * @return the longArr
   */
  public Long[] getLongArr() {
    return longArr;
  }

  /**
   *
   * @return the nested
   */
  public TestElasticNested getNested() {
    return nested;
  }

  /**
   *
   * @return the nestedCollection
   */
  public List<TestElasticNested> getNestedCollection() {
    return nestedCollection;
  }

  /**
   *
   * @param embedded the embedded to set
   */
  public void setEmbedded(TestElasticEmbeddable embedded) {
    this.embedded = embedded;
  }

  /**
   *
   * @param embeddedCollection the embeddedCollection to set
   */
  public void setEmbeddedCollection(List<TestElasticEmbeddable> embeddedCollection) {
    this.embeddedCollection = embeddedCollection;
  }

  /**
   *
   * @param esId the esId to set
   */
  public void setEsId(String esId) {
    this.esId = esId;
  }

  /**
   *
   * @param longArr the longArr to set
   */
  public void setLongArr(Long[] longArr) {
    this.longArr = longArr;
  }

  /**
   *
   * @param nested the nested to set
   */
  public void setNested(TestElasticNested nested) {
    this.nested = nested;
  }

  /**
   *
   * @param nestedCollection the nestedCollection to set
   */
  public void setNestedCollection(List<TestElasticNested> nestedCollection) {
    this.nestedCollection = nestedCollection;
  }

}
