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
package org.corant.modules.security.shared;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import org.corant.modules.security.Principal;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:56:13
 */
public class UserAuthcData extends SimpleAuthcData {

  private static final long serialVersionUID = -7319705255292058038L;

  protected Serializable userId;

  public UserAuthcData(Serializable userId, Object credentials,
      Collection<? extends Principal> principals) {
    super(credentials, principals);
    this.userId = userId;
  }

  public UserAuthcData(Serializable userId, Object credentials,
      Collection<? extends Principal> principals, Map<String, ? extends Serializable> attributes) {
    super(credentials, principals, attributes);
    this.userId = userId;
  }

  protected UserAuthcData() {}

  public Serializable getUserId() {
    return userId;
  }

}
