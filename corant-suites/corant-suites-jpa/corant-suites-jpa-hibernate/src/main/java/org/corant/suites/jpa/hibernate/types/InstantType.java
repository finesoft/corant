package org.corant.suites.jpa.hibernate.types;

import org.corant.suites.jpa.hibernate.types.descriptor.InstantGridTypeDescriptor;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;

import java.time.Instant;

/**
 * corant-root <br>
 *
 * @auther sushuaihao 2019/6/12
 * @since
 */
public class InstantType extends AbstractGenericBasicType<Instant> {
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
