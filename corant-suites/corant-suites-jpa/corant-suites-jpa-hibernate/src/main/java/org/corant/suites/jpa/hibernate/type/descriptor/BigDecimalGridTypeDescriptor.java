package org.corant.suites.jpa.hibernate.type.descriptor;

import static org.corant.shared.util.ConversionUtils.toBigDecimal;
import java.math.BigDecimal;
import org.bson.types.Decimal128;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * cps-m2b <br>
 *
 * @auther sushuaihao 2019/8/14
 * @since
 */
public class BigDecimalGridTypeDescriptor implements GridTypeDescriptor {

  public static final BigDecimalGridTypeDescriptor INSTANCE = new BigDecimalGridTypeDescriptor();

  private static final long serialVersionUID = -3323272062776948180L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BasicGridBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
        BigDecimal unwrap = javaTypeDescriptor.unwrap(value, BigDecimal.class, options);
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
      } else if (document instanceof Decimal128) {
        return javaTypeDescriptor
            .wrap(toBigDecimal(Decimal128.class.cast(document).bigDecimalValue()), null);
      } else if (Double.class.isInstance(document)) {
        return javaTypeDescriptor.wrap(toBigDecimal(document), null);
      } else if (BigDecimal.class.isInstance(document)) {
        return javaTypeDescriptor.wrap(toBigDecimal(document), null);
      } else {
        return javaTypeDescriptor.wrap(toBigDecimal(document.toString()), null);
      }
    };
  }

}
