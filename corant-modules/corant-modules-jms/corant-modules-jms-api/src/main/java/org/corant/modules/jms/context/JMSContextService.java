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
package org.corant.modules.jms.context;

import jakarta.jms.JMSContext;

/**
 * corant-modules-jms-api
 *
 * <pre>
 * If an injected JMSContext is used in a JTA transaction (both bean-managed and container-managed),
 * its scope will be that of the transaction. This means that: The JMSContext object will be
 * automatically created the first time it is used within the transaction.
 *
 * The JMSContext object will be automatically closed when the transaction is committed.
 *
 * If, within the same JTA transaction, different beans, or different methods within the same bean,
 * use an injected JMSContext which is injected using identical annotations then they will all share
 * the same JMSContext object.
 *
 * If an injected JMSContext is used when there is no JTA transaction then its scope will be the
 * existing CDI scope @RequestScoped. This means that: The JMSContext object will be created the
 * first time it is used within a request.
 *
 * The JMSContext object will be closed when the request ends.
 *
 * If, within the same request, different beans, or different methods within the same bean, use an
 * injected JMSContext which is injected using identical annotations then they will all share the
 * same JMSContext object.
 *
 * If injected JMSContext is used both in a JTA transaction and outside a JTA transaction then
 * separate JMSContext objects will be used, with a separate JMSContext object being used for each
 * JTA transaction as described above.
 * </pre>
 *
 * @see <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p1">Proposed
 *      changes to JMSContext to support injection (Option 4)-1</a>
 * @see <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p2">Proposed
 *      changes to JMSContext to support injection (Option 4)-2</a>
 * @see <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p3">Proposed
 *      changes to JMSContext to support injection (Option 4)-3</a>
 * @see <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p4">Proposed
 *      changes to JMSContext to support injection (Option 4)-4</a>
 *
 * @author bingo 上午11:21:30
 *
 */
public interface JMSContextService {

  default JMSContext getJMSContext(String connectionFactoryId) {
    return getJMSContext(connectionFactoryId, false);
  }

  JMSContext getJMSContext(String connectionFactoryId, boolean dupsOkAck);

}
