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
package org.corant.modules.servlet.metadata;

import java.util.Arrays;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import org.corant.shared.util.Strings;

/**
 * corant-modules-servlet
 *
 * @author bingo 上午11:00:53
 *
 */
public class HttpMethodConstraintMetaData {

  private String value;

  private EmptyRoleSemantic emptyRoleSemantic = EmptyRoleSemantic.PERMIT;
  private TransportGuarantee transportGuarantee = TransportGuarantee.NONE;
  private String[] rolesAllowed = Strings.EMPTY_ARRAY;

  public HttpMethodConstraintMetaData(HttpMethodConstraint anno) {
    if (anno != null) {
      setEmptyRoleSemantic(anno.emptyRoleSemantic());
      setTransportGuarantee(anno.transportGuarantee());
      setValue(anno.value());
      setRolesAllowed(Arrays.stream(anno.rolesAllowed()).toArray(String[]::new));
    }
  }

  public HttpMethodConstraintMetaData(String method) {
    setValue(method);
  }

  /**
   * @param value
   * @param emptyRoleSemantic
   * @param transportGuarantee
   * @param rolesAllowed
   */
  public HttpMethodConstraintMetaData(String value, EmptyRoleSemantic emptyRoleSemantic,
      TransportGuarantee transportGuarantee, String[] rolesAllowed) {
    setValue(value);
    setEmptyRoleSemantic(emptyRoleSemantic);
    setTransportGuarantee(transportGuarantee);
    setRolesAllowed(rolesAllowed);
  }

  protected HttpMethodConstraintMetaData() {}

  public static HttpMethodConstraintMetaData[] of(HttpMethodConstraint... constraints) {
    return Arrays.stream(constraints).map(HttpMethodConstraintMetaData::new)
        .toArray(HttpMethodConstraintMetaData[]::new);
  }

  /**
   *
   * @return the emptyRoleSemantic
   */
  public EmptyRoleSemantic getEmptyRoleSemantic() {
    return emptyRoleSemantic;
  }

  /**
   *
   * @return the rolesAllowed
   */
  public String[] getRolesAllowed() {
    return Arrays.copyOf(rolesAllowed, rolesAllowed.length);
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
  public String getValue() {
    return value;
  }

  /**
   *
   * @param emptyRoleSemantic the emptyRoleSemantic to set
   */
  protected void setEmptyRoleSemantic(EmptyRoleSemantic emptyRoleSemantic) {
    this.emptyRoleSemantic =
        emptyRoleSemantic == null ? EmptyRoleSemantic.PERMIT : emptyRoleSemantic;
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
  protected void setValue(String value) {
    this.value = value;
  }

}
