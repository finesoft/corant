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
package org.corant.suites.query.jpql;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:56:57
 *
 */
public class JpqlTemplateMethodModelEx implements DynamicTemplateMethodModelEx<Object[]> {

  public static final String SQL_PS_PLACE_HOLDER = "?";
  public static final SimpleScalar SQL_SS_PLACE_HOLDER = new SimpleScalar(SQL_PS_PLACE_HOLDER);
  public static final String TYPE = "JPQL";

  private List<Object> parameters = new ArrayList<>();
  private int seq = 0;

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
          placeHolders[i] = getPlaceHolder();
        }
        return new SimpleScalar(String.join(",", placeHolders));
      } else {
        parameters.add(arg);
        return getPlaceHolder();
      }
    }
    return arguments;
  }

  @Override
  public Object[] getParameters() {
    return parameters.toArray(new Object[parameters.size()]);
  }

  @Override
  public String getType() {
    return TYPE;
  }

  String getPlaceHolder() {
    String pl = SQL_PS_PLACE_HOLDER + seq;
    seq++;
    return pl;
  }

}
