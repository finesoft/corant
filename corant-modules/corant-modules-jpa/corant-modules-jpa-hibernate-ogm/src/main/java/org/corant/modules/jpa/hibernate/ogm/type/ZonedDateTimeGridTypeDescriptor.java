package org.corant.modules.jpa.hibernate.ogm.type;

import static org.corant.shared.util.Conversions.toObject;
import java.time.ZonedDateTime;
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
 * @author bingo 下午12:52:50
 *
 */
public class ZonedDateTimeGridTypeDescriptor implements GridTypeDescriptor {

  public static final ZonedDateTimeGridTypeDescriptor INSTANCE =
      new ZonedDateTimeGridTypeDescriptor();

  private static final long serialVersionUID = 991665264844086431L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new ZonedDateTimeBasicGridBinder<>(javaTypeDescriptor, this, javaTypeDescriptor);
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Object document = resultSet.get(name);
      if (document == null) {
        return null;
      }
      return javaTypeDescriptor.wrap(toObject(document, ZonedDateTime.class), null);
    };
  }

  /**
   * corant-modules-jpa-hibernate-ogm
   *
   * @author bingo 下午3:39:43
   *
   */
  private static final class ZonedDateTimeBasicGridBinder<X> extends BasicGridBinder<X> {
    private final JavaTypeDescriptor<X> javaTypeDescriptor;

    /**
     * @param javaDescriptor
     * @param gridDescriptor
     * @param javaTypeDescriptor
     */
    private ZonedDateTimeBasicGridBinder(JavaTypeDescriptor<X> javaDescriptor,
        GridTypeDescriptor gridDescriptor, JavaTypeDescriptor<X> javaTypeDescriptor) {
      super(javaDescriptor, gridDescriptor);
      this.javaTypeDescriptor = javaTypeDescriptor;
    }

    @Override
    protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
      ZonedDateTime unwrap = javaTypeDescriptor.unwrap(value, ZonedDateTime.class, options);
      resultset.put(names[0], unwrap);
    }
  }

}
