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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.SPACE;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.defaultString;
import java.util.Locale;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.corant.modules.bundle.MessageResolver.MessageParameter;
import org.corant.modules.bundle.MessageResolver.MessageSeverity;
import org.corant.shared.exception.ExceptionMessageResolver;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午12:07:35
 *
 */
public class DefaultExceptionMessageResolver implements ExceptionMessageResolver {

  @Override
  public String getMessage(GeneralRuntimeException exception, Locale locale) {
    Instance<MessageResolver> inst = CDI.current().select(MessageResolver.class);
    if (inst.isResolvable()) {
      return inst.get().getMessage(defaultObject(locale, Locale::getDefault),
          new ExceptionMessageParameter(exception));
    } else {
      return defaultString(exception.getOriginalMessage()) + SPACE
          + asDefaultString(exception.getCode());
    }
  }

  /**
   * corant-modules-bundle
   *
   * @author bingo 下午2:35:56
   *
   */
  static class ExceptionMessageParameter implements MessageParameter {

    final GeneralRuntimeException ex;

    protected ExceptionMessageParameter(GeneralRuntimeException ex) {
      this.ex = ex;
    }

    @Override
    public Object getCodes() {
      return MessageSeverity.ERR.genMessageCode(ex.getCode(), ex.getSubCode());
    }

    @Override
    public String getDefaultMessage(Locale locale) {
      Instance<MessageResolver> inst = CDI.current().select(MessageResolver.class);
      if (inst.isResolvable()) {
        return inst.get().getMessage(locale, UNKNOW_ERR_CODE, new Object[] {getCodes()},
            MessageParameter.super::getDefaultMessage);
      }
      return MessageParameter.super.getDefaultMessage(locale);
    }

    @Override
    public MessageSeverity getMessageSeverity() {
      return MessageSeverity.ERR;
    }

    @Override
    public Object[] getParameters() {
      return ex.getParameters();
    }

  }

}
