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
package org.corant.asosat.ddd.application.query;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getKeyPathMapValue;
import static org.corant.shared.util.MapUtils.putKeyPathMapValue;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.split;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.corant.suites.query.Query.ForwardList;
import org.corant.suites.query.Query.PagedList;
import org.corant.suites.query.mapping.QueryHint;
import org.corant.suites.query.spi.ResultHintHandler;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
@ApplicationServices
public class EnumConverterHintHandler implements ResultHintHandler {

  @Inject
  ConversionService cs;

  @Override
  public boolean canHandle(QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), QueryHintNames.ENUM_CVT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(QueryHint hint, Object result) {
    String[] hv = split(hint.getValue(), ":", true, true);
    if (hv.length != 2) {
      return;
    }
    String path = hv[0];
    String enumClass = hv[1];
    Class<?> enumCls = tryAsClass(enumClass);
    if (enumCls == null) {
      return;
    }
    if (result instanceof Map) {
      handle(Map.class.cast(result), path, enumCls);
    } else {
      List<?> list = null;
      if (result instanceof ForwardList) {
        list = ForwardList.class.cast(result).getResults();
      } else if (result instanceof List) {
        list = List.class.cast(result);
      } else if (result instanceof PagedList) {
        list = PagedList.class.cast(result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            handle(Map.class.cast(item), path, enumCls);
          }
        }
      }
    }
  }

  protected void handle(Map<String, Object> map, String keyPath, Class<?> enumCls) {
    Map<String, Object> useMap = map;
    Object orginalVal = getKeyPathMapValue(useMap, keyPath, ".");
    if (orginalVal != null) {
      Object enumObj = cs.convert(orginalVal, enumCls);
      putKeyPathMapValue(useMap, keyPath, ".", enumObj);
    }
  }
}
