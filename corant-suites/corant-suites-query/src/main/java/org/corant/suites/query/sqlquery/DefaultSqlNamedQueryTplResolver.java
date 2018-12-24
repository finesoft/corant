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
package org.corant.suites.query.sqlquery;

import java.util.ArrayList;
import java.util.List;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.dynamic.template.DynamicQueryTplResolver;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * asosat-query
 *
 * @author bingo 下午7:56:57
 *
 */
public class DefaultSqlNamedQueryTplResolver implements DynamicQueryTplResolver<Object[]> {

  public static final String SQL_PS_PLACE_HOLDER = "?";
  public static final SimpleScalar SQL_SS_PLACE_HOLDER = new SimpleScalar(SQL_PS_PLACE_HOLDER);

  private List<Object> parameters = new ArrayList<>();

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments != null && arguments.size() == 1) {
      Object arg = getParamValue(arguments.get(0));
      if (arg instanceof List) {
        List ss = List.class.cast(arg);
        List<String> sphs = new ArrayList<>(ss.size());
        for (int i = 0; i < ss.size(); i++) {
          parameters.add(ss.get(i));
          sphs.add(SQL_PS_PLACE_HOLDER);
        }
        return new SimpleScalar(String.join(",", sphs.toArray(new String[0])));
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

  @Override
  public QueryTemplateMethodModelType getType() {
    return QueryTemplateMethodModelType.SP;
  }

  Object getParamValue(Object arg) throws TemplateModelException {
    if (arg instanceof TemplateScalarModel) {
      return ((TemplateScalarModel) arg).getAsString().trim();
    } else if (arg instanceof TemplateDateModel) {
      return ((TemplateDateModel) arg).getAsDate();
    } else if (arg instanceof TemplateNumberModel) {
      return ((TemplateNumberModel) arg).getAsNumber();
    } else if (arg instanceof TemplateBooleanModel) {
      return ((TemplateBooleanModel) arg).getAsBoolean();
    } else if (arg instanceof TemplateSequenceModel) {
      List<Object> list = new ArrayList<>();
      TemplateSequenceModel ss = (TemplateSequenceModel) arg;
      for (int i = 0; i < ss.size(); i++) {
        list.add(getParamValue(ss.get(i)));
      }
      return list;
    } else if (arg instanceof WrapperTemplateModel) {
      return ((WrapperTemplateModel) arg).getWrappedObject();
    } else {
      throw new QueryRuntimeException(
          "Unknow arguement,the class is " + (arg == null ? "null" : arg.getClass()));
    }
  }

}
