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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.shared.ubiquity.Mutable.MutableObject;
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

  @Inject
  Logger logger;

  @Inject
  @Any
  protected Instance<MessageSource> messageSources;

  @Inject
  @Any
  protected Instance<MessageParameterResolver> parameterResolver;

  public Object[] genParameters(Locale locale, Object[] parameters) {
    if (parameters != null && parameters.length > 0) {
      return Arrays.stream(parameters).map(p -> handleParameter(locale, p)).toArray();
    }
    return Objects.EMPTY_ARRAY;
  }

  @Override
  public String getMessage(Locale locale, MessageParameter parameter) {
    if (parameter == null) {
      throw new NoSuchMessageException("Can't find any message.");
    }
    return getMessage(locale, parameter.getCodes(), parameter.getParameters(),
        parameter::getDefaultMessage);
  }

  @Override
  public String getMessage(Locale locale, Object codes, Object... params) {
    return getMessage(locale, codes, params, null);
  }

  @Override
  public String getMessage(Locale locale, Object codes, Object[] params,
      Function<Locale, String> dfltMsgResolver) {
    Locale useLocale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = genParameters(useLocale, params);
    String msg = null;
    if (!messageSources.isUnsatisfied()) {
      msg = messageSources.stream().sorted(Sortable::compare)
          .map(b -> b.getMessage(useLocale, codes, parameters, s -> null))
          .filter(Strings::isNotBlank).findFirst().orElse(null);
    }
    if (msg == null) {
      logger.warning(() -> String.format("Can't find any message for %s", codes));
      if (dfltMsgResolver != null) {
        msg = dfltMsgResolver.apply(locale);
      } else {
        throw new NoSuchMessageException("Can't find any message for %s.", codes);
      }
    }
    return msg;
  }

  protected Object handleParameter(Locale locale, Object obj) {
    MutableObject<Object> mo = new MutableObject<>(obj);
    if (!parameterResolver.isUnsatisfied()) {
      parameterResolver.stream().filter(h -> h.supprots(obj)).sorted(Sortable::compare)
          .forEach(h -> mo.apply(o -> h.handle(locale, o)));
    }
    return mo.get();
  }

  @Produces
  @MessageCodes
  @Dependent
  protected String produce(InjectionPoint ip) {
    String codes = null;
    Locale locale = Locale.getDefault();
    Object[] parameters = Objects.EMPTY_ARRAY;
    for (Annotation ann : ip.getQualifiers()) {
      if (ann instanceof MessageCodes) {
        MessageCodes mc = (MessageCodes) ann;
        codes = mc.value();
        if (isNotBlank(mc.locale())) {
          locale = LocaleUtils.langToLocale(mc.locale(), PropertyResourceBundle.LOCALE_SPT_CHAR);
        }
        parameters = mc.parameters();
        break;
      }
    }
    if (isNotBlank(codes)) {
      return getMessage(locale, codes, parameters);
    }
    throw new NoSuchMessageException("Can't find any message");
  }

}
