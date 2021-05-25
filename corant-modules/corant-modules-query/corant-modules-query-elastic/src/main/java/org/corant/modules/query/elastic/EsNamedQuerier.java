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
package org.corant.modules.query.elastic;

import java.util.Map;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午10:54:47
 *
 */
public interface EsNamedQuerier extends DynamicQuerier<Map<String, Object>, Map<Object, Object>> {

  String[] getHintKeys();

  String getIndexName();

  @Override
  Map<Object, Object> getScript(Map<?, ?> additionals);

}
