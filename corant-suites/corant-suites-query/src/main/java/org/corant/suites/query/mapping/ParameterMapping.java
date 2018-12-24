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
package org.corant.suites.query.mapping;

import java.io.Serializable;

/**
 * asosat-query
 *
 * @author bingo 上午10:36:16
 *
 */
public class ParameterMapping implements Serializable {

  private static final long serialVersionUID = -1992270251269637655L;

  private String name;
  private Class<?> type;

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type
   */
  public Class<?> getType() {
    return type;
  }

  void setName(String name) {
    this.name = name;
  }

  void setType(Class<?> type) {
    this.type = type;
  }



}
