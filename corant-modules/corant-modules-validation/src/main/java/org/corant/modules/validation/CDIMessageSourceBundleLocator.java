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
package org.corant.modules.validation;

import static java.util.Collections.enumeration;
import static org.corant.context.Beans.find;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.corant.modules.bundle.MessageSource;
import org.corant.modules.bundle.MessageSourceManager;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.util.Functions;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

/**
 * corant-modules-validation
 *
 * @author bingo 上午11:54:07
 */
@Experimental
public class CDIMessageSourceBundleLocator implements ResourceBundleLocator {

  protected Map<Locale, MessageSourceBundle> sources = new ConcurrentHashMap<>();

  protected volatile boolean initialized = false;

  @Override
  public ResourceBundle getResourceBundle(Locale locale) {
    initialized();
    return sources.get(locale);
  }

  protected void initialized() {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          Optional<MessageSourceManager> resolver = find(MessageSourceManager.class);
          if (resolver.isPresent()) {
            Map<Locale, List<MessageSource>> map = new HashMap<>();
            resolver.get().stream().forEach(ms -> {
              for (Locale locale : ms.getLocales()) {
                map.computeIfAbsent(locale, k -> new ArrayList<>()).add(ms);
              }
            });
            sources.clear();
            map.forEach((k, mss) -> sources.put(k, new MessageSourceBundle(k, mss)));
          }
          initialized = true;
        }
      }
    }
  }

  /**
   * corant-modules-validation
   *
   * @author bingo 下午3:43:08
   *
   */
  public static class MessageSourceBundle extends ResourceBundle {

    final List<MessageSource> sources;
    final Locale locale;

    public MessageSourceBundle(Locale locale, List<MessageSource> sources) {
      this.sources = sources;
      this.locale = locale;
    }

    @Override
    public Enumeration<String> getKeys() {
      Set<String> keys = new LinkedHashSet<>();
      for (MessageSource source : sources) {
        keys.addAll(source.getKeys(locale));
      }
      return enumeration(keys);
    }

    @Override
    public Locale getLocale() {
      return locale;
    }

    @Override
    protected Object handleGetObject(String key) {
      Object message = null;
      for (MessageSource source : sources) {
        message = source.getMessage(locale, key, Functions.emptyFunction());
        if (message != null) {
          break;
        }
      }
      return message;
    }
  }
}
