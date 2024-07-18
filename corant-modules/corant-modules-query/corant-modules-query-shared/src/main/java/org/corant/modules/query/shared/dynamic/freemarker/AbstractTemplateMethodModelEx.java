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
package org.corant.modules.query.shared.dynamic.freemarker;

import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Primitives.isSimpleClass;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Strings.NULL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.exception.CorantRuntimeException;
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
 * corant-modules-query-shared
 *
 * @author bingo 上午10:35:11
 */
public abstract class AbstractTemplateMethodModelEx<P> implements DynamicTemplateMethodModelEx<P> {

  /**
   * Convert parameter values using parameter types and hints. If the expected parameter type passed
   * in is null then use {@link #defaultConvertParamValue(Object)} to convert.
   *
   * @param value the parameter value to be converted
   * @param expectedType the expected type
   * @param convertHints the conversion hints
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
   * @param value the parameter value
   * @return defaultConvertParamValue
   */
  protected Object defaultConvertParamValue(Object value) {
    return value;
  }

  /**
   * Extract single template method parameter value.
   *
   * @param arg the parameter value to be extracted
   * @return a normal java type parameter value
   * @throws TemplateModelException extractTplModValue
   */
  protected Object extractParamValue(Object arg) throws TemplateModelException {
    if (arg instanceof WrapperTemplateModel) {
      return ((WrapperTemplateModel) arg).getWrappedObject();
    } else if (arg instanceof TemplateScalarModel) {
      if (getQueryProperty(".disable-trim-string-parameter", Boolean.class, false)) {
        return ((TemplateScalarModel) arg).getAsString();
      }
      return ((TemplateScalarModel) arg).getAsString().trim();
    } else if (arg instanceof TemplateDateModel) {
      return ((TemplateDateModel) arg).getAsDate();
    } else if (arg instanceof TemplateNumberModel) {
      return ((TemplateNumberModel) arg).getAsNumber();
    } else if (arg instanceof TemplateBooleanModel) {
      return ((TemplateBooleanModel) arg).getAsBoolean();
    } else if (arg instanceof TemplateSequenceModel tsm) {
      int size = tsm.size();
      List<Object> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(extractParamValue(tsm.get(i)));
      }
      return list;
    } else if (arg instanceof TemplateCollectionModel) {
      final TemplateModelIterator it = ((TemplateCollectionModel) arg).iterator();
      List<Object> list = new ArrayList<>();
      if (it != null) {
        while (it.hasNext()) {
          list.add(extractParamValue(it.next()));
        }
      }
      return list;
    } else {
      // FIXME Resolve TemplateHashModel
      throw new QueryRuntimeException("Unknown argument,the class is %s.",
          arg == null ? NULL : arg.getClass());
    }
  }

  /**
   * Get the extracted and converted parameter from template method.
   *
   * <p>
   * The args passed in is a List, the first element (args.get(0)) is the actual parameter value;
   * <p>
   * If args has more than one elements then the second element (args.get(1)) must be a string that
   * represent the expected type (java class name);
   * <p>
   * If args has more than three elements then starting with the third element, each of the
   * following two elements represents a key-value pair that use for converter as hints.
   *
   * <pre>
   * example:
   *    TM(val, "java.time.LocalDateTime", "converter.zone-id", "UTC")
   *    value = val
   *    type = "java.time.LocalDateTime"
   *    hints = {"converter.zone-id","UTC"}
   * </pre>
   *
   * @param args the template model parameters
   * @see ConverterHints
   * @return a parameter value
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
        extHints = mapOf(args.subList(2, argsLen).stream().map(a -> {
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

  protected abstract Query getQuery();

  protected <T> T getQueryProperty(String name, Class<T> type, T nvl) {
    if (getQuery() != null) {
      return getQuery().getProperty(name, type, nvl);
    }
    return nvl;
  }

  /**
   * Return whether the element of the passed iterable is simple type.
   *
   * @param it the iterable to be checked
   */
  protected boolean isSimpleElement(Iterable<?> it) {
    if (it != null) {
      for (Object o : it) {
        if (o != null && !isSimpleType(o.getClass())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Return whether the element of the passed array is simple type.
   *
   * @param array the array to be checked
   */
  protected boolean isSimpleElement(Object[] array) {
    if (array != null) {
      for (Object o : array) {
        if (o != null && !isSimpleType(o.getClass())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Return whether the passed object is simple type or simple type collection or simple type array.
   *
   * @param obj the object to be checked
   */
  protected boolean isSimpleObject(Object obj) {
    if (obj instanceof Iterable<?> it) {
      return isSimpleElement(it);
    } else if (obj != null && obj.getClass().isArray()) {
      return isSimpleElement(wrapArray(obj));
    } else {
      return obj == null || isSimpleType(obj.getClass());
    }
  }

  /**
   * check if is simple cls
   *
   * @param cls the parameter value class to be checked
   * @return isSimpleType
   */
  protected boolean isSimpleType(Class<?> cls) {
    return isSimpleClass(cls);
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 上午9:26:56
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
