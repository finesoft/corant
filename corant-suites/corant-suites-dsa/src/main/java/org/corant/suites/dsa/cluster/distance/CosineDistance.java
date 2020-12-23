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
package org.corant.suites.dsa.cluster.distance;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.sizeOf;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-dsa
 *
 * @author bingo 下午1:55:18
 *
 */
public class CosineDistance implements Distance, DistanceMeasure {

  private static final long serialVersionUID = 1884325108414472284L;

  @Override
  public double calculate(Map<Object, Double> f1, Map<Object, Double> f2,
      Map<String, Double> hints) {
    if (f1 == null || f2 == null) {
      throw new CorantRuntimeException("Feature vectors can't be null");
    }
    double dividend = 0;
    double divisor = 1;
    Set<Map.Entry<Object, Double>> entries = f1.entrySet();
    for (Map.Entry<Object, Double> entry : entries) {
      Double v1 = entry.getValue();
      Double v2 = f2.get(entry.getKey());
      if (v1 != null && v2 != null) {
        dividend = Math.pow(v1 * v2, 2);
        divisor = Math.pow(v1, 2) + Math.pow(v2, 2);
      }
    }
    return dividend / divisor;
  }

  @Override
  public double compute(double[] a, double[] b) throws DimensionMismatchException {
    int size = sizeOf(a);
    shouldBeTrue(size == sizeOf(b), () -> new DimensionMismatchException(a.length, b.length));
    double dividend = 0;
    double divisor = 1;
    for (int i = 0; i < size; i++) {
      double v1 = a[i];
      double v2 = b[i];
      dividend = Math.pow(v1 * v2, 2);
      divisor = Math.pow(v1, 2) + Math.pow(v2, 2);
    }
    return dividend / divisor;
  }

}
