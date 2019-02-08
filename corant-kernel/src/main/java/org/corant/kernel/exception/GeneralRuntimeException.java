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
package org.corant.kernel.exception;

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.defaultString;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * @author bingo 下午6:19:52
 *
 */
public class GeneralRuntimeException extends CorantRuntimeException {

  private static final long serialVersionUID = -3720369148530068164L;

  private Object code;

  private Object subCode;

  private GeneralExceptionSeverity serverity = GeneralExceptionSeverity.ERROR;

  private Object[] parameters = new Object[0];

  private Map<Object, Object> attributes = new HashMap<>();

  public GeneralRuntimeException() {
    super("An unknown error has occurred!");
  }

  public GeneralRuntimeException(Object code) {
    this(code, null, new HashMap<>(), new Object[0]);
  }

  public GeneralRuntimeException(Object code, Object... variants) {
    this(code, null, new HashMap<>(), variants);
  }

  public GeneralRuntimeException(Object code, Object subCode, Map<Object, Object> attributes,
      Object... parameters) {
    super();
    setCode(code);
    setSubCode(subCode);
    setParameters(parameters);
    setAttributes(attributes);
  }

  public GeneralRuntimeException(Throwable cause) {
    super(cause);
    if (cause instanceof GeneralRuntimeException) {
      GeneralRuntimeException causeToUse = (GeneralRuntimeException) cause;
      setCode(causeToUse.getCode());
      setSubCode(causeToUse.getSubCode());
      setParameters(causeToUse.getParameters());
      setAttributes(causeToUse.getAttributes());
    }
  }

  public GeneralRuntimeException(Throwable cause, Object code) {
    super(cause);
    setCode(code);
  }

  public GeneralRuntimeException(Throwable cause, Object code, Object... parameters) {
    super(cause);
    setCode(code);
    setParameters(parameters);
  }

  public GeneralRuntimeException attribute(Object name, Object value) {
    attributes.put(name, value);
    return this;
  }

  public GeneralRuntimeException attributes(
      Function<Map<Object, Object>, Map<Object, Object>> func) {
    if (func != null) {
      setAttributes(func.apply(new HashMap<>(attributes)));
    }
    return this;
  }

  /**
   * For handle intention
   */
  public Map<Object, Object> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  /**
   * Imply the exception type, for exception type intention
   */
  public Object getCode() {
    return code;
  }

  @Override
  public String getLocalizedMessage() {
    Instance<GeneralRuntimeExceptionMessager> msger =
        CDI.current().select(GeneralRuntimeExceptionMessager.class);
    if (msger.isResolvable()) {
      return this.getLocalizedMessage(Locale.getDefault(), msger.get());
    } else {
      return defaultString(super.getMessage()) + " " + asDefaultString(getCode());
    }
  }

  public String getLocalizedMessage(Locale locale, GeneralRuntimeExceptionMessager messager) {
    return messager.getMessage(locale, this);
  }

  @Override
  public String getMessage() {
    return this.getLocalizedMessage();
  }

  public Object[] getParameters() {
    return Arrays.copyOf(parameters, parameters.length);
  }

  public GeneralExceptionSeverity getServerity() {
    return serverity;
  }

  /**
   * Imply the exception type polymorph, for exception type polymorph
   */
  public Object getSubCode() {
    return subCode;
  }

  /**
   * Supply the original parameters list for handler and then return the new variants
   */
  public GeneralRuntimeException parameters(Function<List<Object>, List<Object>> func) {
    if (func != null) {
      List<Object> updated = func.apply(asList(parameters));
      setParameters(updated == null ? new Object[0] : updated.toArray());
    }
    return this;
  }

  public GeneralRuntimeException serverity(GeneralExceptionSeverity serverity) {
    setServerity(serverity);
    return this;
  }

  public GeneralRuntimeException subCode(Object subCode) {
    setSubCode(subCode);
    return this;
  }

  /**
   * Return the exception wrapper for transform the exception sub code or variants or attributes.
   * Example:
   *
   * <pre>
   * try {
   *    //...domain logic break by new GeneralRuntimeException("original code","original var")
   * } catch (GeneralRuntimeException ex) {
   *   throw ex.wrapper().ifCodeIs("original code").thenSubcode("new sub
   *   code").thenVariants((o)->Arrays.asList("new var")).wrap();
   * }
   * </pre>
   *
   * @see GeneralRuntimeExceptionWrapper
   */
  public GeneralRuntimeExceptionWrapper wrapper() {
    return new GeneralRuntimeExceptionWrapper(this);
  }

  protected void setAttributes(Map<Object, Object> attributes) {
    this.attributes.clear();
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
  }

  protected void setCode(Object code) {
    this.code = code;
  }

  protected void setParameters(Object[] parameters) {
    this.parameters =
        parameters == null ? new Object[0] : Arrays.copyOf(parameters, parameters.length);
  }

  protected void setServerity(GeneralExceptionSeverity serverity) {
    this.serverity = serverity == null ? GeneralExceptionSeverity.ERROR : serverity;
  }

  protected void setSubCode(Object subCode) {
    this.subCode = subCode;
  }

}
