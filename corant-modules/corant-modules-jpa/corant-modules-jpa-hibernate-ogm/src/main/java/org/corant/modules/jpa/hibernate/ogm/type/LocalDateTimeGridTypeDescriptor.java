package org.corant.modules.jpa.hibernate.ogm.type;

import static org.corant.shared.util.Conversions.toObject;
import java.time.LocalDateTime;
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
public class LocalDateTimeGridTypeDescriptor implements GridTypeDescriptor {

  public static final LocalDateTimeGridTypeDescriptor INSTANCE =
      new LocalDateTimeGridTypeDescriptor();

  private static final long serialVersionUID = 991665264844086431L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new LocalDateTimeBasicGridBinder<>(javaTypeDescriptor, this, javaTypeDescriptor);
  }

  @Override
  public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return (resultSet, name) -> {
      Object document = resultSet.get(name);
      if (document == null) {
        return null;
      }
      return javaTypeDescriptor.wrap(toObject(document, LocalDateTime.class), null);
    };
  }

  /**
   * corant-modules-jpa-hibernate-ogm
   *
   * @author bingo 下午3:39:28
   *
   */
  private static final class LocalDateTimeBasicGridBinder<X> extends BasicGridBinder<X> {
    private final JavaTypeDescriptor<X> javaTypeDescriptor;

    /**
     * @param javaDescriptor
     * @param gridDescriptor
     * @param javaTypeDescriptor
     */
    private LocalDateTimeBasicGridBinder(JavaTypeDescriptor<X> javaDescriptor,
        GridTypeDescriptor gridDescriptor, JavaTypeDescriptor<X> javaTypeDescriptor) {
      super(javaDescriptor, gridDescriptor);
      this.javaTypeDescriptor = javaTypeDescriptor;
    }

    @Override
    protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
      LocalDateTime unwrap = javaTypeDescriptor.unwrap(value, LocalDateTime.class, options);
      resultset.put(names[0], unwrap);
    }
  }

}
