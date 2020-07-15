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
package org.corant.suites.bundle;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.suites.bundle.MessageResolver.MessageSource.UNKNOW_ERR_CODE;
import static org.corant.suites.bundle.MessageResolver.MessageSource.UNKNOW_INF_CODE;
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
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 * corant-suites-bundle
 *
 * @author bingo 下午8:01:13
 */
@ApplicationScoped
public class DefaultMessageResolver implements MessageResolver {

  static final Object[] EMPTY_ARGS = new Object[0];

  static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm")
      .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

  @Inject
  protected PropertyMessageBundle messageBundle;

  @Inject
  protected EnumerationBundle enumBundle;

  public Object[] genParameters(Locale locale, Object[] parameters) {
    if (parameters.length > 0) {
      return Arrays.stream(parameters).map(p -> handleParameter(locale, p)).toArray();
    }
    return new Object[0];
  }

  @Override
  public String getMessage(Locale locale, MessageSource messageSource) {
    if (messageSource == null) {
      return null;
    }
    String codes = asDefaultString(messageSource.getCodes());
    locale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = genParameters(locale, messageSource.getParameters());
    return messageBundle.getMessage(locale, codes, parameters,
        l -> getUnknowMessage(l, messageSource.getMessageSeverity(), codes));
  }

  @Override
  public String getMessage(Locale locale, Object codes, Object... params) {
    locale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = genParameters(locale, params);
    return messageBundle.getMessage(locale, codes, parameters,
        l -> String.format("Can't find any message for %s", codes));
  }

  public String getUnknowMessage(Locale locale, MessageSeverity ser, Object code) {
    String unknow = ser == MessageSeverity.INF ? UNKNOW_INF_CODE : UNKNOW_ERR_CODE;
    return messageBundle.getMessage(locale, unknow, new Object[] {code},
        l -> String.format("Can't find any message for %s", code));
  }

  @SuppressWarnings("rawtypes")
  protected Object handleParameter(Locale locale, Object obj) {
    if (obj instanceof Enum) {
      String literal = enumBundle.getEnumItemLiteral((Enum) obj, locale);
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
      return getMessage(locale, codes, MessageResolver.EMPTY_PARAM);
    }
    return null;
  }

}
