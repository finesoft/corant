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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

  P getParameters();

  default Object getParamValue(Object arg) throws TemplateModelException {
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
        list.add(getParamValue(tsm.get(i)));
      }
      return list;
    } else if (arg instanceof TemplateCollectionModel) {
      final TemplateModelIterator it = ((TemplateCollectionModel) arg).iterator();
      List<TemplateModel> tsm = listOf(new Iterator<TemplateModel>() {
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
      });
      int size = tsm.size();
      List<Object> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(getParamValue(tsm.get(i)));
      }
      return list;
    } else {
      throw new QueryRuntimeException("Unknow arguement,the class is %s",
          arg == null ? "null" : arg.getClass());
    }
  }

  String getType();

}
