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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import org.corant.suites.ddd.model.AbstractEntity;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.saga.Saga;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:19:02
 *
 */
public abstract class AbstractSaga extends AbstractEntity implements Saga {

  private static final long serialVersionUID = 888380663718890415L;

  @Column
  private volatile boolean actived = true;

  @Column
  private String trackingToken;

  public AbstractSaga() {}

  @Override
  public abstract List<DefaultSagaAttribute> getAttributes();

  public Map<String, List<DefaultSagaAttribute>> getAttributesMap() {
    Map<String, List<DefaultSagaAttribute>> map = new LinkedHashMap<>();
    if (getAttributes() != null) {
      getAttributes()
          .forEach(at -> map.computeIfAbsent(at.getName(), k -> new ArrayList<>()).add(at));
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public abstract AggregateIdentifier getOriginal();

  @Override
  public String getTrackingToken() {
    return trackingToken;
  }

  @Override
  public boolean isActived() {
    return actived;
  }

  @Override
  public Saga withTrackingToken(String trackingToken) {
    this.trackingToken = trackingToken;
    return this;
  }

  protected void setActived(boolean actived) {
    this.actived = actived;
  }

  protected void setTrackingToken(String trackingToken) {
    this.trackingToken = trackingToken;
  }

}
