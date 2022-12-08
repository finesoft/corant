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
package org.corant.shared.exception;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.areEqual;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 上午10:24:46
 *
 */
public class GeneralRuntimeExceptionWrapper {

  protected final GeneralRuntimeException exception;
  protected final List<ConjoinedMessageKeyBuilder> keyBuilders = new LinkedList<>();
  protected final List<ConjoinedCodeBuilder> codeBuilders = new LinkedList<>();

  GeneralRuntimeExceptionWrapper(GeneralRuntimeException exception) {
    this.exception = exception;
  }

  public ConjoinedCodeBuilder ifCodeIs(Object code) {
    return new ConjoinedCodeBuilder(this, code);
  }

  public ConjoinedMessageKeyBuilder ifMessageKeyIs(Object messageKey) {
    return new ConjoinedMessageKeyBuilder(this, messageKey);
  }

  public Builder whatever() {
    return new Builder(this) {
      @Override
      public Builder thenCode(Object code) {
        return super.thenCode(code);
      }

      @Override
      public Builder thenMessageKey(Object messageKey) {
        return super.thenMessageKey(messageKey);
      }
    };
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:58:13
   *
   */
  public static class Builder {
    protected final GeneralRuntimeExceptionWrapper wrapper;
    protected Object messageKey;
    protected Object[] messageParameters = Objects.EMPTY_ARRAY;
    protected Object code;
    protected Map<Object, Object> attributes = new HashMap<>();

    Builder(GeneralRuntimeExceptionWrapper wrapper) {
      this.wrapper = wrapper;
    }

    Builder(GeneralRuntimeExceptionWrapper wrapper, Object messageKey) {
      this.messageKey = messageKey;
      this.wrapper = wrapper;
      int varLen = wrapper.exception.getMessageParameters().length;
      if (varLen > 0) {
        messageParameters = new Object[varLen];
        System.arraycopy(wrapper.exception.getMessageParameters(), 0, messageParameters, 0, varLen);
      }
      attributes.putAll(wrapper.exception.getAttributes());
      code = wrapper.exception.getCode();
    }

    Builder(Object code, GeneralRuntimeExceptionWrapper wrapper) {
      this.code = code;
      this.wrapper = wrapper;
      int varLen = wrapper.exception.getMessageParameters().length;
      if (varLen > 0) {
        messageParameters = new Object[varLen];
        System.arraycopy(wrapper.exception.getMessageParameters(), 0, messageParameters, 0, varLen);
      }
      attributes.putAll(wrapper.exception.getAttributes());
      messageKey = wrapper.exception.getMessageKey();
    }

    public Builder thenAttributes(Map<Object, Object> attributes) {
      return this.thenAttributes(t -> attributes);
    }

    public Builder thenAttributes(UnaryOperator<Map<Object, Object>> func) {
      if (func != null) {
        setAttributes(func.apply(new HashMap<>(attributes)));
      }
      return this;
    }

    public Builder thenMessageParameters(List<Object> parameters) {
      return this.thenMessageParameters(t -> parameters);
    }

    public Builder thenMessageParameters(UnaryOperator<List<Object>> func) {
      if (func != null) {
        List<Object> updated = func.apply(listOf(messageParameters));
        setMessageParameters(updated == null ? null : updated.toArray());
      }
      return this;
    }

    public GeneralRuntimeException wrap() {
      GeneralRuntimeException ex = wrapper.exception.attributes(t -> getAttributes())
          .messageParameters(t -> listOf(getMessageParameters())).code(getCode());
      attributes.clear();
      return ex;
    }

    Map<Object, Object> getAttributes() {
      return attributes;
    }

    Object getCode() {
      return code;
    }

    Object getMessageKey() {
      return messageKey;
    }

    Object[] getMessageParameters() {
      return messageParameters;
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

    void setMessageParameters(Object[] messageParameters) {
      this.messageParameters = messageParameters == null ? Objects.EMPTY_ARRAY : messageParameters;
    }

    Builder thenCode(Object code) {
      this.code = code;
      return this;
    }

    Builder thenMessageKey(Object messageKey) {
      this.messageKey = messageKey;
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:58:09
   */
  public static class ConjoinedCodeBuilder extends Builder {

    ConjoinedCodeBuilder(GeneralRuntimeExceptionWrapper wrapper, Object code) {
      super(code, wrapper);
      wrapper.codeBuilders.removeIf(p -> areEqual(p.getCode(), code));
      wrapper.codeBuilders.add(this);
    }

    public ConjoinedCodeBuilder elseIfCodeIs(Object code) {
      return new ConjoinedCodeBuilder(getWrapper(), code);
    }

    @Override
    public ConjoinedCodeBuilder thenAttributes(Map<Object, Object> attributes) {
      super.thenAttributes(attributes);
      return this;
    }

    @Override
    public ConjoinedCodeBuilder thenAttributes(UnaryOperator<Map<Object, Object>> func) {
      super.thenAttributes(func);
      return this;
    }

    @Override
    public ConjoinedCodeBuilder thenMessageKey(Object messageKey) {
      super.thenMessageKey(messageKey);
      return this;
    }

    @Override
    public ConjoinedCodeBuilder thenMessageParameters(List<Object> parameters) {
      super.thenMessageParameters(parameters);
      return this;
    }

    @Override
    public ConjoinedCodeBuilder thenMessageParameters(UnaryOperator<List<Object>> func) {
      super.thenMessageParameters(func);
      return this;
    }

    @Override
    public GeneralRuntimeException wrap() {
      for (Builder builder : getWrapper().codeBuilders) {
        if (areEqual(builder.code, getWrapper().exception.getCode())) {
          getWrapper().exception.attributes(t -> builder.attributes)
              .messageParameters(t -> listOf(builder.messageParameters))
              .messageKey(builder.messageKey);
          break;
        } else {
          builder.attributes.clear();
        }
      }
      getWrapper().codeBuilders.clear();
      return getWrapper().exception;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:58:09
   */
  public static class ConjoinedMessageKeyBuilder extends Builder {

    ConjoinedMessageKeyBuilder(GeneralRuntimeExceptionWrapper wrapper, Object messageKey) {
      super(wrapper, messageKey);
      wrapper.keyBuilders.removeIf(p -> areEqual(p.getMessageKey(), messageKey));
      wrapper.keyBuilders.add(this);
    }

    public ConjoinedMessageKeyBuilder elseIfMessageKeyIs(Object messageKey) {
      return new ConjoinedMessageKeyBuilder(getWrapper(), messageKey);
    }

    @Override
    public ConjoinedMessageKeyBuilder thenAttributes(Map<Object, Object> attributes) {
      super.thenAttributes(attributes);
      return this;
    }

    @Override
    public ConjoinedMessageKeyBuilder thenAttributes(UnaryOperator<Map<Object, Object>> func) {
      super.thenAttributes(func);
      return this;
    }

    @Override
    public ConjoinedMessageKeyBuilder thenCode(Object code) {
      super.thenCode(code);
      return this;
    }

    @Override
    public ConjoinedMessageKeyBuilder thenMessageParameters(List<Object> parameters) {
      super.thenMessageParameters(parameters);
      return this;
    }

    @Override
    public ConjoinedMessageKeyBuilder thenMessageParameters(UnaryOperator<List<Object>> func) {
      super.thenMessageParameters(func);
      return this;
    }

    @Override
    public GeneralRuntimeException wrap() {
      for (Builder builder : getWrapper().keyBuilders) {
        if (areEqual(builder.messageKey, getWrapper().exception.getMessageKey())) {
          getWrapper().exception.attributes(t -> builder.attributes)
              .messageParameters(t -> listOf(builder.messageParameters)).code(builder.code);
          break;
        } else {
          builder.attributes.clear();
        }
      }
      getWrapper().keyBuilders.clear();
      return getWrapper().exception;
    }

  }
}
