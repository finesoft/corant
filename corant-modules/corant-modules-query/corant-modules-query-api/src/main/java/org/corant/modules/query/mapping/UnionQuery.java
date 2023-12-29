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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * corant-modules-query-api
 *
 * Unfinish yet!
 *
 * @author bingo 下午7:28:59
 */
public class UnionQuery implements Serializable {

  private static final long serialVersionUID = 1426980997445445995L;

  private List<QueryReference> queries = new ArrayList<>();

  private boolean distinct;

  private Script unionScript = new Script();

  /**
   *
   * @return the queries
   */
  public List<QueryReference> getQueries() {
    return queries;
  }

  /**
   *
   * @return the unionScript
   */
  public Script getUnionScript() {
    return unionScript;
  }

  /**
   *
   * @return the distinct
   */
  public boolean isDistinct() {
    return distinct;
  }

  /**
   *
   * @param distinct the distinct to set
   */
  protected void setDistinct(boolean distinct) {
    this.distinct = distinct;
  }

  /**
   *
   * @param queries the queries to set
   */
  protected void setQueries(List<QueryReference> queries) {
    this.queries = queries;
  }

  /**
   *
   * @param unionScript the unionScript to set
   */
  protected void setUnionScript(Script unionScript) {
    this.unionScript = unionScript;
  }

}
