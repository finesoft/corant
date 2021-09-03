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

import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:09:10
 *
 */
@Singleton
public class XSSFilter implements Function<String, String> {

  @Inject
  @Any
  protected Instance<XSSFilterHandler> handlers;

  protected Function<String, String> chainHandler;

  @Override
  public String apply(String content) {
    if (content == null || chainHandler == null) {
      return content;
    }
    return chainHandler.apply(content);
  }

  @PostConstruct
  protected synchronized void onPostConstruct() {
    if (!handlers.isUnsatisfied() && chainHandler == null) {
      chainHandler = handlers.stream().sorted(Sortable::compare)
          .map(h -> (Function<String, String>) h).reduce(s -> s, Function::andThen);
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    chainHandler = null;
  }
}
