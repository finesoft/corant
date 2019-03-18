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
package org.corant.asosat.ddd.domain.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.MappedSuperclass;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午1:49:31
 *
 */
@MappedSuperclass
@SuppressWarnings("rawtypes")
public abstract class AbstractTreeNodeAggregateReference<T extends AbstractTreeNodeAggregate>
    extends AbstractVersionedAggregateReference<T> {

  private static final long serialVersionUID = -2947751238888458455L;

  public AbstractTreeNodeAggregateReference() {
    super();
  }

  public AbstractTreeNodeAggregateReference(T agg) {
    super(agg);
  }

  public abstract Iterable<T> obtainChilds();

  @SuppressWarnings("unchecked")
  public T obtainParent() {
    if (getId() != null) {
      return (T) this.retrieve().getParent();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Iterable<T> obtainSiblings() {
    List<T> siblings = new ArrayList<>();
    T parent = this.obtainParent();
    if (parent != null) {
      parent.getChilds().forEach(c -> siblings.add((T) c));
    }
    return siblings;
  }

}
