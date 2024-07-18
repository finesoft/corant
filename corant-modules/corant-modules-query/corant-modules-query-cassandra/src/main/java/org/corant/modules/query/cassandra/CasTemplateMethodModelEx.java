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
package org.corant.modules.query.cassandra;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-cassandra
 *
 * @author bingo 下午7:56:57
 */
public class CasTemplateMethodModelEx extends AbstractTemplateMethodModelEx<Object[]> {

  public static final String SQL_PS_PLACE_HOLDER = "?";
  public static final SimpleScalar SQL_SS_PLACE_HOLDER = new SimpleScalar(SQL_PS_PLACE_HOLDER);
  private final List<Object> parameters = new ArrayList<>();
  private final Query query;

  public CasTemplateMethodModelEx(Query query) {
    this.query = query;
  }

  @Override
  public Object defaultConvertParamValue(Object value) {
    if (value instanceof Iterable<?> it) {
      return convertIterableParamValue(it);
    } else if (value.getClass().isArray()) {
      return convertArrayParamValue((Object[]) value);
    } else {
      return convertSingleValueIfNecessary(value);
    }
  }

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      if (arg instanceof List argList) {
        int argSize = argList.size();
        String[] placeHolders = new String[argSize];
        for (int i = 0; i < argSize; i++) {
          parameters.add(argList.get(i));
          placeHolders[i] = SQL_PS_PLACE_HOLDER;
        }
        return new SimpleScalar(String.join(",", placeHolders));
      } else {
        parameters.add(arg);
        return SQL_SS_PLACE_HOLDER;
      }
    }
    return arguments;
  }

  @Override
  public Object[] getParameters() {
    return parameters.toArray(new Object[0]);
  }

  protected Object convertArrayParamValue(Object[] array) {
    boolean need = false;
    for (Object e : array) {
      if (needConvert(e)) {
        need = true;
        break;
      }
    }
    if (need) {
      Collection<Object> collection = new ArrayList<>();
      for (Object e : array) {
        collection.add(convertSingleValueIfNecessary(e));
      }
      return collection;
    }
    return array;
  }

  protected Object convertIterableParamValue(Iterable<?> it) {
    boolean need = false;
    for (Object e : it) {
      if (needConvert(e)) {
        need = true;
        break;
      }
    }
    if (need) {
      Collection<Object> collection = (it instanceof Set<?>) ? new HashSet<>() : new ArrayList<>();
      for (Object e : it) {
        collection.add(convertSingleValueIfNecessary(e));
      }
      return collection;
    }
    return it;
  }

  protected Object convertSingleValueIfNecessary(Object value) {
    if (value instanceof Instant || value instanceof LocalDateTime
        || value instanceof ZonedDateTime) {
      return toObject(value, Timestamp.class, null);
    } else if (value instanceof LocalDate) {
      return toObject(value, Date.class, null);
    } else if (value instanceof Enum<?>) {
      return value.toString();
    } else {
      return value;
    }
  }

  @Override
  protected Query getQuery() {
    return query;
  }

  protected boolean needConvert(Object value) {
    return value instanceof Instant || value instanceof LocalDateTime
        || value instanceof ZonedDateTime || value instanceof LocalDate || value instanceof Enum<?>;
  }

}
