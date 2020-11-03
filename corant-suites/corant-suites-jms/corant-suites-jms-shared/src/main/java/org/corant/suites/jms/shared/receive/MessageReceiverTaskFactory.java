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
package org.corant.suites.jms.shared.receive;

import javax.enterprise.context.ApplicationScoped;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 14:59:51
 *
 */
public interface MessageReceiverTaskFactory {

  CancellableTask create(MessageReceiverMetaData metaData);

  interface CancellableTask extends Runnable {

    boolean cancel();
  }

  @ApplicationScoped
  public static class DefaultMessageReceiverTaskFactory implements MessageReceiverTaskFactory {

    @Override
    public CancellableTask create(MessageReceiverMetaData metaData) {
      return new DefaultMessageReceiverTask(metaData);
    }

  }

}
