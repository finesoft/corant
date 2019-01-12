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
package org.corant.asosat.exp.provider;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.asosat.ddd.message.MemonyMessageTesting;
import org.corant.asosat.ddd.message.MessageSender;
import org.corant.asosat.ddd.pattern.interceptor.Retry;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;
import org.corant.suites.ddd.repository.JpaRepository;

/**
 * @author bingo 上午11:04:00
 *
 */
@ApplicationScoped
@InfrastructureServices
public class TestMessageSender implements MessageSender {

  @Inject
  @JPA
  protected JpaRepository repo;

  public TestMessageSender() {}

  @Retry(exceptions = IOException.class)
  @Transactional
  @Override
  public boolean send(ExchangedMessage message) throws Exception {
    MemonyMessageTesting.test(message);
    return true;
  }


}
