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
package org.corant.context;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.URLResource;

/**
 * corant-context
 *
 * @author bingo 16:26:34
 *
 */
@ApplicationScoped
public class ResourceProvider {

  Stream<URLResource> find(InjectionPoint injectionPoint) {
    SURI suri = null;
    for (Annotation ann : injectionPoint.getQualifiers()) {
      if (ann.annotationType().equals(SURI.class)) {
        suri = (SURI) ann;
      }
    }
    if (suri != null) {
      try {
        return Resources.from(suri.value());
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return Stream.empty();
  }

  @Produces
  @SURI
  @Dependent
  List<URLResource> list(InjectionPoint injectionPoint) {
    return find(injectionPoint).collect(Collectors.toList());
  }

  @Produces
  @SURI
  @Dependent
  Optional<URLResource> optional(InjectionPoint injectionPoint) {
    return find(injectionPoint).findFirst();
  }

  @Produces
  @SURI
  @Dependent
  Stream<URLResource> stream(InjectionPoint injectionPoint) {
    return find(injectionPoint);
  }

}
