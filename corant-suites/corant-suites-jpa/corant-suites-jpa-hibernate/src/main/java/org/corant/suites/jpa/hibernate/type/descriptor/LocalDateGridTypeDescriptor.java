package org.corant.suites.jpa.hibernate.type.descriptor;

import static org.corant.shared.util.ConversionUtils.toObject;
import java.time.LocalDate;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午12:52:45
 *
 */
public class LocalDateGridTypeDescriptor implements GridTypeDescriptor {

  public static final LocalDateGridTypeDescriptor INSTANCE = new LocalDateGridTypeDescriptor();

  private static final long serialVersionUID = 991665264844086431L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BasicGridBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
        LocalDate unwrap = javaTypeDescriptor.unwrap(value, LocalDate.class, options);
        resultset.put(names[0], unwrap);
      }
    };
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Object document = resultSet.get(name);
      if (document == null) {
        return null;
      }
      return javaTypeDescriptor.wrap(toObject(document, LocalDate.class), null);
    };
  }

}
