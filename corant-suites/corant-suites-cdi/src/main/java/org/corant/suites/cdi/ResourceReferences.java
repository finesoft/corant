/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.cdi;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * corant-kernel
 *
 * @author bingo 下午7:15:41
 *
 */
public class ResourceReferences {

  public static <T> ResourceReference<T> ref(final Supplier<T> supplier) {
    return ref(supplier, null);
  }

  public static <T> ResourceReference<T> ref(final Supplier<T> supplier,
      final Consumer<T> releaser) {

    return new ResourceReference<T>() {

      volatile T instance;

      @Override
      public T getInstance() {
        if (instance == null) {
          synchronized (this) {
            if (instance == null) {
              instance = supplier.get();
            }
          }
        }
        return instance;
      }

      @Override
      public void release() {
        if (releaser != null) {
          releaser.accept(instance);
        }
      }
    };
  }

  public static <T> ResourceReferenceFactory<T> refac(final Supplier<T> supplier) {
    return () -> ref(supplier);
  }
}
