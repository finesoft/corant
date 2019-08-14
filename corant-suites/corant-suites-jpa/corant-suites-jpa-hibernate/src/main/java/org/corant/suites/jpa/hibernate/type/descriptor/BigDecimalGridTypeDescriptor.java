package org.corant.suites.jpa.hibernate.type.descriptor;

import static org.corant.shared.util.ConversionUtils.toBigDecimal;
import java.math.BigDecimal;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * corant-suites-jpa-hibernate <br>
 *
 * @auther sushuaihao 2019/8/14
 * @since
 */
public class BigDecimalGridTypeDescriptor implements GridTypeDescriptor {
  public static final BigDecimalGridTypeDescriptor INSTANCE = new BigDecimalGridTypeDescriptor();
  private static final long serialVersionUID = -3323272062776948180L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BigDecimalGridBinder<>(javaTypeDescriptor, this, javaTypeDescriptor);
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Double document = (Double) resultSet.get(name);
      if (document == null) {
        return null;
      }
      return javaTypeDescriptor.wrap(toBigDecimal(document), null);
    };
  }

  static final class BigDecimalGridBinder<X> extends BasicGridBinder<X> {
    private final JavaTypeDescriptor<X> javaTypeDescriptor;

    /**
     * @param javaDescriptor
     * @param gridDescriptor
     * @param javaTypeDescriptor
     */
    BigDecimalGridBinder(JavaTypeDescriptor<X> javaDescriptor, GridTypeDescriptor gridDescriptor,
        JavaTypeDescriptor<X> javaTypeDescriptor) {
      super(javaDescriptor, gridDescriptor);
      this.javaTypeDescriptor = javaTypeDescriptor;
    }

    @Override
    protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
      BigDecimal unwrap = javaTypeDescriptor.unwrap(value, BigDecimal.class, options);
      resultset.put(names[0], unwrap);
    }
  }
}
