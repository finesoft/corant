package org.corant.modules.jpa.hibernate.ogm.type;

import java.time.ZonedDateTime;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 * corant-modules-jpa-hibernate-ogm
 *
 * @author bingo 下午12:52:35
 */
public class ZonedDateTimeType extends AbstractGenericBasicType<ZonedDateTime> {

  private static final long serialVersionUID = -3820937201712074842L;

  public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

  public ZonedDateTimeType() {
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
