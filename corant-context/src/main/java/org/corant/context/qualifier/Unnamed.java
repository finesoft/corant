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
package org.corant.context.qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * corant-context
 *
 * This qualifier is used to extend the Named Qualifier mechanism of CDI. When an interface has
 * multiple implementations, one of which has no specified name and the others have specified names,
 * the instance without a specified name has a default qualifier called Unnamed. This approach may
 * change in the future.
 *
 * @author bingo 上午11:29:39
 *
 */
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface Unnamed {

  UnnamedLiteral INST = new UnnamedLiteral();

  class UnnamedLiteral extends AnnotationLiteral<Unnamed> {

    private static final long serialVersionUID = 1401984266283097479L;

  }
}
