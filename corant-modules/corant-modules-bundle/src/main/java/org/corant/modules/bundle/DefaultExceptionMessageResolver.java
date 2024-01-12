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
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import org.corant.modules.bundle.MessageResolver.MessageCategory;
import org.corant.shared.exception.ExceptionMessageResolver;
import org.corant.shared.exception.GeneralRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-bundle
 *
 * <p>
 * Default exception message resolver, use {@link MessageResolver} CDI bean to resolve the exception
 * message.
 *
 * @author bingo 下午12:07:35
 */
public class DefaultExceptionMessageResolver implements ExceptionMessageResolver {

  @Override
  public String getMessage(Exception exception, Locale locale) {
    if (exception instanceof GeneralRuntimeException) {
      GeneralRuntimeException gre = (GeneralRuntimeException) exception;
      if (gre.getMessageKey() == null && gre.getOriginalMessage() != null) {
        return gre.getOriginalMessage();
      }
      Instance<MessageResolver> inst = CDI.current().select(MessageResolver.class);
      if (!inst.isUnsatisfied()) {
        final MessageResolver resolver;
        if (inst.isResolvable()) {
          resolver = inst.get();
        } else {
          resolver = inst.stream().sorted(Sortable::compare).findFirst().get();
        }
        // FIXME Do we need to append category prefix to message keys?
        final Object key = MessageCategory.ERR.genMessageKey(gre.getMessageKey());
        final Object[] parameters = gre.getMessageParameters();
        return resolver.getMessage(locale, key, parameters,
            l1 -> resolver.getMessage(l1, MessageResolver.UNKNOWN_ERR_KEY, parameters,
                l2 -> MessageResolver.getNoFoundMessage(l2, key)));
      } else {
        return defaultString(gre.getOriginalMessage()) + SPACE
            + asDefaultString(gre.getMessageKey());
      }
    }
    return defaultObject(exception.getLocalizedMessage(), exception::getMessage);
  }

}
