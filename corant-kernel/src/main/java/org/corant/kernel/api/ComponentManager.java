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
package org.corant.kernel.api;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.transaction.TransactionScoped;

/**
 * corant-kernel
 *
 * @author bingo 下午5:29:56
 *
 */
public interface ComponentManager<N, C> extends Serializable {

  C computeIfAbsent(N name, Function<N, C> func);

  C get(N name);

  C remove(N name);

  /**
   * corant-kernel
   *
   * @author bingo 下午5:34:28
   *
   */
  public abstract static class AbstractComponentManager<N, C> implements ComponentManager<N, C> {

    private static final long serialVersionUID = -2257315467951134869L;

    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected final transient Map<N, C> components = new ConcurrentHashMap<>();

    @Override
    public C computeIfAbsent(N name, Function<N, C> func) {
      return components.computeIfAbsent(name, func);
    }

    @Override
    public C get(N name) {
      return components.get(name);
    }

    @Override
    public C remove(N name) {
      return components.remove(name);
    }

    @PreDestroy
    protected void onPreDestroy() {
      preDestroy();
    }

    protected abstract void preDestroy();

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
    }
  }

  @RequestScoped
  public abstract static class RsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = -2588026760995417834L;
  }

  @SessionScoped
  public abstract static class SsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = 7462742316873226368L;
  }

  @TransactionScoped
  public abstract static class TsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = -2804585149568989342L;
  }
}
