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
/**
 * corant-suites-ddd
 *
 * @author bingo 下午3:49:10
 *
 */
package org.corant.suites.ddd.model;

import org.corant.suites.bundle.GlobalMessageCodes;

class PkgMsgCds implements GlobalMessageCodes {
  static final String ERR_AGG_LC = "aggregate.lifecycle_state_error";
  static final String ERR_AGG_AST_INSTAL = "aggregate.assistant_install_error";
  static final String ERR_AGG_MSG_SEQ = "aggregate.msgSeqNum_error";
  static final String ERR_AGG_ID = "aggregate.identifier_error";

  private PkgMsgCds() {}

}
