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

import java.util.ArrayList;
import java.util.List;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:56:57
 *
 */
public class DynamicTemplateMethodModelExSql implements DynamicTemplateMethodModelEx<Object[]> {

  public static final String TYPE = "SP";
  public static final String SQL_PS_PLACE_HOLDER = "?";
  public static final SimpleScalar SQL_SS_PLACE_HOLDER = new SimpleScalar(SQL_PS_PLACE_HOLDER);
  private final List<Object> parameters = new ArrayList<>();

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments != null && arguments.size() == 1) {
      Object arg = getParamValue(arguments.get(0));
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

  @Override
  public String getType() {
    return TYPE;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getWrappedParamValue(WrapperTemplateModel arg) {
    Object obj = DynamicTemplateMethodModelEx.super.getWrappedParamValue(arg);
    if (Enum.class.isAssignableFrom(obj.getClass())) {
      return ((Enum) obj).name();
    }
    return obj;
  }
}
