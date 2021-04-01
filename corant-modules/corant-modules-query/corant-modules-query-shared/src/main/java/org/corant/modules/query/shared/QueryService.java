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
package org.corant.modules.query.shared;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午10:33:30
 *
 */
public interface QueryService<Q, P> {

  /**
   * Step forward query, do not pagination, do not calculate the total number of records, can be
   * used for mass data streaming processing.
   *
   * @param <T> The expected query result set record type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The result set and whether there is a flag for the next result set.
   */
  <T> Forwarding<T> forward(Q q, P p);

  /**
   * Like {@link #forward(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result set record type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The result set and whether there is a flag for the next result set.
   */
  default <R, T> Forwarding<T> forward(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    Forwarding<R> raw = forward(q, p);
    if (raw == null) {
      return Forwarding.inst();
    } else {
      Forwarding<T> result = new Forwarding<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> f.apply(i, this)).collect(Collectors.toList()))
          .withHasNext(raw.hasNext);
    }
  }

  /**
   * Query a single result.
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The query result object.
   */
  <T> T get(Q q, P p);

  /**
   * Like {@link #get(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result object.
   */
  default <R, T> T get(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    R t = get(q, p);
    return f.apply(t, this);
  }

  /**
   * Paging query, returns the query result set and total record counts etc.
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The paging result list.
   */
  <T> Paging<T> page(Q q, P p);

  /**
   * Like {@link #page(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result object.
   */
  default <R, T> Paging<T> page(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    Paging<R> raw = page(q, p);
    if (raw == null) {
      return Paging.inst();
    } else {
      Paging<T> result = new Paging<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> f.apply(i, this)).collect(Collectors.toList()))
          .withTotal(raw.total);
    }
  }

  /**
   * Query list results
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The query result list
   */
  <T> List<T> select(Q q, P p);

  /**
   * Like {@link #select(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result list
   */
  default <R, T> List<T> select(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    List<R> raw = select(q, p);
    if (raw == null) {
      return new ArrayList<>();
    } else {
      return raw.stream().map(i -> f.apply(i, this)).collect(Collectors.toList());
    }
  }

  /**
   * Query stream results, use for mass data query.
   *
   * <p>
   * NOTE: In order to release related resources, please remember to close after using the stream.
   *
   * <pre>
   * Example: try(Stream stream = QueryService.stream(q,p)){
   *    stream.forEach(row->{
   *        //do somthing
   *    })
   * }
   * </pre>
   *
   * @param <T>
   * @param q
   * @param p
   * @return stream
   */
  <T> Stream<T> stream(Q q, P p);

  /**
   * corant-suites-query-shared
   *
   * Forward query result list, Consists of the result list and has next result mark.
   *
   * @author bingo 下午5:51:56
   *
   */
  class Forwarding<T> {

    private boolean hasNext;
    private final List<T> results = new ArrayList<>();

    Forwarding() {}

    public static <T> Forwarding<T> inst() {
      return new Forwarding<>();
    }

    public static <T> Forwarding<T> of(List<T> results, boolean hasNext) {
      Forwarding<T> il = new Forwarding<>();
      return il.withResults(results).withHasNext(hasNext);
    }

    /**
     * The result list
     */
    public List<T> getResults() {
      return results;
    }

    /**
     * Returns {@code true} if the query has more result set.
     *
     * @return hasNext
     */
    public boolean hasNext() {
      return hasNext;
    }

    public boolean hasResults() {
      return isNotEmpty(results);
    }

    /**
     * @return the hasNext
     */
    public boolean isHasNext() {
      return hasNext;
    }

    public Forwarding<T> with(Forwarding<T> other) {
      if (other != null) {
        withHasNext(other.hasNext());
        withResults(other.getResults());
      } else {
        withHasNext(false);
        withResults(new ArrayList<>());
      }
      return this;
    }

    public Forwarding<T> withHasNext(boolean hasNext) {
      this.hasNext = hasNext;
      return this;
    }

    public Forwarding<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

  }

  /**
   * corant-suites-query-shared
   *
   * Paging query result list.
   *
   * @author bingo 下午6:11:55
   *
   */
  class Paging<T> {

    private int total;
    private int pageSize;
    private int currentPage;
    private int totalPages;
    private int offset;
    private List<T> results = new ArrayList<>();

    public static <T> Paging<T> inst() {
      return new Paging<>();
    }

    public static <T> Paging<T> of(int offset, int pageSize) {
      Paging<T> pl = new Paging<>();
      return pl.withOffset(offset).withPageSize(pageSize);
    }

    public static <T> Paging<T> of(int total, List<T> results, int offset, int pageSize) {
      Paging<T> pl = new Paging<>();
      return pl.withResults(results).withTotal(total).withOffset(offset).withPageSize(pageSize);
    }

    /**
     * @return the currentPage
     */
    public int getCurrentPage() {
      return currentPage;
    }

    public int getOffset() {
      return offset;
    }

    /**
     * @return the pageSize
     */
    public int getPageSize() {
      return pageSize;
    }

    /**
     * @return the data
     */
    public List<T> getResults() {
      return results;
    }

    /**
     * @return the total
     */
    public int getTotal() {
      return total;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public boolean hasResults() {
      return isNotEmpty(results);
    }

    public Paging<T> withOffset(int offset) {
      this.offset = offset;
      calPages();
      return this;
    }

    public Paging<T> withPageSize(int pageSize) {
      this.pageSize = pageSize;
      calPages();
      return this;
    }

    public Paging<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

    public Paging<T> withTotal(int total) {
      this.total = total;
      calPages();
      return this;
    }

    void calPages() {
      if (pageSize > 0 && offset >= 0) {
        if (total > 0) {
          totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        }
        int i = offset + 1;
        currentPage = i % pageSize == 0 ? i / pageSize : i / pageSize + 1;
      }
    }

  }

  enum QueryWay {

    /**
     * @see QueryService#forward(Object, Object)
     */
    FORWARD,
    /**
     * @see QueryService#get(Object, Object)
     */
    GET,
    /**
     * @see QueryService#page(Object, Object)
     */
    PAGE,
    /**
     * @see QueryService#select(Object, Object)
     */
    SELECT,
    /**
     * @see QueryService#stream(Object, Object)
     */
    STREAM;

    public static QueryWay fromMethodName(String methodName) {
      if (methodName != null) {
        for (QueryWay qw : QueryWay.values()) {
          if (methodName.startsWith(qw.name().toLowerCase(Locale.ENGLISH))) {
            return qw;
          }
        }
      }
      return SELECT;
    }
  }
}
