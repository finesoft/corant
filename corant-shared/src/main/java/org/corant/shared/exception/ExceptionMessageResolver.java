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
package org.corant.shared.exception;

import static org.corant.shared.ubiquity.Atomics.atomicOneOffInitializer;
import static org.corant.shared.util.Conversions.tryConvert;
import static org.corant.shared.util.Maps.getMapObject;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.isNoneNull;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.corant.shared.normal.Defaults;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Chars;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.Resource;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 上午11:48:24
 *
 */
public interface ExceptionMessageResolver extends Sortable {

  String getMessage(GeneralRuntimeException exception, Locale locale);

  static class SimpleExceptionMessageResolver implements ExceptionMessageResolver {

    public static final String SPEC_SOURCE_PATH_KEY =
        Names.CORANT_PREFIX + "exception-message-source-path";
    static final String DEFAULT_SOURCE_PATH = "META-INF/**Messages_*.properties";
    static final Supplier<Map<Locale, Map<String, MessageFormat>>> source =
        atomicOneOffInitializer(SimpleExceptionMessageResolver::collect);

    static Map<Locale, Map<String, MessageFormat>> collect() {
      Map<Locale, Map<String, MessageFormat>> source = new HashMap<>();
      Set<String> paths = setOf(split(
          Systems.getSystemProperty(SPEC_SOURCE_PATH_KEY, String.class, DEFAULT_SOURCE_PATH), ","));
      paths.stream().filter(Strings::isNotBlank).forEach(path -> {
        try {
          Resources.from(path).forEach(rs -> {
            Pair<Locale, Map<String, MessageFormat>> mfs = parse(rs);
            source.computeIfAbsent(mfs.key(), l -> new HashMap<>()).putAll(mfs.value());
          });
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      });
      return source;
    }

    static Pair<Locale, Map<String, MessageFormat>> parse(Resource resource) {
      MutableObject<Locale> locales = new MutableObject<>(null);
      String name;
      int pos = -1;
      if (isNotBlank(name = FileUtils.getFileBaseName(resource.getName()))
          && (pos = name.indexOf(Chars.UNDERSCORE)) > 0) {
        tryConvert(name.substring(pos + 1).replace(Chars.UNDERSCORE, Chars.DASH), Locale.class)
            .ifPresent(locales::set);
      }
      Map<String, MessageFormat> source = new HashMap<>();
      final Locale locale = locales.orElseGet(Locale::getDefault);
      try (InputStream is = resource.openStream();
          InputStreamReader isr = new InputStreamReader(is, Defaults.DFLT_CHARSET)) {
        Properties properties = new Properties();
        properties.load(isr);
        properties.forEach((k, v) -> {
          if (isNoneNull(k, v)) {
            source.put(k.toString(), new MessageFormat(v.toString(), locale));
          }
        });
        properties.clear();
      } catch (IOException e) {
        // Ignore
      }
      return Pair.of(locale, source);
    }

    @Override
    public String getMessage(GeneralRuntimeException exception, Locale locale) {
      String message = null;
      if (exception.getCode() != null) {
        String key = exception.getCode().toString();
        if (exception.getSubCode() != null) {
          key = key.concat(Names.NAME_SPACE_SEPARATORS).concat(exception.getSubCode().toString());
        }
        message = getMessage(locale, key, exception.getParameters());
      }
      return defaultString(message, exception.getOriginalMessage());
    }

    public String getMessage(Locale locale, String key, Object... parameters) {
      MessageFormat mf =
          (MessageFormat) getMapObject(source.get().get(defaultObject(locale, Locale::getDefault)),
              key);
      if (mf != null) {
        return mf.format(parameters);
      }
      return null;
    }

    @Override
    public int getPriority() {
      return Integer.MAX_VALUE;
    }

  }
}
