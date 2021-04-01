/*
 * Copyright (c) 2013-2018. BIN.CHEN
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
package org.corant.modules.bundle;

/**
 * @author bingo 下午2:29:36
 *
 */
public interface GlobalMessageCodes {

  String ERR_SYS = "global.system_error";
  String ERR_PARAM = "global.parameter_error";
  String ERR_UNKNOW = "global.unknown_error";
  String ERR_OP_NON_SUP = "global.operation_not_support_error";
  String ERR_OBJ_SEL = "global.object.serialize_error";
  String ERR_OBJ_NON_FUD = "global.object_not_found_error";
  String ERR_CTX = "global.context_error";

  String INF_OP_SUS = "global.operation_success";
  String INF_OP_FAL = "global.operation_failure";
}
