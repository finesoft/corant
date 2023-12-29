/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import org.corant.shared.util.UnsafeAccessors;
import sun.misc.Unsafe;

/**
 * corant-shared
 *
 * @author bingo 下午8:13:44
 */
public abstract class UnsafeStruct {
  protected static final Unsafe unsafe = UnsafeAccessors.get();
  protected final int capacity;
  protected final byte[] memory;

  protected UnsafeStruct(int capacity) {
    shouldBeTrue(capacity > 0);
    this.capacity = capacity;
    memory = new byte[capacity];
  }

  public static class Mat extends UnsafeStruct {

    public Mat(int rows, int cols, int typeStride) {
      super(rows * cols * typeStride);
    }

  }
}
