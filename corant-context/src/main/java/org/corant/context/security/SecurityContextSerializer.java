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
package org.corant.context.security;

import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.util.Serializations;

/**
 * corant-context
 *
 * @author bingo 下午3:29:59
 *
 */
public interface SecurityContextSerializer {

  SecurityContext deserialize(String securityContext);

  String serialize(SecurityContext securityContext);

  public class Base64SecurityContextSerializer implements SecurityContextSerializer {

    public static final Base64SecurityContextSerializer INSTANCE =
        new Base64SecurityContextSerializer();

    static final Logger logger = Logger.getLogger(Base64SecurityContextSerializer.class.getName());

    private Base64SecurityContextSerializer() {}

    @Override
    public SecurityContext deserialize(String securityContext) {
      if (isNotBlank(securityContext)) {
        try {
          byte[] bytes = Base64.getDecoder().decode(securityContext);
          return (SecurityContext) Serializations.deserialize(bytes);
        } catch (Exception e) {
          logger.log(Level.SEVERE, e, () -> "Can't deserialize security context!");
        }
      }
      return null;
    }

    @Override
    public String serialize(SecurityContext securityContext) {
      if (securityContext != null) {
        try {
          byte[] bytes = Serializations.serialize(securityContext);
          return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
          logger.log(Level.SEVERE, e, () -> "Can't serialize security context!");
        }
      }
      return null;
    }

  }
}
