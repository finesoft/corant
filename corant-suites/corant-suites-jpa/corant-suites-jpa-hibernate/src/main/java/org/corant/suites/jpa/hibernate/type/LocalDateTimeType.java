package org.corant.suites.jpa.hibernate.type;

import java.time.ZonedDateTime;
import org.corant.suites.jpa.hibernate.type.descriptor.ZonedDateTimeGridTypeDescriptor;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午12:52:35
 *
 */
public class LocalDateTimeType extends AbstractGenericBasicType<ZonedDateTime> {

  private static final long serialVersionUID = -3820937201712074842L;

  public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

  public LocalDateTimeType() {
    super(ZonedDateTimeGridTypeDescriptor.INSTANCE, ZonedDateTimeJavaDescriptor.INSTANCE);
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