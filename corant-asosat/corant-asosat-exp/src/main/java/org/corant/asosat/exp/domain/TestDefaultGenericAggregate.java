/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.exp.domain;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.corant.suites.ddd.model.AbstractDefaultGenericAggregate;
import org.hibernate.annotations.GenericGenerator;

/**
 * corant-asosat-exp
 *
 * @author bingo 下午1:51:08
 *
 */
@Entity
@Table(name = "EP_Test")
public class TestDefaultGenericAggregate
    extends AbstractDefaultGenericAggregate<Map<String, Object>, TestDefaultGenericAggregate> {

  private static final long serialVersionUID = 3161662624636832024L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "snowflake")
  @GenericGenerator(name = "snowflake",
      strategy = "org.corant.suites.jpa.hibernate.HibernateSnowflakeIdGenerator")
  private Long id;

  @Column
  private String name;

  @Override
  public Serializable getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
