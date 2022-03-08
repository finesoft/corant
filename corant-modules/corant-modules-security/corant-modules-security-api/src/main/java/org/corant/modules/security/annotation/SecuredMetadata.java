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

import static org.corant.config.Configs.assemblyStringConfigProperty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.EMPTY_ARRAY;
import static org.corant.shared.util.Strings.defaultBlank;
import static org.corant.shared.util.Strings.defaultString;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.corant.config.Configs;

public class SecuredMetadata implements Serializable {

  private static final long serialVersionUID = -2874685620910996061L;

  public static final SecuredMetadata EMPTY_INST = new SecuredMetadata();

  public static final String DEFAULT_SECURED_TYPE = "${corant.secutiry.secured.type:ROLE}";

  Collection<String> allowed = Collections.emptyList();

  String type = SecuredType.ROLE.name();

  String runAs = EMPTY;

  public SecuredMetadata(Secured secured) {
    this(secured.type(), secured.runAs(), secured.allowed());
  }

  public SecuredMetadata(String type, String runAs, String[] allowed) {
    this.type = defaultBlank(assemblyStringConfigProperty(type),
        defaultBlank(assemblyStringConfigProperty(DEFAULT_SECURED_TYPE), SecuredType.ROLE.name()));
    this.runAs = assemblyStringConfigProperty(defaultString(runAs));
    this.allowed = Collections.unmodifiableList(Arrays.stream(defaultObject(allowed, EMPTY_ARRAY))
        .map(Configs::assemblyStringConfigProperties).flatMap(List::stream)
        .collect(Collectors.toList()));
  }

  protected SecuredMetadata() {}

  public Collection<String> allowed() {
    return allowed;
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
    return Objects.equals(allowed, other.allowed) && Objects.equals(runAs, other.runAs)
        && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowed, runAs, type);
  }

  public String runAs() {
    return runAs;
  }

  @Override
  public String toString() {
    return "SecuredMetadata [allowed=[" + String.join(",", allowed) + "], type=" + type + ", runAs="
        + runAs + "]";
  }

  public String type() {
    return type;
  }

}
