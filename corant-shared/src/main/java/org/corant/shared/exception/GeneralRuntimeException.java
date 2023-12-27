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
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.SPACE;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.defaultString;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * <p>
 * Exception class that can carry more business-significant information. In general, this class
 * doesn't specify the concrete detailed message, it is used with the
 * {@link ExceptionMessageResolver} to resolve the exception message. The
 * {@link ExceptionMessageResolver} may use {@link #getMessageKey()},
 * {@link #getMessageParameters()} to resolve the message and may specify code {@link #getCode()}
 * and some attributes {@link #getAttributes()} to the catcher to do some action.
 *
 * @see ExceptionMessageResolver
 * @see GeneralRuntimeExceptionWrapper
 *
 * @author bingo 下午6:19:52
 *
 */
public class GeneralRuntimeException extends CorantRuntimeException {

  protected static final String UNKNOWN_EX_MSG = "An unknown exception has occurred!";

  private static final long serialVersionUID = -3720369148530068164L;

  private GeneralExceptionSeverity severity = GeneralExceptionSeverity.ERROR;

  private Object messageKey;

  private Object[] messageParameters = Objects.EMPTY_ARRAY;

  private Object code;

  private Map<Object, Object> attributes = new HashMap<>();

  public GeneralRuntimeException() {
    this(UNKNOWN_EX_MSG);
  }

  /**
   * Constructs a general runtime exception with the given message key
   *
   * @param messageKey the exception message key use for message resolving or checking
   */
  public GeneralRuntimeException(Object messageKey) {
    this(messageKey, null, null, null);
  }

  /**
   * Constructs a general runtime exception with the given message key and message parameters
   *
   * @param messageKey the exception message key use for message resolving or checking
   * @param messageParameters the message parameters
   */
  public GeneralRuntimeException(Object messageKey, Object... messageParameters) {
    this(messageKey, messageParameters, null, null);
  }

  /**
   * Use cause to construct a new runtime exception.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public GeneralRuntimeException(Throwable cause) {
    super(cause);
    if (cause instanceof GeneralRuntimeException causeToUse) {
      messageKey = causeToUse.getMessageKey();
      if (causeToUse.getMessageParameters() != null) {
        messageParameters = Arrays.copyOf(causeToUse.getMessageParameters(),
            causeToUse.getMessageParameters().length);
      }
      code = causeToUse.getCode();
      attributes.putAll(causeToUse.getAttributes());
    }
  }

  /**
   * Use cause and message key to construct a new runtime exception.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param messageKey the exception message key use for message resolving or checking
   */
  public GeneralRuntimeException(Throwable cause, Object messageKey) {
    super(cause);
    this.messageKey = messageKey;
  }

  /**
   * Use cause and message key and message parameters to construct a new runtime exception.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param messageKey the exception message key use for message resolving or checking
   * @param messageParameters the message parameters
   */
  public GeneralRuntimeException(Throwable cause, Object messageKey, Object... messageParameters) {
    super(cause);
    this.messageKey = messageKey;
    if (messageParameters != null) {
      this.messageParameters = Arrays.copyOf(messageParameters, messageParameters.length);
    }
  }

  /**
   * Constructs a general runtime exception with the given message key and message parameters and
   * code and additional attributes.
   *
   * @param messageKey the exception message key use for message resolving or checking
   * @param messageParameters the message parameters
   * @param code the exception code for the catcher to do more action
   * @param attributes the additional business-significant attributes for the catcher to do more
   *        action
   */
  protected GeneralRuntimeException(Object messageKey, Object[] messageParameters, Object code,
      Map<Object, Object> attributes) {
    this.messageKey = messageKey;
    if (messageParameters != null) {
      this.messageParameters = Arrays.copyOf(messageParameters, messageParameters.length);
    }
    this.code = code;
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
  }

  /**
   * Use the specific message to construct the instance.
   *
   * @param message the specific original message.
   *
   * @see #getOriginalMessage()
   */
  protected GeneralRuntimeException(String message) {
    super(message);
  }

  /**
   * Use the specific message and cause to construct the instance.
   *
   * @param message the specific original message.
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A {@code null} value is permitted, and indicates that the cause is nonexistent or
   *        unknown.)
   *
   * @see #getOriginalMessage()
   */
  protected GeneralRuntimeException(String message, Throwable cause) {
    super(cause, message);
  }

  /**
   * Returns an instance construct with a specific message.
   *
   * @param message the specific original message.
   *
   * @see #getOriginalMessage()
   */
  public static GeneralRuntimeException of(String message) {
    return new GeneralRuntimeException(message);
  }

  /**
   * Returns an instance construct with A specific message and a cause.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A {@code null} value is permitted, and indicates that the cause is nonexistent or
   *        unknown.)
   * @param message the specific original message.
   * @see #getOriginalMessage()
   */
  public static GeneralRuntimeException of(Throwable cause, String message) {
    return new GeneralRuntimeException(message, cause);
  }

  public GeneralRuntimeException attribute(Object name, Object value) {
    attributes.put(name, value);
    return this;
  }

  public GeneralRuntimeException attributes(Map<Object, Object> attributes) {
    this.attributes.clear();
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
    return this;
  }

  public GeneralRuntimeException attributes(UnaryOperator<Map<Object, Object>> func) {
    if (func != null) {
      Map<Object, Object> newAttr = func.apply(new HashMap<>(attributes));
      attributes.clear();
      attributes.putAll(newAttr);
    } else {
      attributes.clear();
    }
    return this;
  }

  public GeneralRuntimeException code(Object code) {
    this.code = code;
    return this;
  }

  /**
   * Context data for exceptions, it is usually handled by subsequent business logic that catches
   * the exception.
   */
  public Map<Object, Object> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  /**
   * Implied exception type, for the intent of the exception type, it is usually handled by
   * subsequent business logic that catches the exception.
   */
  public Object getCode() {
    return code;
  }

  @Override
  public String getLocalizedMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }

  public String getLocalizedMessage(Locale locale) {
    try {
      return ExceptionMessageResolver.INSTANCE.getMessage(this,
          defaultObject(locale, Locale::getDefault));
    } catch (Exception e) {
      addSuppressed(e);
    }
    return defaultString(super.getMessage()) + SPACE + asDefaultString(getMessageKey());
  }

  /**
   * {@inheritDoc}
   * <p>
   * Note: The message returned here has generally been processed by the
   * {@link ExceptionMessageResolver#getMessage(Exception, Locale)}.
   *
   * @see ExceptionMessageResolver#getMessage(Exception, Locale)
   */
  @Override
  public String getMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }

  /**
   * Returns the exception message key, which can be used with the specified Locale for i18 message
   * retrieval.
   */
  public Object getMessageKey() {
    return messageKey;
  }

  /**
   * Returns the parameter of the exception message, which can be used together with the
   * {@link #getMessageKey()} to construct the message.
   */
  public Object[] getMessageParameters() {
    return Arrays.copyOf(messageParameters, messageParameters.length);
  }

  /**
   * Returns the original message. The original message refers to the message that is not
   * constructed using {@link #getMessageKey()} and {@link #getMessageParameters()} and is generally
   * directly constructed by the constructor.
   *
   * @see #of(Throwable, String)
   * @see #GeneralRuntimeException(String)
   */
  public String getOriginalMessage() {
    return super.getMessage();
  }

  public GeneralExceptionSeverity getSeverity() {
    return severity;
  }

  public GeneralRuntimeException messageKey(Object messageKey) {
    this.messageKey = messageKey;
    return this;
  }

  public GeneralRuntimeException messageParameters(Object[] messageParameters) {
    if (messageParameters != null) {
      this.messageParameters = Arrays.copyOf(messageParameters, messageParameters.length);
    } else {
      this.messageParameters = Objects.EMPTY_ARRAY;
    }
    return this;
  }

  /**
   * Supply the original parameters list for handler and then return the new variants
   */
  public GeneralRuntimeException messageParameters(UnaryOperator<List<Object>> func) {
    if (func != null) {
      List<Object> updated = func.apply(listOf(messageParameters));
      messageParameters = updated == null ? Objects.EMPTY_ARRAY : updated.toArray();
    } else {
      messageParameters = Objects.EMPTY_ARRAY;
    }
    return this;
  }

  public GeneralRuntimeException severity(GeneralExceptionSeverity severity) {
    this.severity = defaultObject(severity, GeneralExceptionSeverity.ERROR);
    return this;
  }

  /**
   * Return the exception wrapper for transform the exception sub code or variants or attributes.
   * Example:
   *
   * <pre>
   * try {
   *    //...domain logic break by new GeneralRuntimeException("original message key","original var")
   * } catch (GeneralRuntimeException ex) {
   *   throw ex.wrapper().ifMessageKeyIs("original message key").thenCode("new sub
   *   code").thenVariants((o)->Arrays.asList("new var")).wrap();
   * }
   *
   * try {
   *    //...domain logic break by new GeneralRuntimeException("original code","original message key","original var")
   * } catch (GeneralRuntimeException ex) {
   *   throw ex.wrapper().ifCodeIs("original code").thenMessageKey("new message
   *   key").thenVariants((o)->Arrays.asList("new var")).wrap();
   * }
   * </pre>
   *
   * @see GeneralRuntimeExceptionWrapper
   */
  public GeneralRuntimeExceptionWrapper wrapper() {
    return new GeneralRuntimeExceptionWrapper(this);
  }

}
