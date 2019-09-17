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
package org.corant.suites.query.shared;

import static org.corant.shared.util.MapUtils.mapOf;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午9:16:07
 *
 */
public interface QueryParameter extends Serializable {

  /**
   * Return the query context that may be contain current user context or security context.
   *
   * @return getContext
   */
  default Map<String, Object> getContext() {
    return Collections.emptyMap();
  }

  /**
   * Return query criteria. The criteria can be Map or POJO that used to construct query conditions
   * or specificationsused to construct query conditions or specifications.
   *
   * @return getCriteria
   */
  Object getCriteria();

  /**
   * Returns the number of query result records, usually used for paging queries or specified range
   * queries. Default return -1, means that if it is used in paging query value is 16 else is 128.
   *
   * @return getLimit
   */
  default int getLimit() {
    return 1;
  }

  /**
   * Used for query result handling or specific database query instructions, means skips the offset
   * rows before beginning to return the rows. Default return -1 means that not supply offset.
   *
   * @return getOffset
   */
  default int getOffset() {
    return 0;
  }

  /**
   * corant-suites-query-shared
   *
   * @author bingo 下午4:09:56
   *
   */
  public static class DefaultQueryParameter implements QueryParameter {

    private static final long serialVersionUID = 6618232487063961660L;

    public static final DefaultQueryParameter EMPTY_INST = new DefaultQueryParameter();

    private Object criteria;
    private int limit = 1;
    private int offset = 0;
    private Map<String, Object> context = new HashMap<>();

    /**
     *
     * @param context the context to set
     */
    public DefaultQueryParameter context(Map<String, Object> context) {
      this.context = context;
      return this;
    }

    /**
     *
     * @param context the context to set
     */
    public DefaultQueryParameter context(Object... objects) {
      context = mapOf(objects);
      return this;
    }

    /**
     *
     * @param criteria the criteria to set
     */
    public DefaultQueryParameter criteria(Object criteria) {
      this.criteria = criteria;
      return this;
    }

    /**
     *
     * @return the context
     */
    @Override
    public Map<String, Object> getContext() {
      return context;
    }

    /**
     *
     * @return the criteria
     */
    @Override
    public Object getCriteria() {
      return criteria;
    }

    /**
     *
     * @return the limit
     */
    @Override
    public int getLimit() {
      return limit;
    }

    /**
     *
     * @return the offset
     */
    @Override
    public int getOffset() {
      return offset;
    }

    /**
     *
     * @param limit the limit to set
     */
    public DefaultQueryParameter limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     *
     * @param offset the offset to set
     */
    public DefaultQueryParameter offset(int offset) {
      this.offset = offset;
      return this;
    }

  }

}
