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
package org.corant.modules.security.annotation;

import static java.util.Collections.unmodifiableCollection;
import static org.corant.config.Configs.assemblyStringConfigProperty;
import static org.corant.config.Configs.getValue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.EMPTY_ARRAY;
import static org.corant.shared.util.Strings.defaultBlank;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultTrim;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.corant.config.Configs;
import org.corant.shared.util.Strings;

public class SecuredMetadata implements Serializable {

  private static final long serialVersionUID = -2874685620910996061L;

  public static final SecuredMetadata ALLOW_ALL_INST =
      new SecuredMetadata(EMPTY, EMPTY, EMPTY_ARRAY, false);

  public static final SecuredMetadata DENY_ALL_INST =
      new SecuredMetadata(EMPTY, EMPTY, EMPTY_ARRAY, true);

  public static final String DEFAULT_SECURED_TYPE_CFG_NAME = "corant.secutiry.secured.type";

  private final Collection<String> allowed;

  private final SecuredType type;

  private final String runAs;

  private final boolean denyAll;

  private final int hash;

  public SecuredMetadata(Secured secured) {
    this(secured.type(), secured.runAs(), secured.allowed(), secured.denyAll());
  }

  public SecuredMetadata(String type, String runAs, String[] allowed, boolean denyAll) {
    this.type = SecuredType.valueOf(defaultBlank(assemblyStringConfigProperty(type),
        getValue(DEFAULT_SECURED_TYPE_CFG_NAME, String.class, SecuredType.ROLE.name())).strip());
    this.runAs = defaultTrim(assemblyStringConfigProperty(defaultString(runAs)));
    Collection<String> aws = Arrays.stream(defaultObject(allowed, EMPTY_ARRAY))
        .map(Configs::assemblyStringConfigProperties).flatMap(List::stream)
        .filter(Strings::isNotBlank).map(String::strip).collect(Collectors.toList());
    this.allowed = unmodifiableCollection(aws);
    this.denyAll = denyAll;
    hash = Objects.hash(this.allowed, this.denyAll, this.runAs, this.type);
  }

  public Collection<String> allowed() {
    return allowed;
  }

  public boolean denyAll() {
    return denyAll;
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
    SecuredMetadata other = (SecuredMetadata) obj;
    return Objects.equals(allowed, other.allowed) && denyAll == other.denyAll
        && Objects.equals(runAs, other.runAs) && type == other.type;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  public String runAs() {
    return runAs;
  }

  @Override
  public String toString() {
    return "SecuredMetadata [allowed=" + allowed + ", type=" + type + ", runAs=" + runAs
        + ", denyAll=" + denyAll + "]";
  }

  public SecuredType type() {
    return type;
  }
}
