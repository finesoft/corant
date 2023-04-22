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
package org.corant.modules.jta.xadisk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XASession;

/**
 * corant-modules-jta-xadisk
 *
 * Unfinished yet
 *
 * @author bingo 上午1:00:55
 *
 */
@ApplicationScoped
public class XADiskSessionProducer {

  @Inject
  TransactionManager transactionManager;

  @Inject
  XAFileSystem fileSystem;

  final transient Map<Transaction, XASession> sessions = new ConcurrentHashMap<>();

  @Produces
  @TransactionScoped
  XASession produce() {
    return fileSystem.createSessionForXATransaction();
  }
}
