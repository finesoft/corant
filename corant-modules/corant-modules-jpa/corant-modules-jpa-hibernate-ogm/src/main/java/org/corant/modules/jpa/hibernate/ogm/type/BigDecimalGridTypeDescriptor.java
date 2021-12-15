package org.corant.modules.jpa.hibernate.ogm.type;

import static org.corant.shared.util.Conversions.toBigDecimal;
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
 * corant-modules-jpa-hibernate-ogm
 *
 * @author sushuaihao 2019/8/14
 * @since
 */
public class BigDecimalGridTypeDescriptor implements GridTypeDescriptor {

  public static final BigDecimalGridTypeDescriptor INSTANCE = new BigDecimalGridTypeDescriptor();

  private static final long serialVersionUID = -3323272062776948180L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BigDecimalBasicGridBinder<>(javaTypeDescriptor, this, javaTypeDescriptor);
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Object document = resultSet.get(name);
      if (document == null) {
        return null;
      } else if (document instanceof Decimal128) {
        return javaTypeDescriptor.wrap(toBigDecimal(((Decimal128) document).bigDecimalValue()),
            null);
      } else if ((document instanceof Double) || (document instanceof BigDecimal)) {
        return javaTypeDescriptor.wrap(toBigDecimal(document), null);
      } else {
        return javaTypeDescriptor.wrap(toBigDecimal(document.toString()), null);
      }
    };
  }

  /**
   * corant-modules-jpa-hibernate-ogm
   *
   * @author bingo 下午3:38:06
   *
   */
  private static final class BigDecimalBasicGridBinder<X> extends BasicGridBinder<X> {
    private final JavaTypeDescriptor<X> javaTypeDescriptor;

    /**
     * @param javaDescriptor
     * @param gridDescriptor
     * @param javaTypeDescriptor
     */
    private BigDecimalBasicGridBinder(JavaTypeDescriptor<X> javaDescriptor,
        GridTypeDescriptor gridDescriptor, JavaTypeDescriptor<X> javaTypeDescriptor) {
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
