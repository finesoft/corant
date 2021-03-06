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
package org.corant.modules.rpc.feign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * corant-modules-rpc-feign
 *
 * Use the FeignClient qualifier on an injection to point to indicate that this injection point is
 * meant to use an instance of a Type-Safe Feign Client.
 *
 * @author bingo 10:12:50
 *
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FeignClient {

  FeignClient LITERAL = new FeignClientLiteral();

  class FeignClientLiteral extends AnnotationLiteral<FeignClient> implements FeignClient {

    private static final long serialVersionUID = 6093147504142122378L;

  }

}
