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

import java.net.URL;
import java.util.Map;
import org.corant.config.CorantConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:11:23
 *
 */
public class XmlConfigSource extends CorantConfigSource {

  private static final long serialVersionUID = -6510093356770922600L;

  XmlConfigSource(URL resourceUrl, int ordinal) {}

  @Override
  public Map<String, String> getProperties() {
    return null;
  }

}
