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
package org.corant.modules.security.shared.filter;

import static org.corant.shared.util.Strings.isBlank;
import javax.enterprise.context.ApplicationScoped;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:22:28
 *
 */
@ApplicationScoped
public class XSSHTMLEscapeContentHandler implements SecuredContentFilterHandler {

  @Override
  public String apply(String html) {
    if (isBlank(html)) {
      return html;
    }
    int length = html.length();
    StringBuilder escaped = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = html.charAt(i);
      switch (c) {
        case '\'':
          escaped.append("&#39;");
          break;
        case '\\':
          escaped.append("&#x5C;");
          break;
        case ' ':
          escaped.append("&#x20;");
          break;
        case '/':
          escaped.append("&#x2F;");
          break;
        case '"':
          escaped.append("&quot;");
          break;
        case '<':
          escaped.append("&lt;");
          break;
        case '>':
          escaped.append("&gt;");
          break;
        case '&':
          escaped.append("&amp;");
          break;
        default:
          escaped.append(c);
          break;
      }
    }
    return escaped.toString();
  }

}
