package org.corant.modules.jpa.hibernate.ogm.type;

import java.time.Instant;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;

/**
 * corant-modules-jpa-hibernate-ogm
 *
 * @author sushuaihao 2019/6/12
 * @since
 */
public class InstantType extends AbstractGenericBasicType<Instant> {

  private static final long serialVersionUID = -3820937201712074842L;

  public static final InstantType INSTANCE = new InstantType();

  public InstantType() {
    super(InstantGridTypeDescriptor.INSTANCE, InstantJavaDescriptor.INSTANCE);
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
