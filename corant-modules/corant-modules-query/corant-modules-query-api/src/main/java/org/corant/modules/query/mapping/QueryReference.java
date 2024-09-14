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

import static org.corant.shared.util.Strings.EMPTY;
import java.io.Serializable;
import org.corant.modules.query.mapping.Query.QueryType;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午7:29:52
 */
public class QueryReference implements Serializable {

  private static final long serialVersionUID = 2437518228682652812L;

  protected String name;
  protected QueryType type;
  protected String qualifier;
  protected String version = EMPTY;

  public QueryReference(Query query) {
    setName(query.getName());
    setType(query.getType());
    setQualifier(query.getQualifier());
    setVersion(query.getVersion());
  }

  protected QueryReference() {}

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the qualifier
   */
  public String getQualifier() {
    return qualifier;
  }

  /**
   * @return the type
   */
  public QueryType getType() {
    return type;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  public String getVersionedName() {
    return SchemaNames.resolveVersionedName(getName(), getVersion());
  }

  @Override
  public String toString() {
    return "{\"name\":\"" + name + "\", \"type\": \"" + type + "\", \"qualifier\": \"" + qualifier
        + "\", \"version\": \"" + version + "\"}";
  }

  /**
   * @param name the name to set
   */
  protected void setName(String name) {
    this.name = name;
  }

  /**
   *
   * @param qualifier the qualifier to set
   */
  protected void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  /**
   * @param type the type to set
   */
  protected void setType(QueryType type) {
    this.type = type;
  }

  /**
   * @param version the version to set
   */
  protected void setVersion(String version) {
    this.version = version;
  }

}
