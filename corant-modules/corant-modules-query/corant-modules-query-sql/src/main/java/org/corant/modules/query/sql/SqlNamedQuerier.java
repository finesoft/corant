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
package org.corant.modules.query.sql;

import org.corant.modules.query.shared.NamedQuerier;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午7:11:05
 *
 */
public interface SqlNamedQuerier extends DynamicQuerier<Object[], String>, NamedQuerier {
}
