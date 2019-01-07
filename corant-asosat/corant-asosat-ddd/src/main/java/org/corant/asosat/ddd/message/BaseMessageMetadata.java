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

import static org.corant.kernel.util.Preconditions.requireNotNull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.corant.asosat.ddd.domain.shared.BaseAggregateIdentifier;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.model.Aggregate;

/**
 * @author bingo 下午3:05:34
 *
 */
@MappedSuperclass
@Embeddable
public class BaseMessageMetadata extends AbstractGenericMessageMetadata<DynamicAttributeMap> {

  private static final long serialVersionUID = -9150793205434516254L;

  @Embedded
  @AttributeOverrides(value = {@AttributeOverride(column = @Column(name = "sourceId"), name = "id"),
      @AttributeOverride(column = @Column(name = "sourceType"), name = "type")})
  private BaseAggregateIdentifier source;

  @Column
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String attributeContent;

  @Transient
  protected transient DynamicAttributeMap attributes = new DynamicAttributeMap();

  public BaseMessageMetadata(String queue, String trackingToken, Aggregate aggregate,
      DynamicAttributeMap attributes) {
    this(queue, trackingToken, aggregate.getVn(), BaseAggregateIdentifier.of(aggregate),
        attributes);
  }

  public BaseMessageMetadata(String queue, String trackingToken, long sequenceNumber,
      BaseAggregateIdentifier identifier, DynamicAttributeMap attributes) {
    super(queue, trackingToken, sequenceNumber);
    setSource(identifier);
    setAttributes(attributes);
  }

  protected BaseMessageMetadata() {
    super();
  }

  @Override
  public DynamicAttributeMap getAttributes() {
    return attributes.unmodifiable();
  }

  @Override
  public BaseAggregateIdentifier getSource() {
    return source;
  }

  @Override
  public void resetSequenceNumber(long sequenceNumber) {

  }

  @Override
  public String toString() {
    return "BaseMessageMetadata [getAttributes()=" + getAttributes() + ", getSource()="
        + getSource() + ", getOccurredTime()=" + getOccurredTime() + ", getQueue()=" + getQueue()
        + ", getSequenceNumber()=" + getSequenceNumber() + ", getClass()=" + this.getClass()
        + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
  }

  protected void setAttributes(DynamicAttributeMap attributes) {
    this.attributes.clear();
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
  }

  protected void setSource(BaseAggregateIdentifier source) {
    this.source = requireNotNull(source, GlobalMessageCodes.ERR_PARAM);
  }

  void deserializeAttributeContent() {
    setAttributes(attributeContent == null ? new DynamicAttributeMap()
        : new DynamicAttributeMap(
            JsonUtils.fromJsonStr(attributeContent, String.class, Object.class)));
  }

  void serializeAttributeContent() {
    attributeContent = JsonUtils.toJsonStr(attributes);
  }

}
