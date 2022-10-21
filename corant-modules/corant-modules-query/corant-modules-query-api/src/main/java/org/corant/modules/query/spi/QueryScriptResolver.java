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
package org.corant.modules.query.spi;

/**
 * corant-modules-query-api
 *
 * <p>
 * A query script resolver which use to generate the query script and query parameters.
 * <p>
 * In general the query script resolver is a CDI bean with named qualifiers and its bean scope is
 * ApplicationScope, the name of qualifier is generally the name of a particular query.
 *
 *
 * @author bingo 上午10:12:25
 *
 */
@FunctionalInterface
public interface QueryScriptResolver {

  /**
   * Returns a query script and query parameters object
   *
   * @param parameter the query parameter
   * @return a query script
   */
  Object resolve(Object parameter);

}
