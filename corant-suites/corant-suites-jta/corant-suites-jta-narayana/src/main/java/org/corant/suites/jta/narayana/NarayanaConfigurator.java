/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jta.narayana;

import org.corant.config.ComparableConfigurator;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午10:46:42
 *
 */
public interface NarayanaConfigurator extends ComparableConfigurator {

  void configCoordinatorEnvironment(CoordinatorEnvironmentBean bean);

  void configCoreEnvironment(CoreEnvironmentBean bean);

  void configObjectStoreEnvironment(ObjectStoreEnvironmentBean bean, String name);

  void configRecoveryEnvironment(RecoveryEnvironmentBean bean);

}
