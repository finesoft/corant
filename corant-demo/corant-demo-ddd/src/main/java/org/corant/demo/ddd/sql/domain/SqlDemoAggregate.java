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
package org.corant.demo.ddd.sql.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.corant.demo.ddd.shared.domain.AbstractDemoAggregate;
import org.corant.demo.ddd.shared.domain.DemoValue;

/**
 * corant-demo-ddd
 *
 * @author bingo 下午6:45:20
 *
 */
@Entity
@Table(name = "CT_DEMO_SQLAGG")
public class SqlDemoAggregate extends AbstractDemoAggregate {

  private static final long serialVersionUID = 8336865495535105382L;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "CT_DEMO_SQLAGG_SVALS", joinColumns = {
      @JoinColumn(name = "sqlAggId", foreignKey = @ForeignKey(name = "FK_SQLAGG_SVALS"))})
  private List<DemoValue> someValues = new ArrayList<>();

  /**
   *
   * @return the someValues
   */
  public List<DemoValue> getSomeValues() {
    return someValues;
  }

  /**
   *
   * @param someValues the someValues to set
   */
  public void setSomeValues(List<DemoValue> someValues) {
    this.someValues = someValues;
  }

}
