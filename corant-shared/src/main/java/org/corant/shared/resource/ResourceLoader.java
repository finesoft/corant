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
package org.corant.shared.resource;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * corant-shared
 *
 * @author bingo 下午3:27:09
 *
 */
public interface ResourceLoader {

  Collection<? extends Resource> load(Object location) throws IOException;

  default Collection<? extends Resource> tryLoad(Object location) {
    try {
      return load(location);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
