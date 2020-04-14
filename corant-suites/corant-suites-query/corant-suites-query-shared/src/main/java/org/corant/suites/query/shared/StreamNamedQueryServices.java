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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.suites.cdi.Instances.resolve;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import org.corant.shared.util.StreamUtils;
import org.corant.suites.query.shared.QueryParameter.StreamQueryParameter;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午5:15:27
 *
 */
public class StreamNamedQueryServices {

  private final String queryName;
  private final Annotation qualifier;
  private StreamQueryParameter parameter = new StreamQueryParameter();

  /**
   * @param queryName
   * @param qualifier
   */
  public StreamNamedQueryServices(String queryName, Annotation qualifier) {
    super();
    this.queryName = shouldNotBlank(queryName);
    this.qualifier = qualifier;
  }

  public static StreamNamedQueryServices of(String queryName, Annotation qualifier) {
    return new StreamNamedQueryServices(queryName, qualifier);
  }

  public <T> Stream<List<T>> batch() {
    return StreamUtils.batchStream(parameter.getLimit(), stream());
  }

  public StreamNamedQueryServices context(Map<String, Object> context) {
    parameter.context(context);
    return this;
  }

  public StreamNamedQueryServices context(Object... objects) {
    parameter.context(objects);
    return this;
  }

  public StreamNamedQueryServices criteria(Map<String, Object> context) {
    parameter.criteria(context);
    return this;
  }

  public StreamNamedQueryServices criteria(Object... objects) {
    parameter.criteria(mapOf(objects));
    return this;
  }

  public StreamNamedQueryServices enhancer(BiConsumer<Object, StreamQueryParameter> enhancer) {
    parameter.enhancer(enhancer);
    return this;
  }

  public StreamNamedQueryServices errorTransfer(
      Function<Exception, RuntimeException> errorTransfer) {
    parameter.errorTransfer(errorTransfer);
    return this;
  }

  public StreamNamedQueryServices limit(Integer limit) {
    parameter.limit(limit);
    return this;
  }

  public StreamNamedQueryServices offset(Integer offset) {
    parameter.offset(offset);
    return this;
  }

  public StreamNamedQueryServices retryInterval(Duration retryInterval) {
    parameter.retryInterval(retryInterval);
    return this;
  }

  public StreamNamedQueryServices retryTimes(int retryTimes) {
    parameter.retryTimes(retryTimes);
    return this;
  }

  public <T> Stream<T> stream() {
    if (qualifier == null) {
      return resolve(NamedQueryService.class).stream(queryName, parameter);
    }
    return resolve(NamedQueryService.class, qualifier).stream(queryName, parameter);
  }

  public StreamNamedQueryServices terminater(BiPredicate<Integer, Object> terminater) {
    parameter.terminater(terminater);
    return this;
  }
}