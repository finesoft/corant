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
package org.corant.asosat.ddd.domain.shared;

import java.util.Map;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;

/**
 * @author bingo 下午9:13:58
 *
 */
public class MaintainResultInfo extends AbstractValueObject {

  private static final long serialVersionUID = 7024532037862347695L;

  final Long id;

  final Long vn;

  final DynamicAttributeMap attributes = new DynamicAttributeMap();

  /**
   *
   * @param id
   * @param vn
   * @param attrs
   */
  public MaintainResultInfo(Long id, Long vn, Map<String, Object> attrs) {
    super();
    this.id = id;
    this.vn = vn;
    if (attrs != null) {
      attributes.putAll(attrs);
    }
  }

  /**
   * @return the attributes
   */
  public DynamicAttributeMap getAttributes() {
    return attributes;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the vn
   */
  public Long getVn() {
    return vn;
  }


}
