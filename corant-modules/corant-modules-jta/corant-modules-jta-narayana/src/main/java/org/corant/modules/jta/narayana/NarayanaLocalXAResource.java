/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jta.narayana;

import org.corant.modules.jta.shared.TransactionAwareness;
import org.corant.modules.jta.shared.TransactionIntegrator.LocalXAResource;
import org.jboss.tm.LastResource;

/**
 * corant-modules-jta-narayana
 *
 * @author bingo 11:18:58
 */
public class NarayanaLocalXAResource extends LocalXAResource implements LastResource {

  public NarayanaLocalXAResource(TransactionAwareness transactionAwareness) {
    super(transactionAwareness);
  }

}
