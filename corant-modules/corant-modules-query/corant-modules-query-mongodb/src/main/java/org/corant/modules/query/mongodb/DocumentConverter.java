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
package org.corant.modules.query.mongodb;

import java.util.Map;
import org.bson.Document;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 下午7:44:08
 *
 */
@FunctionalInterface
public interface DocumentConverter extends Sortable {
  Map<String, Object> convert(Document document, MgNamedQuerier querier);
}
