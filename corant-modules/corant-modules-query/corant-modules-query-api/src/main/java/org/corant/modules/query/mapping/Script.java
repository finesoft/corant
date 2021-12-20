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
package org.corant.modules.query.mapping;

import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.strip;
import java.io.Serializable;
import java.util.UUID;

/**
 * corant-modules-query-api
 *
 * @author bingo 上午10:14:58
 *
 */
public class Script implements Serializable {

  private static final long serialVersionUID = -8086650413401748374L;

  public static final Script EMPTY = new Script();

  private final String id = UUID.randomUUID().toString();

  private String code;

  private String src;

  private ScriptType type;

  /**
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   *
   * @return the src
   */
  public String getSrc() {
    return src;
  }

  /**
   *
   * @return the type
   */
  public ScriptType getType() {
    return type;
  }

  public boolean isValid() {
    return isNotBlank(code);
  }

  /**
   *
   * @param code the code to set
   */
  protected void setCode(String code) {
    this.code = strip(code);
  }

  /**
   *
   * @param src the src to set
   */
  protected void setSrc(String src) {
    this.src = src;
  }

  /**
   *
   * @param type the type to set
   */
  protected void setType(ScriptType type) {
    this.type = type;
  }

  public enum ScriptType {

    JS, FM, KT, CDI, JSE;

  }
}
