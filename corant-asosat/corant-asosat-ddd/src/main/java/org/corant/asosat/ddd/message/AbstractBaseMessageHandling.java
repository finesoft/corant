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
package org.corant.asosat.ddd.message;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author bingo 下午4:58:42
 *
 */
@MappedSuperclass
public abstract class AbstractBaseMessageHandling extends AbstractMessageHandling {

  private static final long serialVersionUID = 8211337037953269393L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "snowflake")
  @GenericGenerator(name = "snowflake",
      strategy = "org.corant.suites.jpa.hibernate.HibernateSnowflakeIdGenerator")
  private Long id;

  @Column
  private String handler;

  @Column
  private String remark;

  @Column
  private Long messageId;

  @Column
  private String queue;

  public AbstractBaseMessageHandling() {}

  @Override
  public String getHandler() {
    return handler;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public Long getMessageId() {
    return messageId;
  }

  @Override
  public String getQueue() {
    return queue;
  }

  public String getRemark() {
    return remark;
  }

  protected void setHandler(String handler) {
    this.handler = handler;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  protected void setMessageId(Long messageId) {
    this.messageId = messageId;
  }

  @Override
  protected void setQueue(String queue) {
    this.queue = queue;
  }

  protected void setRemark(String remark) {
    this.remark = remark;
  }

}
