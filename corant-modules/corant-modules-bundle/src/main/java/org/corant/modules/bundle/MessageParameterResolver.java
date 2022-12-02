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
package org.corant.modules.bundle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Strings;

/**
 * corant-modules-bundle
 *
 * <p>
 * Interface for resolving message parameters, mainly used to increase its readability and
 * internationalization for some message parameters.
 *
 * @author bingo 下午1:14:50
 *
 */
public interface MessageParameterResolver extends Sortable {

  Object handle(Locale locale, Object obj);

  boolean supprots(Object obj);

  @ApplicationScoped
  public static class EnumMessageParameterResolver implements MessageParameterResolver {

    @Inject
    @Any
    protected Instance<EnumerationSource> enumerationSources;

    @Override
    public Object handle(Locale locale, Object obj) {
      MutableString ms = MutableString.of(null);
      if (!enumerationSources.isUnsatisfied()) {
        enumerationSources.stream().sorted(Sortable::compare)
            .map(b -> b.getEnumItemLiteral((Enum<?>) obj, locale)).filter(Strings::isNotBlank)
            .findFirst().ifPresent(ms::set);
      }
      String literal = ms.get();
      return literal == null ? obj : literal;
    }

    @Override
    public boolean supprots(Object obj) {
      return obj instanceof Enum;
    }

  }

  @ApplicationScoped
  public static class InstantMessageParameterResolver implements MessageParameterResolver {

    static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    @Override
    public Object handle(Locale locale, Object obj) {
      return DATE_TIME_FMT.format((Instant) obj);
    }

    @Override
    public boolean supprots(Object obj) {
      return obj instanceof Instant;
    }

  }

  @ApplicationScoped
  public static class ReadableMessageParameterResolver implements MessageParameterResolver {

    @Override
    public Object handle(Locale locale, Object obj) {
      return ((Readable<?>) obj).toHumanReader(locale);
    }

    @Override
    public boolean supprots(Object obj) {
      return obj instanceof Readable;
    }

  }

  @ApplicationScoped
  public static class ZonedDateTimeMessageParameterResolver implements MessageParameterResolver {

    static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    @Override
    public Object handle(Locale locale, Object obj) {
      return DATE_TIME_FMT.format((ZonedDateTime) obj);
    }

    @Override
    public boolean supprots(Object obj) {
      return obj instanceof ZonedDateTime;
    }

  }
}
