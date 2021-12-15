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
package org.corant.modules.ddd.shared.model;

import javax.persistence.MappedSuperclass;
import org.corant.modules.ddd.AggregateAssistant;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * An abstract aggregate that part of the implementation of event processing and other related
 * functions, use JPA.
 *
 * @author bingo 下午3:43:12
 */
@MappedSuperclass
public abstract class AbstractDefaultAggregate extends AbstractAggregate {

  private static final long serialVersionUID = -1347035224644784732L;

  protected transient volatile AggregateAssistant assistant;

  protected AbstractDefaultAggregate() {}

  @Override
  protected synchronized AggregateAssistant callAssistant() {
    if (assistant == null) {
      assistant = new DefaultAggregateAssistant(this);
    }
    return assistant;
  }

}
