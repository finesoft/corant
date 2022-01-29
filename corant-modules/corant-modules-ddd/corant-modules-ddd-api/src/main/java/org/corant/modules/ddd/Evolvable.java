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
package org.corant.modules.ddd;

import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-ddd-api
 *
 * <p>
 * Represents an object that can be persisted or deleted.
 *
 * @author bingo 下午9:05:07
 *
 */
public interface Evolvable<P, T> {

  /**
   * delete the object from persistence
   *
   * @param Param
   * @param handler destroy
   */
  void destroy(P Param, DestroyingHandler<P, T> handler);

  /**
   * preserve to underly persistence
   *
   * @param param
   * @param handler
   * @return preserve
   */
  T preserve(P param, PreservingHandler<P, T> handler);

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:04:45
   *
   */
  abstract class AggregateHandlerAdapter<P, T extends Aggregate>
      extends PreservingHandlerAdapter<P, T> implements DestroyingHandler<P, T> {

    @Override
    public void preDestroy(P param, T destroyable) {}

  }

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:05:13
   *
   */
  @FunctionalInterface
  interface DestroyingHandler<P, T> extends Sortable {
    @SuppressWarnings("rawtypes")
    DestroyingHandler EMPTY_INST = (p, t) -> {
    };

    void preDestroy(P param, T enabling);
  }

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:05:19
   *
   */
  abstract class DestroyingHandlerAdapter<P, T> implements DestroyingHandler<P, T> {
    @Override
    public void preDestroy(P param, T enabling) {}
  }

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:05:13
   *
   */
  @FunctionalInterface
  interface PreservingHandler<P, T> extends Sortable {
    @SuppressWarnings("rawtypes")
    PreservingHandler EMPTY_INST = (p, t) -> {
    };

    void prePreserve(P param, T enabling);
  }

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:05:19
   *
   */
  abstract class PreservingHandlerAdapter<P, T> implements PreservingHandler<P, T> {
    @Override
    public void prePreserve(P param, T enabling) {}
  }

}
