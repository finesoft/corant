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

import static org.corant.shared.util.Conversions.toObject;
import java.io.Serializable;
import java.util.Arrays;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午8:20:38
 *
 */
public interface Token extends Serializable {

  class IdentifierToken implements Token {

    private static final long serialVersionUID = 4564497446521891392L;

    protected Serializable id;

    public IdentifierToken(Serializable id) {
      this.id = id;
    }

    protected IdentifierToken() {}

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
      IdentifierToken other = (IdentifierToken) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      return true;
    }

    public Serializable getId() {
      return id;
    }

    public <T extends Serializable> T getId(Class<T> cls) {
      return toObject(id, cls);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (id == null ? 0 : id.hashCode());
    }

  }

  class JsonWebToken implements Token {

    private static final long serialVersionUID = -8729528213796543219L;

    protected String data;

    public JsonWebToken(String data) {
      this.data = data;
    }

    protected JsonWebToken() {}

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
      JsonWebToken other = (JsonWebToken) obj;
      if (data == null) {
        if (other.data != null) {
          return false;
        }
      } else if (!data.equals(other.data)) {
        return false;
      }
      return true;
    }

    public String getData() {
      return data;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (data == null ? 0 : data.hashCode());
    }

  }

  class UsernamePasswordToken extends UsernameToken {

    private static final long serialVersionUID = 8043690813970431112L;

    protected char[] password;

    public UsernamePasswordToken(String username, String password) {
      super(username);
      this.password = password == null ? null : password.toCharArray();
    }

    protected UsernamePasswordToken() {}

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      UsernamePasswordToken other = (UsernamePasswordToken) obj;
      if (!Arrays.equals(password, other.password)) {
        return false;
      }
      return true;
    }

    public char[] getPassword() {
      return password == null ? null : Arrays.copyOf(password, password.length);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      return prime * result + Arrays.hashCode(password);
    }

  }

  class UsernameToken implements Token {

    private static final long serialVersionUID = 3261086381812525002L;

    protected String username;

    protected UsernameToken() {}

    protected UsernameToken(String username) {
      this.username = username;
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
      UsernameToken other = (UsernameToken) obj;
      if (username == null) {
        if (other.username != null) {
          return false;
        }
      } else if (!username.equals(other.username)) {
        return false;
      }
      return true;
    }

    public String getUsername() {
      return username;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (username == null ? 0 : username.hashCode());
    }

  }
}
