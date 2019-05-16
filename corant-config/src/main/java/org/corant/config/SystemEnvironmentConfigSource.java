/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.config;

import java.util.Collections;
import java.util.Map;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Priorities;

/**
 * corant-config
 *
 * @author bingo 上午11:04:36
 *
 */
public class SystemEnvironmentConfigSource extends AbstractConfigSource {

  final Map<String, String> sysPros = Collections.unmodifiableMap(System.getenv());

  public SystemEnvironmentConfigSource() {
    super();
    name = "System.environment";
    ordinal = Priorities.ConfigPriorities.SYSTEM_ENVIRONMENT_ORGINAL;
  }

  @Override
  public Map<String, String> getProperties() {
    return sysPros;
  }

  @Override
  AbstractConfigSource withProperties(Map<String, String> properties) {
    throw new NotSupportedException("Can not adjust system environment!");
  }

}