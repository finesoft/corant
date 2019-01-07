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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.suites.ddd.model.Aggregate;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author bingo 下午4:22:48
 *
 */
@MappedSuperclass
@EntityListeners(BaseMessageEntityListener.class)
public abstract class AbstractBaseMessage
    extends AbstractGenericMessage<DynamicAttributeMap, DynamicAttributeMap> {

  private static final long serialVersionUID = -6025218140663379212L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "snowflake")
  @GenericGenerator(name = "snowflake",
      strategy = "org.corant.suites.jpa.hibernate.HibernateSnowflakeIdGenerator")
  private Long id;

  @Embedded
  private BaseMessageMetadata metadata;

  @Column
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String payloadContent;

  @Transient
  protected transient DynamicAttributeMap payload = new DynamicAttributeMap();

  protected AbstractBaseMessage() {}

  protected AbstractBaseMessage(ExchangedMessage message) {
    metadata = (BaseMessageMetadata) message.getMetadata();
    payloadContent = JsonUtils.toJsonStr(message.getPayload());
    deserializePayloadContent();
  }

  protected AbstractBaseMessage(String queue, Aggregate aggregate, DynamicAttributeMap attributes,
      DynamicAttributeMap payload) {
    this(queue, null, aggregate, attributes, payload);
  }

  protected AbstractBaseMessage(String queue, String trackingToken, Aggregate aggregate,
      DynamicAttributeMap attributes, DynamicAttributeMap payload) {
    metadata = new BaseMessageMetadata(queue, trackingToken, aggregate, attributes);
    setPayload(payload);
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public BaseMessageMetadata getMetadata() {
    return metadata;
  }

  @Override
  public DynamicAttributeMap getPayload() {
    return payload.unmodifiable();
  }

  protected void setMetadata(BaseMessageMetadata metadata) {
    this.metadata = metadata;
  }

  protected void setPayload(DynamicAttributeMap payload) {
    this.payload.clear();
    if (payload != null) {
      this.payload.putAll(payload);
    }
  }

  void deserializePayloadContent() {
    setPayload(payloadContent == null ? new DynamicAttributeMap()
        : new DynamicAttributeMap(
            JsonUtils.fromJsonStr(payloadContent, String.class, Object.class)));
    if (getMetadata() != null) {
      getMetadata().deserializeAttributeContent();
    }
  }

  void serializePayloadContent() {
    payloadContent = JsonUtils.toJsonStr(getPayload());
    if (getMetadata() != null) {
      metadata.serializeAttributeContent();
    }
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

}
