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
package org.corant.config.source;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.net.URL;
import java.util.Map;

/**
 * corant-config
 *
 * @author bingo 上午10:10:55
 *
 */
public class JsonConfigSource extends AbstractCorantConfigSource {

  private static final long serialVersionUID = 4384365167157384602L;

  JsonConfigSource(URL resourceUrl, int ordinal) {
    super(shouldNotNull(resourceUrl).toExternalForm(), ordinal);
    // TODO
  }

  @Override
  public Map<String, String> getProperties() {
    // TODO
    return null;
  }

}
