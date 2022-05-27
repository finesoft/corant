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

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.trim;
import java.util.List;
import org.corant.config.Configs;
import org.corant.modules.query.QueryRuntimeException;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午5:40:20
 *
 */
public interface DynamicTemplateMethodModelEx<P> extends TemplateMethodModelEx {

  ConfigValueTemplateMethodModelEx CONFIG_TMM_INST = new ConfigValueTemplateMethodModelEx();

  String TM = "TM";
  String CM = "CM";

  /**
   * Return resolved parameters
   *
   * @return getParameters
   */
  P getParameters();

  /**
   * The type
   *
   * @return getType
   */
  default String getType() {
    return TM;
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:26:56
   *
   */
  class ConfigValueTemplateMethodModelEx implements DynamicTemplateMethodModelEx<Object> {

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
      if (isNotEmpty(arguments)) {
        int size = sizeOf(arguments);
        String configName =
            trim(shouldInstanceOf(arguments.get(0), TemplateScalarModel.class).getAsString());
        Class<?> configClass = String.class;
        Object defaultValue = null;
        if (size > 1) {
          configClass = asClass(
              trim(shouldInstanceOf(arguments.get(1), TemplateScalarModel.class).getAsString()));
          if (size > 2) {
            Object argVal = arguments.get(2);
            if (argVal != null) {
              if (argVal instanceof TemplateScalarModel) {
                defaultValue = ((TemplateScalarModel) argVal).getAsString();
              } else if (argVal instanceof TemplateDateModel) {
                defaultValue = ((TemplateDateModel) argVal).getAsDate();
              } else if (argVal instanceof TemplateNumberModel) {
                defaultValue = ((TemplateNumberModel) argVal).getAsNumber();
              } else if (argVal instanceof TemplateBooleanModel) {
                defaultValue = ((TemplateBooleanModel) argVal).getAsBoolean();
              } else {
                throw new QueryRuntimeException("Can't support template method parameter %s",
                    argVal);
              }
            }
          }
        }
        if (defaultValue == null) {
          return Configs.getValue(configName, configClass);
        } else {
          final Class<?> cc = configClass;
          final Object dv = defaultValue;
          return defaultObject(Configs.getValue(configName, configClass), () -> toObject(dv, cc));
        }
      }
      return null;
    }

    @Override
    public Object getParameters() {
      return null;
    }

    @Override
    public String getType() {
      return CM;
    }

  }
}
