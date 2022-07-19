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
package org.corant.modules.ddd;

import static org.corant.shared.util.Objects.forceCast;
import org.corant.modules.ddd.annotation.Events;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 下午5:57:36
 */
@Events
public abstract class AbstractPropertyChangedEvent<T, P> extends AbstractEvent {

  private static final long serialVersionUID = 6311499831097921960L;

  private final P oldValue;

  private final P newValue;

  public AbstractPropertyChangedEvent(T source, P oldValue, P newValue) {
    super(source);
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public P getNewValue() {
    return this.newValue;
  }

  public P getOldValue() {
    return this.oldValue;
  }

  @Override
  public T getSource() {
    return forceCast(super.getSource());
  }
}
