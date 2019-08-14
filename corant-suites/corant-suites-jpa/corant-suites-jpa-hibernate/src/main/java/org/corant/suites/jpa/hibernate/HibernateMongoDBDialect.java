package org.corant.suites.jpa.hibernate;

import org.corant.suites.jpa.hibernate.type.BigDecimalType;
import org.corant.suites.jpa.hibernate.type.InstantType;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

/**
 * corant-suites-jpa-hibernate <br>
 *
 * @auther sushuaihao 2019/6/12
 * @since
 */
public class HibernateMongoDBDialect extends MongoDBDialect {

  private static final long serialVersionUID = -5387045835552664861L;

  public HibernateMongoDBDialect(MongoDBDatastoreProvider provider) {
    super(provider);
  }

  @Override
  public GridType overrideType(Type type) {
    GridType gt = super.overrideType(type);
    if (gt == null && type == org.hibernate.type.InstantType.INSTANCE) {
      return InstantType.INSTANCE;
    } else if (type == org.hibernate.type.BigDecimalType.INSTANCE) {
      return BigDecimalType.INSTANCE;
    }
    return null;
  }
}
