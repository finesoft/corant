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
package org.corant.context;

import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.transaction.TransactionScoped;
import org.jboss.weld.environment.se.contexts.ThreadScoped;

/**
 * corant-context
 * <p>
 * A CDI component management class for hosting the creation and destruction of various bean
 * instances using CDI-related scopes. These instances can be obtained by specifying a unique name
 * key. These instances are also destroyed through the scope of CDI, and manual destruction is also
 * supported.
 *
 * @author bingo 下午5:29:56
 */
public interface ComponentManager<N, C> extends Serializable {

  C computeIfAbsent(N key, Function<N, C> func);

  C get(N key);

  default C get(N key, C alt) {
    return defaultObject(get(key), alt);
  }

  C put(N key, C component);

  C remove(N key);

  /**
   * corant-kernel
   *
   * @author bingo 下午5:34:28
   */
  abstract class AbstractComponentManager<N, C> implements ComponentManager<N, C> {

    private static final long serialVersionUID = -2257315467951134869L;

    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected final transient Map<N, C> components = new ConcurrentHashMap<>();

    @Override
    public C computeIfAbsent(N key, Function<N, C> func) {
      return components.computeIfAbsent(key, func);
    }

    @Override
    public C get(N key) {
      return components.get(key);
    }

    @Override
    public C put(N key, C component) {
      return components.put(key, component);
    }

    @Override
    public C remove(N key) {
      return components.remove(key);
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

  /**
   * corant-context
   * <p>
   * Request scoped component manager
   *
   * @author bingo 上午10:50:08
   */
  @RequestScoped
  abstract class RsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = -2588026760995417834L;
  }

  /**
   * corant-context
   * <p>
   * Session scoped component manager
   *
   * @author bingo 上午10:50:29
   */
  @SessionScoped
  abstract class SsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = 7462742316873226368L;
  }

  /**
   * corant-context
   * <p>
   * Thread scoped component manager
   *
   * @author bingo 上午10:50:50
   */
  @ThreadScoped
  abstract class ThComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = -5758319290954516372L;
  }

  /**
   * corant-context
   * <p>
   * Transaction scoped component manager
   *
   * @author bingo 上午10:51:21
   */
  @TransactionScoped
  abstract class TsComponentManager<N, C> extends AbstractComponentManager<N, C> {
    private static final long serialVersionUID = -2804585149568989342L;
  }
}
