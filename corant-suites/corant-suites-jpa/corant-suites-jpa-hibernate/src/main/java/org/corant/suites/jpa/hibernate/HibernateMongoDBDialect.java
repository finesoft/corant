package org.corant.suites.jpa.hibernate;

import org.corant.suites.jpa.hibernate.types.InstantType;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

/**
 * corant-root <br>
 *
 * @auther sushuaihao 2019/6/12
 * @since
 */
public class HibernateMongoDBDialect extends MongoDBDialect {

  /** @param provider */
  public HibernateMongoDBDialect(MongoDBDatastoreProvider provider) {
    super(provider);
  }

  @Override
  public GridType overrideType(Type type) {
    GridType gt = super.overrideType(type);
    if (gt == null && type == org.hibernate.type.InstantType.INSTANCE) {
      return InstantType.INSTANCE;
    }
    return null;
  }
}
