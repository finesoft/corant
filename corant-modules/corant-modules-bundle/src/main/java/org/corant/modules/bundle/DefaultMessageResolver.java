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
package org.corant.modules.bundle;

import static org.corant.modules.bundle.MessageResolver.MessageParameter.UNKNOW_ERR_CODE;
import static org.corant.modules.bundle.MessageResolver.MessageParameter.UNKNOW_INF_CODE;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;

/**
 * corant-modules-bundle
 *
 * FIXME: Need to simplify
 *
 * @author bingo 下午8:01:13
 */
@ApplicationScoped
public class DefaultMessageResolver implements MessageResolver {

  protected static final DateTimeFormatter DATE_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(Locale.getDefault())
          .withZone(ZoneId.systemDefault());

  @Inject
  @Any
  protected Instance<MessageSource> messageSources;

  @Inject
  @Any
  protected Instance<EnumerationSource> enumerationSources;

  public Object[] genParameters(Locale locale, Object[] parameters) {
    if (parameters.length > 0) {
      return Arrays.stream(parameters).map(p -> handleParameter(locale, p)).toArray();
    }
    return Objects.EMPTY_ARRAY;
  }

  @Override
  public String getMessage(Locale locale, MessageParameter messageParameter) {
    if (messageParameter == null) {
      return null;
    }
    String codes = asDefaultString(messageParameter.getCodes());
    Locale useLocale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = genParameters(useLocale, messageParameter.getParameters());
    MutableString ms = MutableString.of(null);
    if (!messageSources.isUnsatisfied()) {
      messageSources.stream().sorted(Sortable::compare)
          .map(b -> b.getMessage(useLocale, codes, parameters)).filter(Strings::isNotBlank)
          .findFirst().ifPresent(ms::set);
    }
    return defaultObject(ms.get(),
        () -> getUnknowMessage(useLocale, messageParameter.getMessageSeverity(), codes));
  }

  @Override
  public String getMessage(Locale locale, Object codes, Object... params) {
    Locale useLocale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = genParameters(useLocale, params);
    MutableString ms = MutableString.of(null);
    if (!messageSources.isUnsatisfied()) {
      messageSources.stream().sorted(Sortable::compare)
          .map(b -> b.getMessage(useLocale, codes, parameters)).filter(Strings::isNotBlank)
          .findFirst().ifPresent(ms::set);
    }
    return defaultObject(ms.get(), () -> String.format("Can't find any message for %s.", codes));
  }

  public String getUnknowMessage(Locale locale, MessageSeverity ser, Object code) {
    String unknow = ser == MessageSeverity.INF ? UNKNOW_INF_CODE : UNKNOW_ERR_CODE;

    MutableString ms = MutableString.of(null);
    if (!messageSources.isUnsatisfied()) {
      messageSources.stream().sorted(Sortable::compare)
          .map(b -> b.getMessage(locale, unknow, new Object[] {code})).filter(Strings::isNotBlank)
          .findFirst().ifPresent(ms::set);
    }
    return defaultObject(ms.get(), () -> String.format("Can't find any message for %s.", code));
  }

  @SuppressWarnings("rawtypes")
  protected Object handleParameter(Locale locale, Object obj) {
    if (obj instanceof Enum) {
      MutableString ms = MutableString.of(null);
      if (!enumerationSources.isUnsatisfied()) {
        enumerationSources.stream().sorted(Sortable::compare)
            .map(b -> b.getEnumItemLiteral((Enum) obj, locale)).filter(Strings::isNotBlank)
            .findFirst().ifPresent(ms::set);
      }
      String literal = ms.get();
      return literal == null ? obj : literal;
    } else if (obj instanceof Instant || obj instanceof ZonedDateTime) {
      return DATE_TIME_FMT.format((TemporalAccessor) obj);
    } else if (obj instanceof Readable) {
      return ((Readable) obj).toHumanReader(locale);
    }
    return obj;
  }

  @Produces
  @MessageCodes
  @Dependent
  protected String produce(InjectionPoint ip) {
    String codes = null;
    Locale locale = Locale.getDefault();
    for (Annotation ann : ip.getQualifiers()) {
      if (ann instanceof MessageCodes) {
        MessageCodes mc = (MessageCodes) ann;
        codes = mc.value();
        if (isNotBlank(mc.locale())) {
          locale = LocaleUtils.langToLocale(mc.locale(), PropertyResourceBundle.LOCALE_SPT_CHAR);
        }
      }
    }
    if (isNotBlank(codes)) {
      return getMessage(locale, codes, Objects.EMPTY_ARRAY);
    }
    return null;
  }

}
