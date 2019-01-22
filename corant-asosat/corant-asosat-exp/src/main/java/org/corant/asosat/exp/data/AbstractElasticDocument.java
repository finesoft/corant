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

import java.math.BigDecimal;
import java.time.Instant;
import org.corant.suites.elastic.metadata.annotation.EsBoolean;
import org.corant.suites.elastic.metadata.annotation.EsDate;
import org.corant.suites.elastic.metadata.annotation.EsKeyword;
import org.corant.suites.elastic.metadata.annotation.EsMappedSuperclass;
import org.corant.suites.elastic.metadata.annotation.EsNumeric;
import org.corant.suites.elastic.metadata.annotation.EsNumeric.EsNumericType;
import org.corant.suites.elastic.metadata.annotation.EsProperty;
import org.corant.suites.elastic.metadata.annotation.EsRange;
import org.corant.suites.elastic.metadata.annotation.EsRange.RangeType;
import org.corant.suites.elastic.metadata.annotation.EsText;
import org.corant.suites.elastic.model.ElasticDocument;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午9:58:40
 *
 */
@EsMappedSuperclass
public abstract class AbstractElasticDocument implements ElasticDocument {

  private static final long serialVersionUID = 8299177069277158526L;

  @EsKeyword(ignore_above = 256)
  private String keyword;

  @EsText(analyzer = "standard", search_analyzer = "standard")
  private String text;

  @EsNumeric(type = EsNumericType.FLOAT)
  private BigDecimal number;

  @EsDate
  private Instant instant;

  @EsRange(type = RangeType.DATE_RANGE, properties = {
      @EsProperty(name = "format", value = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")})
  private Instant dateRange;

  @EsRange(type = RangeType.INTEGER_RANGE, properties = {})
  private Integer integerRange;

  @EsBoolean
  private boolean bool;

  @EsKeyword(ignore_above = 32)
  private TestElasticEnum enums;

  /**
   * 
   * @return the dateRange
   */
  public Instant getDateRange() {
    return dateRange;
  }

  /**
   * 
   * @return the enums
   */
  public TestElasticEnum getEnums() {
    return enums;
  }

  /**
   *
   * @return the instant
   */
  public Instant getInstant() {
    return instant;
  }

  /**
   * 
   * @return the integerRange
   */
  public Integer getIntegerRange() {
    return integerRange;
  }

  /**
   *
   * @return the keyword
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   *
   * @return the number
   */
  public BigDecimal getNumber() {
    return number;
  }

  /**
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   *
   * @return the bool
   */
  public boolean isBool() {
    return bool;
  }

  /**
   *
   * @param bool the bool to set
   */
  public void setBool(boolean bool) {
    this.bool = bool;
  }

  /**
   *
   * @param dateRange the dateRange to set
   */
  public void setDateRange(Instant dateRange) {
    this.dateRange = dateRange;
  }

  /**
   *
   * @param enums the enums to set
   */
  public void setEnums(TestElasticEnum enums) {
    this.enums = enums;
  }

  /**
   *
   * @param instant the instant to set
   */
  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  /**
   *
   * @param integerRange the integerRange to set
   */
  public void setIntegerRange(Integer integerRange) {
    this.integerRange = integerRange;
  }

  /**
   *
   * @param keyword the keyword to set
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   *
   * @param number the number to set
   */
  public void setNumber(BigDecimal number) {
    this.number = number;
  }

  /**
   *
   * @param text the text to set
   */
  public void setText(String text) {
    this.text = text;
  }

}
