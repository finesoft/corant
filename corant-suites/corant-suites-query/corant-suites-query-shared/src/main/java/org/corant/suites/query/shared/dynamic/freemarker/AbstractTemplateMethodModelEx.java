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
package org.corant.suites.query.shared.dynamic.freemarker;

import static org.corant.shared.util.ClassUtils.isPrimitiveOrWrapper;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.CollectionUtils.sublist;
import static org.corant.shared.util.ConversionUtils.toList;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.MapUtils.mapOf;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.QueryRuntimeException;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * corant-suites-query-shared
 *
 * @author bingo 上午10:35:11
 *
 */
public abstract class AbstractTemplateMethodModelEx<P> implements DynamicTemplateMethodModelEx<P> {

  /**
   * Convert parameter values using parameter types and hints. If the expected parameter type passed
   * in is null then use {@link #defaultConvertParamValue(Object)} to convert.
   *
   * @param value
   * @param expectedType
   * @param convertHints
   * @return convertParamValue
   *
   * @see ConverterHints
   * @see Converter
   */
  protected Object convertParamValue(Object value, Class<?> expectedType,
      Map<String, ?> convertHints) {
    if (value == null) {
      return null;
    } else if (expectedType == null) {
      return defaultConvertParamValue(value);
    } else if (value instanceof Iterable || value.getClass().isArray()) {
      return toList(value, expectedType, convertHints);
    } else {
      return toObject(value, expectedType, convertHints);
    }
  }

  /**
   * Converts parameters that do not specify the expected type.
   *
   * @param value
   * @return defaultConvertParamValue
   */
  protected Object defaultConvertParamValue(Object value) {
    return value;
  }

  /**
   * Extract single template method parameter value.
   *
   * @param arg
   * @return
   * @throws TemplateModelException extractTplModValue
   */
  protected Object extractParamValue(Object arg) throws TemplateModelException {
    if (arg instanceof WrapperTemplateModel) {
      return ((WrapperTemplateModel) arg).getWrappedObject();
    } else if (arg instanceof TemplateScalarModel) {
      return ((TemplateScalarModel) arg).getAsString().trim();
    } else if (arg instanceof TemplateDateModel) {
      return ((TemplateDateModel) arg).getAsDate();
    } else if (arg instanceof TemplateNumberModel) {
      return ((TemplateNumberModel) arg).getAsNumber();
    } else if (arg instanceof TemplateBooleanModel) {
      return ((TemplateBooleanModel) arg).getAsBoolean();
    } else if (arg instanceof TemplateSequenceModel) {
      TemplateSequenceModel tsm = (TemplateSequenceModel) arg;
      int size = tsm.size();
      List<Object> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(extractParamValue(tsm.get(i)));
      }
      return list;
    } else if (arg instanceof TemplateCollectionModel) {
      final TemplateModelIterator it = ((TemplateCollectionModel) arg).iterator();
      List<TemplateModel> tsm = listOf(new SimpleTemplateModelIterator(it));
      int size = tsm.size();
      List<Object> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(extractParamValue(tsm.get(i)));
      }
      return list;
    } else {
      // FIXME Resolve TemplateHashModel
      throw new QueryRuntimeException("Unknow arguement,the class is %s",
          arg == null ? "null" : arg.getClass());
    }
  }

  /**
   * Get the extracted and converted parameter from template method.
   *
   * <pre>
   * The args passed in is a List, the first element (args.get(0)) is the actural parameter value;
   *
   * If args has more than one elements then the second element (args.get(1)) must be a string that
   * represent the expected type (java class name);
   *
   * If args has more than three elements then starting with the third element, each of the
   * following two elements represents a key-value pair that use for converter as hints.
   *
   * example:
   *    TM(val, "java.time.LocalDateTime", "converter.zone-id", "UTC")
   *    value = val
   *    type = "java.time.LocalDateTime"
   *    hints = {"converter.zone-id","UTC"}
   * </pre>
   *
   * @param args
   * @return
   * @throws TemplateModelException getParamValue
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected Object getParamValue(List args) throws TemplateModelException {
    int argsLen = sizeOf(args);
    if (argsLen > 0) {
      Object extVal = extractParamValue(args.get(0));
      Class<?> extTyp = argsLen > 1 ? toObject(extractParamValue(args.get(1)), Class.class) : null;
      Map<String, ?> extHints = null;
      if (extTyp != null && argsLen > 3) {
        extHints = mapOf(sublist(args, 2).stream().map(a -> {
          try {
            return extractParamValue(a);
          } catch (TemplateModelException e) {
            throw new CorantRuntimeException(e);
          }
        }).toArray(Object[]::new));
      }
      return convertParamValue(extVal, extTyp, extHints);
    }
    return null;
  }

  /**
   * check if is simple cls
   *
   * @param cls
   * @return isSimpleType
   */
  protected boolean isSimpleType(Class<?> cls) {
    return isPrimitiveOrWrapper(cls) || String.class.equals(cls)
        || Number.class.isAssignableFrom(cls) || Boolean.class.isAssignableFrom(cls)
        || Temporal.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls)
        || Enum.class.isAssignableFrom(cls);
  }

  /**
   * corant-suites-query-shared
   *
   * @author bingo 上午9:26:56
   *
   */
  public static final class SimpleTemplateModelIterator implements Iterator<TemplateModel> {

    private final TemplateModelIterator it;

    public SimpleTemplateModelIterator(TemplateModelIterator it) {
      this.it = it;
    }

    @Override
    public boolean hasNext() {
      try {
        return it.hasNext();
      } catch (TemplateModelException e) {
        throw new QueryRuntimeException(e);
      }
    }

    @Override
    public TemplateModel next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      try {
        return it.next();
      } catch (TemplateModelException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }
}
