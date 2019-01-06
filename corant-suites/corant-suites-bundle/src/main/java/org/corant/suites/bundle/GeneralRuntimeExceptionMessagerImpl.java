/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.bundle;

import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.isEmpty;
import java.util.Arrays;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.kernel.exception.GeneralRuntimeExceptionMessager;

/**
 * corant-suites-bundle
 *
 * @author bingo 下午4:05:08
 *
 */
@ApplicationScoped
public class GeneralRuntimeExceptionMessagerImpl implements GeneralRuntimeExceptionMessager {

  static final Object[] EMPTY_ARGS = new Object[0];

  @Inject
  private PropertyMessageBundle messageBundle;

  @Inject
  private EnumerationBundle enumBundle;

  public String genMessageKey(String code, String subCode) {
    StringBuilder sb = new StringBuilder(MessageSeverity.ERR.name()).append(".").append(code);
    if (!isEmpty(subCode)) {
      sb.append(".").append(subCode);
    }
    return sb.toString();
  }


  public Object[] genParameters(Locale locale, Object[] parameters) {
    if (parameters.length > 0) {
      return Arrays.stream(parameters).map(p -> handleParameter(locale, p)).toArray();
    }
    return new Object[0];
  }

  @Override
  public String getMessage(Locale locale, GeneralRuntimeException exception) {
    if (exception == null) {
      return null;
    }
    String key = genMessageKey(asDefaultString(exception.getCode()),
        asDefaultString(exception.getSubCode()));
    Locale localeToUse = locale == null ? Locale.getDefault() : locale;
    Object[] parameters = genParameters(localeToUse, exception.getParameters());
    return messageBundle.getMessage(localeToUse, key, parameters, (l) -> getUnknowErrorMessage(l));
  }

  @Override
  public String getUnknowErrorMessage(Locale locale) {
    return messageBundle.getMessage(locale, genMessageKey(GlobalMessageCodes.ERR_UNKNOW, null),
        EMPTY_ARGS);
  }

  @SuppressWarnings("rawtypes")
  private Object handleParameter(Locale locale, Object obj) {
    if (obj instanceof Enum) {
      String literal = enumBundle.getEnumItemLiteral((Enum) obj, locale);
      return literal == null ? obj : literal;
    } else if (obj instanceof Readable) {
      return ((Readable) obj).toHumanReader(locale);
    }
    return obj;
  }

}
