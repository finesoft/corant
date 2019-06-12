package org.corant.suites.jpa.hibernate.types.descriptor;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import java.time.Instant;
import java.util.Date;

import static org.corant.shared.util.ConversionUtils.toInstant;

/**
 * corant-root <br>
 *
 * @auther sushuaihao 2019/6/12
 * @since
 */
public class InstantGridTypeDescriptor implements GridTypeDescriptor {

  public static final InstantGridTypeDescriptor INSTANCE = new InstantGridTypeDescriptor();

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BasicGridBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
        Instant unwrap = javaTypeDescriptor.unwrap(value, Instant.class, options);
        resultset.put(names[0], unwrap);
      }
    };
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Date document = (Date) resultSet.get(name);
      if (document == null) {
        return null;
      }
      return javaTypeDescriptor.wrap(toInstant(document), null);
    };
  }
}
