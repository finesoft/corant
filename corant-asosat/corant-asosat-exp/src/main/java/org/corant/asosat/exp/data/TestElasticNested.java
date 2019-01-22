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

import org.corant.suites.elastic.metadata.annotation.EsEmbeddable;
import org.corant.suites.elastic.metadata.annotation.EsKeyword;
import org.corant.suites.elastic.metadata.annotation.EsText;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午10:04:06
 *
 */
@EsEmbeddable
public class TestElasticNested {

  @EsKeyword(ignore_above = 256)
  private String neKeyword;

  @EsText
  private String neText;

  /**
   *
   * @return the neKeyword
   */
  public String getNeKeyword() {
    return neKeyword;
  }

  /**
   *
   * @return the neText
   */
  public String getNeText() {
    return neText;
  }

  /**
   *
   * @param neKeyword the neKeyword to set
   */
  public void setNeKeyword(String neKeyword) {
    this.neKeyword = neKeyword;
  }

  /**
   *
   * @param neText the neText to set
   */
  public void setNeText(String neText) {
    this.neText = neText;
  }

}
