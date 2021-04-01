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

import freemarker.template.Configuration;

/**
 * corant-modules-query-shared
 *
 * <pre>
 * When you try to read a variable, FreeMarker will seek the variable in this order, and stops when
 * it finds a variable with the right name:
 *
 * 1.In the Environment:
 *
 *      1.If you are in a loop, in the set of loop variables. Loop variables are the variables created by
 *      directives like list.
 *
 *      2.If you are inside a macro, in the local variable set of the macro. Local variables can be created
 *      with the local directive. Also, the parameters of macros are local variables.
 *
 *      3.In the current namespace. You can put variables into a namespace with the assign directive.
 *
 *      4.In the set of variables created with global directive. FTL handles these variables as if they
 *      were normal members of the data-model. That is, they are visible in all namespaces, and you can
 *      access them as if they would be in the data-model.
 *
 * 2.In the data-model object you have passed to the process method
 *
 * 3.In the set of shared variables stored in the Configuration
 *
 * </pre>
 *
 * @author bingo 下午5:20:19
 *
 */
public class FreemarkerConfigurations {

  public static final Configuration FM_CFG = new Configuration(Configuration.VERSION_2_3_30);

  static {
    FM_CFG.setNumberFormat("computer");
    FM_CFG.setDefaultEncoding("UTF-8");
    FM_CFG.setLogTemplateExceptions(false);
    FM_CFG.setFallbackOnNullLoopVariable(false);
    FM_CFG.setWrapUncheckedExceptions(true);
  }

  private FreemarkerConfigurations() {}

}
