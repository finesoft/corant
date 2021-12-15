package org.corant.modules.jpa.hibernate.ogm.type;

import static org.corant.shared.util.Conversions.toInstant;
import java.time.Instant;
import java.util.Date;
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
 * @author sushuaihao 2019/6/12
 * @since
 */
public class InstantGridTypeDescriptor implements GridTypeDescriptor {

  public static final InstantGridTypeDescriptor INSTANCE = new InstantGridTypeDescriptor();

  private static final long serialVersionUID = 991665264844086431L;

  @Override
  public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new InstantBasicGridBinder<>(javaTypeDescriptor, this, javaTypeDescriptor);
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

  /**
   * corant-modules-jpa-hibernate-ogm
   *
   * @author bingo 下午3:38:43
   *
   */
  private static final class InstantBasicGridBinder<X> extends BasicGridBinder<X> {
    private final JavaTypeDescriptor<X> javaTypeDescriptor;

    /**
     * @param javaDescriptor
     * @param gridDescriptor
     * @param javaTypeDescriptor
     */
    private InstantBasicGridBinder(JavaTypeDescriptor<X> javaDescriptor,
        GridTypeDescriptor gridDescriptor, JavaTypeDescriptor<X> javaTypeDescriptor) {
      super(javaDescriptor, gridDescriptor);
      this.javaTypeDescriptor = javaTypeDescriptor;
    }

    @Override
    protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
      Instant unwrap = javaTypeDescriptor.unwrap(value, Instant.class, options);
      resultset.put(names[0], unwrap);
    }
  }

}
