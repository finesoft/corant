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

import static java.util.Collections.unmodifiableMap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.corant.modules.security.annotation.SecuredMetadata;
import org.corant.modules.security.annotation.SecuredType;
import org.corant.modules.security.shared.AttributeSet;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:05:42
 *
 */
public interface SecuredInterceptionContext extends Serializable, AttributeSet {

  void clearAttributes();

  Collection<String> getMetaAllowed();

  Serializable getResolvedAllowed();

  String getRunAs();

  SecuredType getType();

  boolean isDenyAll();

  Serializable putAttribute(String name, Serializable value);

  Serializable removeAttribute(String name);

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午11:36:06
   *
   */
  class DefaultSecuredInterceptionContext implements SecuredInterceptionContext {

    private static final long serialVersionUID = 2148235114223231675L;

    protected final Serializable allowed;

    protected final SecuredMetadata meta;

    protected final Map<String, Serializable> attributes = new HashMap<>();

    public DefaultSecuredInterceptionContext(Serializable allowed, SecuredMetadata meta) {
      this.allowed = allowed;
      this.meta = meta;
    }

    @Override
    public void clearAttributes() {
      attributes.clear();
    }

    @Override
    public Map<String, ? extends Serializable> getAttributes() {
      return unmodifiableMap(attributes);
    }

    @Override
    public Collection<String> getMetaAllowed() {
      return meta.allowed();
    }

    @Override
    public Serializable getResolvedAllowed() {
      return allowed;
    }

    @Override
    public String getRunAs() {
      return meta.runAs();
    }

    @Override
    public SecuredType getType() {
      return meta.type();
    }

    @Override
    public boolean isDenyAll() {
      return meta.denyAll();
    }

    @Override
    public Serializable putAttribute(String name, Serializable value) {
      return attributes.put(name, value);
    }

    @Override
    public Serializable removeAttribute(String name) {
      return attributes.remove(name);
    }

  }
}
