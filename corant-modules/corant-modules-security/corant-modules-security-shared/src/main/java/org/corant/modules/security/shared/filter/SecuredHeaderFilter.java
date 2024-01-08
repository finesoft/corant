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
package org.corant.modules.security.shared.filter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.corant.modules.security.shared.SecurityExtension;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Functions;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:09:10
 */
@ApplicationScoped
public class SecuredHeaderFilter implements Consumer<BiConsumer<String, String>> {

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<SecuredHeaderFilterHandler> handlers;

  protected volatile Consumer<BiConsumer<String, String>> chainHandler;

  @Override
  public void accept(BiConsumer<String, String> t) {
    if (t != null) {
      if (SecurityExtension.CACHE_FILTER_HANDLERS && chainHandler != null) {
        chainHandler.accept(t);
      } else {
        composeHandlers().accept(t);
      }
    }
  }

  protected Consumer<BiConsumer<String, String>> composeHandlers() {
    if (!handlers.isUnsatisfied()) {
      return handlers.stream().sorted(Sortable::compare)
          .map(h -> (Consumer<BiConsumer<String, String>>) h)
          .reduce(Functions.emptyConsumer(), Consumer::andThen);
    }
    return Functions.emptyConsumer();
  }

  @PostConstruct
  protected synchronized void onPostConstruct() {
    if (!handlers.isUnsatisfied() && chainHandler == null
        && SecurityExtension.CACHE_FILTER_HANDLERS) {
      chainHandler = composeHandlers();
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    chainHandler = null;
  }
}
