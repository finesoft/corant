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
package org.corant.asosat.exp.application;

import static org.corant.shared.util.ObjectUtils.isEquals;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import org.corant.asosat.exp.domain.TestDefaultGenericAggregate;
import org.corant.asosat.exp.provider.TestRepository;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午11:44:27
 *
 */
@ApplicationScoped
public class TestApplicationService3 {

  AtomicLong al = new AtomicLong();

  @Inject
  TestRepository repo;

  @Inject
  @Named("dmmsRwDs")
  DataSource ds;

  // @Inject
  // @RestClient
  // TestRestClientService client;

  @Inject
  TestApplicationService4 s4;

  @Transactional
  public void testRepo(String param) {
    TestDefaultGenericAggregate obj = new TestDefaultGenericAggregate();
    obj.setName("bingo" + al.incrementAndGet());
    repo.persist(obj);
    s4.testRepo(obj.getName() + UUID.randomUUID().toString());
    if (isEquals(param, "0")) {
      s4.testRollback();
    }

    // System.out.println(client.get("1"));
  }

  public void testRollback() {
    TestDefaultGenericAggregate obj = new TestDefaultGenericAggregate();
    obj.setName("jimmy");
    repo.persist(obj);
    throw new CorantRuntimeException("test roll back");
  }
}
