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

import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Locale;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Objects;

/**
 * corant-kernel
 *
 * @author bingo 下午9:28:49
 *
 */
public interface MessageResolver {

  Object[] EMPTY_PARAM = Objects.EMPTY_ARRAY;

  static String genMessageCode(MessageSeverity severity, Object... codes) {
    StringBuilder sb = new StringBuilder(severity.name());
    for (Object code : codes) {
      String cs;
      if (code != null && isNotBlank(cs = code.toString())) {
        sb.append(Names.NAME_SPACE_SEPARATORS).append(cs);
      }
    }
    return sb.toString();
  }

  String getMessage(Locale locale, MessageSource messageSource);

  String getMessage(Locale locale, Object codes, Object... params);

  public enum MessageSeverity {
    INF, ERR;

    public String genMessageCode(Object... codes) {
      return MessageResolver.genMessageCode(this, codes);
    }
  }

  public interface MessageSource {

    String UNKNOW_INF_CODE = "INF.message.unknow";
    String UNKNOW_ERR_CODE = "ERR.message.unknow";

    Object getCodes();

    MessageSeverity getMessageSeverity();

    default Object[] getParameters() {
      return EMPTY_PARAM;
    }
  }

}
