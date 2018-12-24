/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.servlet.metadata;

import java.util.Arrays;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

/**
 * corant-suites-servlet
 *
 * @author bingo 上午10:59:16
 *
 */
public class HttpConstraintMetaData {

  private EmptyRoleSemantic value = EmptyRoleSemantic.PERMIT;
  private TransportGuarantee transportGuarantee = TransportGuarantee.NONE;
  private String[] rolesAllowed = new String[0];

  /**
   * @param value
   * @param transportGuarantee
   * @param rolesAllowed
   */
  public HttpConstraintMetaData(EmptyRoleSemantic value, TransportGuarantee transportGuarantee,
      String[] rolesAllowed) {
    super();
    setValue(value);
    setTransportGuarantee(transportGuarantee);
    setRolesAllowed(rolesAllowed);
  }

  public HttpConstraintMetaData(HttpConstraint anno) {
    if (anno != null) {
      setValue(anno.value());
      setTransportGuarantee(anno.transportGuarantee());
      setRolesAllowed(Arrays.stream(anno.rolesAllowed()).toArray(String[]::new));
    }
  }

  protected HttpConstraintMetaData() {}

  /**
   *
   * @return the rolesAllowed
   */
  public String[] getRolesAllowed() {
    return rolesAllowed;
  }

  /**
   *
   * @return the transportGuarantee
   */
  public TransportGuarantee getTransportGuarantee() {
    return transportGuarantee;
  }

  /**
   *
   * @return the value
   */
  public EmptyRoleSemantic getValue() {
    return value;
  }

  /**
   *
   * @param rolesAllowed the rolesAllowed to set
   */
  protected void setRolesAllowed(String[] rolesAllowed) {
    this.rolesAllowed = Arrays.stream(rolesAllowed).toArray(String[]::new);
  }

  /**
   *
   * @param transportGuarantee the transportGuarantee to set
   */
  protected void setTransportGuarantee(TransportGuarantee transportGuarantee) {
    this.transportGuarantee =
        transportGuarantee == null ? TransportGuarantee.NONE : transportGuarantee;
  }

  /**
   *
   * @param value the value to set
   */
  protected void setValue(EmptyRoleSemantic value) {
    this.value = value == null ? EmptyRoleSemantic.PERMIT : value;
  }


}
