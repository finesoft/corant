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
package org.corant.modules.security;

import java.io.Serializable;
import java.util.Arrays;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午8:20:38
 *
 */
public interface Token extends Serializable {

  class UsernamePasswordToken implements Token {

    private static final long serialVersionUID = 8043690813970431112L;

    private final String username;
    private final char[] password;

    public UsernamePasswordToken(String username, String password) {
      this.username = username;
      this.password = password == null ? null : password.toCharArray();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      UsernamePasswordToken other = (UsernamePasswordToken) obj;
      if (!Arrays.equals(password, other.password)) {
        return false;
      }
      if (username == null) {
        if (other.username != null) {
          return false;
        }
      } else if (!username.equals(other.username)) {
        return false;
      }
      return true;
    }

    public char[] getPassword() {
      return password == null ? null : Arrays.copyOf(password, password.length);
    }

    public String getUsername() {
      return username;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(password);
      result = prime * result + (username == null ? 0 : username.hashCode());
      return result;
    }

  }
}
