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
import jakarta.enterprise.context.ApplicationScoped;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:22:28
 */
@ApplicationScoped
public class XSSURIEscapeContentHandler implements SecuredContentFilterHandler {

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
        case ' ':
          escaped.append("%20");
          break;
        case '!':
          escaped.append("%21");
          break;
        case '"':
          escaped.append("%22");
          break;
        case '#':
          escaped.append("%23");
          break;
        case '$':
          escaped.append("%24");
          break;
        case '%':
          escaped.append("%25");
          break;
        case '&':
          escaped.append("%26");
          break;
        case '\'':
          escaped.append("%27");
          break;
        case '(':
          escaped.append("%28");
          break;
        case ')':
          escaped.append("%29");
          break;
        case '*':
          escaped.append("%2A");
          break;
        case '+':
          escaped.append("%2B");
          break;
        case ',':
          escaped.append("%2C");
          break;
        case '.':
          escaped.append("%2E");
          break;
        case '/':
          escaped.append("%2F");
          break;
        case ':':
          escaped.append("%3A");
          break;
        case ';':
          escaped.append("%3B");
          break;
        case '<':
          escaped.append("%3C");
          break;
        case '=':
          escaped.append("%3D");
          break;
        case '>':
          escaped.append("%3E");
          break;
        case '?':
          escaped.append("%3F");
          break;
        case '@':
          escaped.append("%40");
          break;
        case '[':
          escaped.append("%5B");
          break;
        case ']':
          escaped.append("%5D");
          break;
        default:
          escaped.append(c);
          break;
      }
    }
    return escaped.toString();
  }

}
