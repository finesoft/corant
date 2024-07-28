/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query;

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Maps.newHashMap;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Map;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;

/**
 *
 * corant-modules-query-api
 *
 * @author bingo 下午6:56:58
 */
public class GenericQueryParameter<T> extends DefaultQueryParameter {

  private static final long serialVersionUID = 7809436027996029494L;

  public GenericQueryParameter() {}

  @Override
  public GenericQueryParameter<T> context(Map<String, Object> context) {
    return setContext(context);
  }

  @Override
  public GenericQueryParameter<T> context(Object... objects) {
    return setContext(mapOf(objects));
  }

  @Override
  public GenericQueryParameter<T> criteria(Object criteria) {
    return setCriteria(forceCast(criteria));
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public T getCriteria() {
    return forceCast(criteria);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public GenericQueryParameter<T> limit(Integer limit) {
    super.limit(limit);
    return this;
  }

  @Override
  public GenericQueryParameter<T> offset(Integer offset) {
    super.offset(offset);
    return this;
  }

  public GenericQueryParameter<T> setContext(Map<String, Object> context) {
    this.context = newHashMap(context);
    return this;
  }

  public GenericQueryParameter<T> setCriteria(T criteria) {
    this.criteria = criteria;
    return this;
  }

  public GenericQueryParameter<T> setLimit(Integer limit) {
    super.limit(limit);
    return this;
  }

  public GenericQueryParameter<T> setOffset(Integer offset) {
    super.offset(offset);
    return this;
  }

}
