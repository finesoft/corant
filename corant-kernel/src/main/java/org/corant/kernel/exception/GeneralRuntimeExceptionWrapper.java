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
import static org.corant.shared.util.ObjectUtils.isEquals;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author bingo 上午10:24:46
 *
 */
public class GeneralRuntimeExceptionWrapper {

  private final GeneralRuntimeException exception;
  private final List<ConjoinedBuilder> builders = new LinkedList<>();

  GeneralRuntimeExceptionWrapper(GeneralRuntimeException exception) {
    this.exception = exception;
  }

  public ConjoinedBuilder ifCodeIs(Object code) {
    return new ConjoinedBuilder(this, code);
  }

  public Builder whatever() {
    return new Builder(this, null);
  }

  public static class Builder {
    private Object[] parameters = new Object[0];
    private Map<Object, Object> attributes = new HashMap<>();
    private Object subCode;
    private final Object code;
    private final GeneralRuntimeExceptionWrapper wrapper;

    Builder(GeneralRuntimeExceptionWrapper wrapper, Object code) {
      super();
      this.code = code;
      this.wrapper = wrapper;
      int varLen = wrapper.exception.getParameters().length;
      if (varLen > 0) {
        parameters = new Object[varLen];
        System.arraycopy(wrapper.exception.getParameters(), 0, parameters, 0, varLen);
      }
      attributes.putAll(wrapper.exception.getAttributes());
      subCode = wrapper.exception.getSubCode();
    }

    public Builder thenAttributes(Function<Map<Object, Object>, Map<Object, Object>> func) {
      if (func != null) {
        setAttributes(func.apply(new HashMap<>(attributes)));
      }
      return this;
    }

    public Builder thenAttributes(Map<Object, Object> attributes) {
      return this.thenAttributes(t -> attributes);
    }

    public Builder thenParameters(Function<List<Object>, List<Object>> func) {
      if (func != null) {
        List<Object> updated = func.apply(asList(parameters));
        setParameters(updated == null ? null : updated.toArray());
      }
      return this;
    }

    public Builder thenParameters(List<Object> parameters) {
      return this.thenParameters(t -> parameters);
    }

    public Builder thenSubCode(Object subCode) {
      this.subCode = subCode;
      return this;
    }

    public GeneralRuntimeException wrap() {
      GeneralRuntimeException ex = wrapper.exception.attributes(t -> getAttributes())
          .parameters(t -> asList(getParameters())).subCode(getSubCode());
      attributes.clear();
      return ex;
    }

    Map<Object, Object> getAttributes() {
      return attributes;
    }

    Object getCode() {
      return code;
    }

    Object[] getParameters() {
      return parameters;
    }

    Object getSubCode() {
      return subCode;
    }

    GeneralRuntimeExceptionWrapper getWrapper() {
      return wrapper;
    }

    void setAttributes(Map<Object, Object> attributes) {
      this.attributes.clear();
      if (attributes != null) {
        this.attributes.putAll(attributes);
      }
    }

    void setParameters(Object[] parameters) {
      this.parameters = parameters == null ? new Object[0] : parameters;
    }

  }

  public static class ConjoinedBuilder extends Builder {
    ConjoinedBuilder(GeneralRuntimeExceptionWrapper wrapper, Object code) {
      super(wrapper, code);
      wrapper.builders.removeIf(p -> isEquals(p.getCode(), code));
      wrapper.builders.add(this);
    }

    public ConjoinedBuilder elseIfCodeIs(Object code) {
      return new ConjoinedBuilder(getWrapper(), code);
    }

    @Override
    public ConjoinedBuilder thenAttributes(
        Function<Map<Object, Object>, Map<Object, Object>> func) {
      super.thenAttributes(func);
      return this;
    }

    @Override
    public ConjoinedBuilder thenAttributes(Map<Object, Object> attributes) {
      super.thenAttributes(attributes);
      return this;
    }

    @Override
    public ConjoinedBuilder thenParameters(Function<List<Object>, List<Object>> func) {
      super.thenParameters(func);
      return this;
    }

    @Override
    public ConjoinedBuilder thenParameters(List<Object> parameters) {
      super.thenParameters(parameters);
      return this;
    }

    @Override
    public ConjoinedBuilder thenSubCode(Object subCode) {
      super.thenSubCode(subCode);
      return this;
    }

    @Override
    public GeneralRuntimeException wrap() {
      for (Builder builder : getWrapper().builders) {
        if (isEquals(builder.code, getWrapper().exception.getCode())) {
          getWrapper().exception.attributes(t -> builder.attributes)
              .parameters(t -> asList(builder.parameters)).subCode(builder.subCode);
          break;
        } else {
          builder.attributes.clear();
        }
      }
      getWrapper().builders.clear();
      return getWrapper().exception;
    }

  }
}
