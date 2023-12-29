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
package org.corant.shared.normal;

/**
 * corant-shared
 *
 * @author bingo 上午10:20:20
 */
public interface Priorities {

  int APPLICATION_HIGHER = 2000;
  int APPLICATION_LOWER = 2999;
  int FRAMEWORK_HIGHER = 200;
  int FRAMEWORK_LOWER = 5000;
  int MODULES_HIGHER = 1000;
  int MODULES_LOWER = 1999;

  interface ConfigPriorities {
    int FRAMEWORK_DEFAULTS_ORDINAL = -1000;
    int APPLICATION_ORDINAL = 200;
    int APPLICATION_PROFILE_ORDINAL = 250;
    int SYSTEM_ENVIRONMENT_ORDINAL = 300;
    int SYSTEM_PROPERTIES_ORDINAL = 400;
    int APPLICATION_ADJUST_ORDINAL = 1000;

  }

  interface PostReadyEventPriorities {

  }

}
