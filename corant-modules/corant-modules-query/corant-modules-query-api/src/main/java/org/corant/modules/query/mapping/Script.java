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
 */
public class Script implements Serializable {

  private static final long serialVersionUID = -8086650413401748374L;

  public static final Script EMPTY = new Script();

  private final String id = UUID.randomUUID().toString();

  private String code;

  private String src;

  private ScriptType type;

  /**
   * Returns script source code
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns this script id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the source code URL. For example: classpath:org/corant/BingoQuery.py, for more URL
   * schema, please see {@code org.corant.shared.resource.SourceType}.
   * <p>
   * Note: If both {@link #getCode()} and {@link #getSrc()} are valid, the {@link #getSrc()} takes
   * precedence. When the script object is constructed, the script of {@link #getSrc()} must be
   * assigned to {@link #code}.
   */
  public String getSrc() {
    return src;
  }

  /**
   * Returns the script type.
   */
  public ScriptType getType() {
    return type;
  }

  public boolean isValid() {
    return isNotBlank(code);
  }

  protected void setCode(String code) {
    this.code = strip(code);
  }

  protected void setSrc(String src) {
    this.src = src;
  }

  protected void setType(ScriptType type) {
    this.type = type;
  }

  /**
   * corant-modules-query-api
   *
   * @author bingo 下午1:11:27
   *
   */
  public enum ScriptType {

    /**
     * Indicates that the script or expression language is Javascript.
     */
    JS,
    /**
     * Indicates that the script or expression language is Freemarker.
     */
    FM,
    /**
     * Indicates that the script or expression language is Kotlin(JSR223).
     */
    KT,
    /**
     * Indicates that the script or expression is a CDI Bean, where the script is the named
     * qualifier of the CDI Bean.
     */
    CDI,
    /**
     * Indicates that the script or expression language is Json expression.
     */
    JSE;

  }
}
