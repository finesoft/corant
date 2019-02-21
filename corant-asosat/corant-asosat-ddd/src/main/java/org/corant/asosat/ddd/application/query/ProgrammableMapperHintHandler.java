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
package org.corant.asosat.ddd.application.query;

import javax.enterprise.context.ApplicationScoped;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.corant.suites.query.mapping.QueryHint;
import org.corant.suites.query.spi.ResultHintHandler;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
@ApplicationServices
public class ProgrammableMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-mapper";
  public static final String HNIT_SCRIPT = "script-lang";
  public static final String HNIT_SCRIPT_FUNC_PARAM = "script-param";

  @Override
  public void handle(QueryHint qh, Object result) {

  }

}
