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
package org.corant.kernel.util;

import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.trim;
import java.lang.annotation.Annotation;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;

/**
 * corant-kernel
 *
 * @author bingo 下午2:10:11
 *
 */
public class Qualifiers {

  public static final Annotation resolveNamed(String name) {
    return isBlank(name) ? Unnamed.INST : NamedLiteral.of(trim(name));
  }

  public static final Annotation[] resolveNameds(String name) {
    return isBlank(name) ? new Annotation[] {Unnamed.INST, Default.Literal.INSTANCE}
        : new Annotation[] {NamedLiteral.of(trim(name)), Default.Literal.INSTANCE};
  }

}
