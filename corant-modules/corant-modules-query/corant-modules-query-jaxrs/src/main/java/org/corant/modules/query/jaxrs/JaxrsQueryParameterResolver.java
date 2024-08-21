/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import java.util.function.Function;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier.JaxrsQueryParameter;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 17:30:01
 */
@FunctionalInterface
public interface JaxrsQueryParameterResolver
    extends Function<QueryParameter, JaxrsQueryParameter>, Sortable {

}
