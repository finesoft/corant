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

import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.CollectionUtils.subList;
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
import org.corant.suites.query.shared.QueryRuntimeException;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:40:20
 *
 */
public interface DynamicTemplateMethodModelEx<P> extends TemplateMethodModelEx {

  default Object convertParamValue(Object value, Class<?> type, Map<String, ?> hints) {
    if (value == null) {
      return null;
    } else if (type == null) {
      return convertUnknowTypeParamValue(value);
    } else if (value instanceof Iterable || value.getClass().isArray()) {
      return toList(value, type, hints);
    } else {
      return toObject(value, type, hints);
    }
  }

  default Object convertUnknowTypeParamValue(Object value) {
    return value;
  }

  default Object extractParamValue(Object arg) throws TemplateModelException {
    if (arg instanceof WrapperTemplateModel) {
      return extractWrappedParamValue((WrapperTemplateModel) arg);
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

  default Object extractWrappedParamValue(WrapperTemplateModel arg) {
    return arg.getWrappedObject();
  }

  P getParameters();

  @SuppressWarnings({"unchecked", "rawtypes"})
  default Object getParamValue(List args) throws TemplateModelException {
    int argsLen = sizeOf(args);
    if (argsLen > 0) {
      Object extVal = extractParamValue(args.get(0));
      Class<?> extTyp = argsLen > 1 ? toObject(args.get(1), Class.class) : null;
      Map<String, ?> extHints = argsLen > 3 ? mapOf(subList(args, 2).toArray()) : null;
      return convertParamValue(extVal, extTyp, extHints);
    }
    return null;
  }

  String getType();

  default boolean isSimpleType(Class<?> cls) {
    return String.class.equals(cls) || Number.class.isAssignableFrom(cls)
        || Boolean.class.isAssignableFrom(cls) || Temporal.class.isAssignableFrom(cls)
        || Date.class.isAssignableFrom(cls) || Enum.class.isAssignableFrom(cls);
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
      try {
        return it.next();
      } catch (TemplateModelException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }

}
