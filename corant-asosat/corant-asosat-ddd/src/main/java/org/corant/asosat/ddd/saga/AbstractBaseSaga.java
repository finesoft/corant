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
package org.corant.asosat.ddd.saga;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.shared.BaseAggregateIdentifier;
import org.corant.asosat.ddd.message.BaseMessageIdentifier;
import org.hibernate.annotations.GenericGenerator;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:21:01
 *
 */
@MappedSuperclass
public abstract class AbstractBaseSaga extends AbstractSaga {

  private static final long serialVersionUID = 5408036361458995128L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "snowflake")
  @GenericGenerator(name = "snowflake",
      strategy = "org.corant.suites.jpa.hibernate.HibernateSnowflakeIdGenerator")
  private Long id;

  @Embedded
  @AttributeOverrides(
      value = {@AttributeOverride(column = @Column(name = "triggerMessageId"), name = "id"),
          @AttributeOverride(column = @Column(name = "triggerMessageType"), name = "type"),
          @AttributeOverride(column = @Column(name = "triggerMessageQueue"), name = "queue")})
  private BaseMessageIdentifier triggerMessage;

  @AttributeOverrides(
      value = {@AttributeOverride(column = @Column(name = "originalId"), name = "id"),
          @AttributeOverride(column = @Column(name = "originalType"), name = "type")})
  private BaseAggregateIdentifier original;

  public AbstractBaseSaga() {}


  @Override
  public Long getId() {
    return id;
  }

  @Override
  public BaseAggregateIdentifier getOriginal() {
    return original;
  }

  @Override
  public BaseMessageIdentifier getTriggerMessage() {
    return triggerMessage;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  protected void setOriginal(BaseAggregateIdentifier original) {
    this.original = original;
  }

  protected void setTriggerMessage(BaseMessageIdentifier triggerMessage) {
    this.triggerMessage = triggerMessage;
  }
}
