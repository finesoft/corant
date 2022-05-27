/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.ddd.shared.event;

import static org.corant.shared.util.Objects.EMPTY_ARRAY;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Arrays;
import javax.persistence.LockModeType;
import org.corant.modules.ddd.AbstractEvent;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.shared.model.AggregateReference;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午7:51:06
 *
 */
public class AggregateLockEvent extends AbstractEvent {

  private static final long serialVersionUID = -3701214400538496807L;

  private final LockModeType lockModeType;

  private final Object[] properties;

  public AggregateLockEvent(Aggregate source) {
    this(source, null);
  }

  public AggregateLockEvent(Aggregate source, LockModeType lockModeType, Object... properties) {
    super(source);
    this.lockModeType = defaultObject(lockModeType, LockModeType.OPTIMISTIC);
    if (properties.length > 0) {
      this.properties = Arrays.copyOf(properties, properties.length);
    } else {
      this.properties = EMPTY_ARRAY;
    }
  }

  public AggregateLockEvent(AggregateReference<? extends Aggregate> source) {
    this(source.retrieve());
  }

  public AggregateLockEvent(AggregateReference<?> source, LockModeType lockModeType,
      Object... properties) {
    this(source.retrieve(), lockModeType, properties);
  }

  public LockModeType getLockModeType() {
    return lockModeType;
  }

  public Object[] getProperties() {
    return properties.length == 0 ? properties : Arrays.copyOf(properties, properties.length);
  }

  @Override
  public Aggregate getSource() {
    return (Aggregate) super.getSource();
  }

}
