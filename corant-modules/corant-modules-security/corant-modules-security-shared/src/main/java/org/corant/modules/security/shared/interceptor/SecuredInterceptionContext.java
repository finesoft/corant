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
package org.corant.modules.security.shared.interceptor;

import java.io.Serializable;
import java.util.Objects;
import org.corant.modules.security.annotation.SecuredType;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:05:42
 *
 */
public class SecuredInterceptionContext implements Serializable {

  private static final long serialVersionUID = 1584629631984929574L;

  public static final SecuredInterceptionContext ALLOWED_ALL = new SecuredInterceptionContext();

  private final Serializable allowed;

  private final SecuredType type;

  private final String runAs;

  private final boolean denyAll;

  private final boolean allowedAll;

  public SecuredInterceptionContext(Serializable allowed, SecuredType type, String runAs,
      boolean denyAll) {
    this.allowed = allowed;
    this.type = type;
    this.runAs = runAs;
    this.denyAll = denyAll;
    allowedAll = false;
  }

  private SecuredInterceptionContext() {
    allowed = null;
    type = null;
    runAs = null;
    denyAll = false;
    allowedAll = true;
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
    SecuredInterceptionContext other = (SecuredInterceptionContext) obj;
    return Objects.equals(allowed, other.allowed) && allowedAll == other.allowedAll
        && denyAll == other.denyAll && Objects.equals(runAs, other.runAs) && type == other.type;
  }

  public Serializable getAllowed() {
    return allowed;
  }

  public String getRunAs() {
    return runAs;
  }

  public SecuredType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowed, allowedAll, denyAll, runAs, type);
  }

  public boolean isAllowedAll() {
    return allowedAll;
  }

  public boolean isDenyAll() {
    return denyAll;
  }

}
