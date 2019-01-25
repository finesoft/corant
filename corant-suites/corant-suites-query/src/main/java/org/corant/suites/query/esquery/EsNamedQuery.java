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
package org.corant.suites.query.esquery;

import java.util.Map;
import org.corant.suites.query.NamedQuery;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:07:55
 *
 */
public interface EsNamedQuery extends NamedQuery {

  Map<String, Object> aggregate(String q, Map<String, Object> param);

  Map<String, Object> search(String q, Map<String, Object> param);

}
