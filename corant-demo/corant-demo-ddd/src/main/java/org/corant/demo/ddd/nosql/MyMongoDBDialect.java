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
package org.corant.demo.ddd.nosql;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.type.impl.TimestampType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.InstantType;
import org.hibernate.type.Type;

/**
 * corant-demo-ddd
 *
 * @author bingo 上午10:26:48
 *
 */
public class MyMongoDBDialect extends MongoDBDialect {

  private static final long serialVersionUID = -301352951174479792L;

  /**
   * @param provider
   */
  public MyMongoDBDialect(MongoDBDatastoreProvider provider) {
    super(provider);
  }

  @Override
  public GridType overrideType(Type type) {
    GridType gt = super.overrideType(type);
    if (gt == null && type == InstantType.INSTANCE) {
      return TimestampType.INSTANCE;
    }
    return null;
  }

}
