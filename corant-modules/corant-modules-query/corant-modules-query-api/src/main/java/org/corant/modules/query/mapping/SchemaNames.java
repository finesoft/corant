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
package org.corant.modules.query.mapping;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午6:24:18
 *
 */
public class SchemaNames {

  public static final String COMMON_SEGMENT = "common-segment";

  public static final String X_NAME = "name";
  public static final String X_VALUE = "value";
  public static final String X_TYPE = "type";
  public static final String X_QUA = "qualifier";
  public static final String X_SRC = "src";
  public static final String X_PARAM = "parameter";
  public static final String X_SCRIPT = "script";
  public static final String X_ENTRY = "entry";
  public static final String X_DESC = "description";
  public static final String X_KEY = "key";
  public static final String X_PROS = "properties";
  public static final String X_PRO = "property";
  public static final String X_DISTINCT = "distinct";
  public static final String X_SINGLE_AS_LIST = "single-as-list";
  public static final String X_GROUP = "group";

  public static final String PARAM_ELE = "parameters-mapping";

  public static final String QUE_MAP_ELE = "query-mapping";
  public static final String QUE_ELE = "query";
  public static final String QUE_FQE_ELE = "fetch-query";
  public static final String QUE_HINT_ELE = "hint";
  public static final String QUE_ATT_RST_CLS = "result-class";
  public static final String QUE_ATT_RST_SET_CLS = "result-set-mapping";
  public static final String QUE_ATT_CACHE = "cache";
  public static final String QUE_ATT_CACHE_RS_MD = "cache-result-set-metadata";
  public static final String QUE_ATT_VER = "version";

  public static final String FQE_ELE_PARAM_ATT_SRC = "source";
  public static final String FQE_ELE_PARAM_ATT_SRC_NME = "source-name";
  public static final String FQE_ELE_PREDICATE_SCRIPT = "predicate-script";
  public static final String FQE_ELE_INJECTION_SCRIPT = "injection-script";
  public static final String FQE_ATT_REF_QUE = "reference-query";
  public static final String FQE_ATT_REF_QUE_TYP = "reference-query-type";
  public static final String FQE_ATT_REF_QUE_QUA = "reference-query-qualifier";
  public static final String FQE_ATT_PRO_NAME = "inject-property-name";
  public static final String FQE_ATT_EAGER_INJECT_NAME = "eager-inject";
  public static final String FQE_ATT_MAX_SIZE = "max-fetch-size";
  public static final String FQE_ATT_VER = "reference-query-version";
  public static final String FQE_ATT_MULT_RECORDS = "multi-records";

  private SchemaNames() {}

}
