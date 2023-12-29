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
package org.corant.modules.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.corant.config.source.JsonConfigSourceResolver;
import org.corant.shared.util.Texts;

/**
 * corant-modules-json
 *
 * @author bingo 下午4:19:30
 */
public class JsonConfigSourceResolverImpl implements JsonConfigSourceResolver {

  @Override
  public Map<String, Object> resolve(InputStream is) throws IOException {
    return Jsons.fromString(Texts.fromInputStream(is));
  }

}
