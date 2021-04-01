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

import static org.corant.shared.util.Classes.getComponentClass;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-cassandra
 *
 * @author bingo 下午7:56:57
 *
 */
public class CasTemplateMethodModelEx extends AbstractTemplateMethodModelEx<Object[]> {

  public static final String SQL_PS_PLACE_HOLDER = "?";
  public static final SimpleScalar SQL_SS_PLACE_HOLDER = new SimpleScalar(SQL_PS_PLACE_HOLDER);
  private final List<Object> parameters = new ArrayList<>();

  @Override
  public Object defaultConvertParamValue(Object value) {
    Class<?> type = getComponentClass(value);
    if (Instant.class.isAssignableFrom(type) || LocalDateTime.class.isAssignableFrom(type)) {
      return convertParamValue(value, Timestamp.class, null);
    } else if (LocalDate.class.isAssignableFrom(type)) {
      return convertParamValue(value, Date.class, null);
    } else if (Enum.class.isAssignableFrom(type)) {
      if (value instanceof Iterable || value.getClass().isArray()) {
        return toList(value, t -> t == null ? null : t.toString());
      } else {
        return value.toString();
      }
    } else {
      return value;
    }
  }

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      if (arg instanceof List) {
        List argList = (List) arg;
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
    return parameters.toArray(new Object[parameters.size()]);
  }

}
