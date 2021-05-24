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
 * corant-modules-ddd-shared
 *
 * @author bingo 下午4:01:00
 *
 */
package org.corant.modules.ddd.shared.unitwork;

import org.corant.modules.bundle.GlobalMessageCodes;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午3:28:01
 *
 */
class PkgMsgCds implements GlobalMessageCodes {

  static final String ERR_UOW_TRANS = "defaultUnitOfWorks.transaction_error";
  static final String ERR_UOW_NOT_ACT = "defaultUnitOfWorks_error_not_activated";
  static final String ERR_UOW_CREATE = "defaultUnitOfWorks.create_error";

  private PkgMsgCds() {}
}
