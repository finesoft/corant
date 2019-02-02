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
package org.corant.suites.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * corant-asosat-query
 *
 * @author bingo 下午10:33:30
 *
 */
public interface Query<Q, P> {

  <T> ForwardList<T> forward(Q q, P param);

  default <R, T> ForwardList<T> forward(Q q, P param, BiFunction<R, Query<Q, P>, T> func) {
    ForwardList<R> raw = forward(q, param);
    if (raw == null) {
      return ForwardList.inst();
    } else {
      ForwardList<T> result = new ForwardList<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> func.apply(i, this)).collect(Collectors.toList()))
          .withHasNext(raw.hasNext);
    }
  }

  <T> T get(Q q, P param);

  default <R, T> T get(Q q, P param, BiFunction<R, Query<Q, P>, T> func) {
    R t = get(q, param);
    return func.apply(t, this);
  }

  <T> PagedList<T> page(Q q, P param);

  default <R, T> PagedList<T> page(Q q, P param, BiFunction<R, Query<Q, P>, T> func) {
    PagedList<R> raw = page(q, param);
    if (raw == null) {
      return PagedList.inst();
    } else {
      PagedList<T> result = new PagedList<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> func.apply(i, this)).collect(Collectors.toList()))
          .withTotal(raw.total);
    }
  }

  <T> List<T> select(Q q, P param);

  default <R, T> List<T> select(Q q, P param, BiFunction<R, Query<Q, P>, T> func) {
    List<R> raw = select(q, param);
    if (raw == null) {
      return new ArrayList<>();
    } else {
      return raw.stream().map(i -> func.apply(i, this)).collect(Collectors.toList());
    }
  }

  <T> Stream<T> stream(Q q, P param);

  public static class ForwardList<T> {

    private boolean hasNext;
    private final List<T> results = new ArrayList<>();

    ForwardList() {}

    public static <T> ForwardList<T> inst() {
      return new ForwardList<>();
    }

    public static <T> ForwardList<T> of(List<T> results, boolean hasNext) {
      ForwardList<T> il = new ForwardList<>();
      return il.withResults(results).withHasNext(hasNext);
    }

    /**
     * @return the data
     */
    public List<T> getResults() {
      return results;
    }

    public boolean hasNext() {
      return false;
    }

    /**
     * @return the hasNext
     */
    public boolean isHasNext() {
      return hasNext;
    }

    public ForwardList<T> withHasNext(boolean hasNext) {
      this.hasNext = hasNext;
      return this;
    }

    public ForwardList<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

  }

  public static class PagedList<T> {

    private int total;
    private int pageSize;
    private int currentPage;
    private int totalPages;
    private int offset;
    private List<T> results = new ArrayList<>();

    public static <T> PagedList<T> inst() {
      return new PagedList<>();
    }

    public static <T> PagedList<T> of(int offset, int pageSize) {
      PagedList<T> pl = new PagedList<>();
      return pl.withOffset(offset).withPageSize(pageSize);
    }

    public static <T> PagedList<T> of(int total, List<T> results, int offset, int pageSize) {
      PagedList<T> pl = new PagedList<>();
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

    public PagedList<T> withOffset(int offset) {
      this.offset = offset;
      calPages();
      return this;
    }

    public PagedList<T> withPageSize(int pageSize) {
      this.pageSize = pageSize;
      calPages();
      return this;
    }

    public PagedList<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

    public PagedList<T> withTotal(int total) {
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
}
