package org.corant.suites.jpa.hibernate.type;

import java.time.LocalDateTime;
import org.corant.suites.jpa.hibernate.type.descriptor.LocalDateTimeGridTypeDescriptor;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午12:52:35
 *
 */
public class ZonedDateTimeType extends AbstractGenericBasicType<LocalDateTime> {

  private static final long serialVersionUID = -3820937201712074842L;

  public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

  public ZonedDateTimeType() {
    super(LocalDateTimeGridTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE);
  }

  @Override
  public int getColumnSpan(Mapping mapping) throws MappingException {
    return 1;
  }

  @Override
  public String getName() {
    return null;
  }
}
