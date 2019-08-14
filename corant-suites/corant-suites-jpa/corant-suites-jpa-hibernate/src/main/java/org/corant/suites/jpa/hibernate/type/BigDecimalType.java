package org.corant.suites.jpa.hibernate.type;

import java.math.BigDecimal;
import org.corant.suites.jpa.hibernate.type.descriptor.BigDecimalGridTypeDescriptor;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;

/**
 * corant-suites-jpa-hibernate <br>
 *
 * @auther sushuaihao 2019/8/14
 * @since
 */
public class BigDecimalType extends AbstractGenericBasicType<BigDecimal> {

  public static final BigDecimalType INSTANCE = new BigDecimalType();
  private static final long serialVersionUID = 4457726737615145243L;

  public BigDecimalType() {
    super(BigDecimalGridTypeDescriptor.INSTANCE, BigDecimalTypeDescriptor.INSTANCE);
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
