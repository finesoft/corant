/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.conversion.converter.particular;

import static org.corant.shared.util.Primitives.wrapArray;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.corant.shared.conversion.Conversion;
import org.corant.shared.ubiquity.Tuple;
import org.corant.shared.ubiquity.Tuple.Dectet;
import org.corant.shared.ubiquity.Tuple.Duet;
import org.corant.shared.ubiquity.Tuple.Nonet;
import org.corant.shared.ubiquity.Tuple.Octet;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Quartet;
import org.corant.shared.ubiquity.Tuple.Quintet;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Septet;
import org.corant.shared.ubiquity.Tuple.Sextet;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.ubiquity.Tuple.Triplet;

/**
 * corant-shared
 *
 * @author bingo 16:35:48
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TupleConverter {

  public static Tuple convert(Object value, Class<?> targetClass, Class<?>[] argClasses,
      Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    if (value instanceof Map map) {
      if (targetClass == Pair.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "key", "value");
        if (eles == null) {
          eles = convertTupleElements(map, argClasses, hints, "left", "right");
          if (eles == null) {
            eles = convertTupleElements(map, argClasses, hints, "first", "second");
          }
        }
        if (eles != null) {
          return Pair.of(eles[0], eles[1]);
        }
      } else if (targetClass == Triple.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "left", "middle", "right");
        if (eles == null) {
          eles = convertTupleElements(map, argClasses, hints, "first", "second", "third");
        }
        if (eles != null) {
          return Triple.of(eles[0], eles[1], eles[2]);
        }
      } else if (targetClass == Duet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second");
        if (eles != null) {
          return new Duet(eles[0], eles[1]);
        }
      } else if (targetClass == Triplet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third");
        if (eles != null) {
          return new Triplet(eles[0], eles[1], eles[2]);
        }
      } else if (targetClass == Range.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "min", "max");
        if (eles != null) {
          return Range.of((Comparable) eles[0], (Comparable) eles[1]);
        }
      } else if (targetClass == Quartet.class) {
        Object[] eles =
            convertTupleElements(map, argClasses, hints, "first", "second", "third", "fourth");
        if (eles != null) {
          return new Quartet(eles[0], eles[1], eles[2], eles[3]);
        }
      } else if (targetClass == Quintet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth");
        if (eles != null) {
          return new Quintet(eles[0], eles[1], eles[2], eles[3], eles[4]);
        }
      } else if (targetClass == Sextet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth", "sixth");
        if (eles != null) {
          return new Sextet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5]);
        }
      } else if (targetClass == Septet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth", "sixth", "seventh");
        if (eles != null) {
          return new Septet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6]);
        }
      } else if (targetClass == Octet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth", "sixth", "seventh", "eighth");
        if (eles != null) {
          return new Octet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7]);
        }
      } else if (targetClass == Nonet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth", "sixth", "seventh", "eighth", "ninth");
        if (eles != null) {
          return new Nonet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7],
              eles[8]);
        }
      } else if (targetClass == Dectet.class) {
        Object[] eles = convertTupleElements(map, argClasses, hints, "first", "second", "third",
            "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth");
        if (eles != null) {
          return new Dectet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7],
              eles[8], eles[9]);
        }
      }
    } else {
      Object[] eles = null;
      if (value instanceof List list) {
        eles = list.toArray();
      } else if (value.getClass().isArray()) {
        eles = wrapArray(value);
      }
      if (eles != null) {
        int length = eles.length;
        if (targetClass == Pair.class && length >= 2) {
          eles = convertTupleElements(2, eles, argClasses, hints);
          return Pair.of(eles[0], eles[1]);
        } else if (targetClass == Triple.class && length >= 3) {
          eles = convertTupleElements(3, eles, argClasses, hints);
          return Triple.of(eles[0], eles[1], eles[2]);
        } else if (targetClass == Duet.class && length >= 2) {
          eles = convertTupleElements(2, eles, argClasses, hints);
          return new Duet(eles[0], eles[1]);
        } else if (targetClass == Triplet.class && length >= 3) {
          eles = convertTupleElements(3, eles, argClasses, hints);
          return new Triplet(eles[0], eles[1], eles[2]);
        } else if (targetClass == Range.class && length >= 2) {
          eles = convertTupleElements(2, eles, argClasses, hints);
          return Range.of((Comparable) eles[0], (Comparable) eles[1]);
        } else if (targetClass == Quartet.class && length >= 4) {
          eles = convertTupleElements(4, eles, argClasses, hints);
          return new Quartet(eles[0], eles[1], eles[2], eles[3]);
        } else if (targetClass == Quintet.class && length >= 5) {
          eles = convertTupleElements(5, eles, argClasses, hints);
          return new Quintet(eles[0], eles[1], eles[2], eles[3], eles[4]);
        } else if (targetClass == Sextet.class && length >= 6) {
          eles = convertTupleElements(6, eles, argClasses, hints);
          return new Sextet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5]);
        } else if (targetClass == Septet.class && length >= 7) {
          eles = convertTupleElements(7, eles, argClasses, hints);
          return new Septet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6]);
        } else if (targetClass == Octet.class && length >= 8) {
          eles = convertTupleElements(8, eles, argClasses, hints);
          return new Octet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7]);
        } else if (targetClass == Nonet.class && length >= 9) {
          eles = convertTupleElements(9, eles, argClasses, hints);
          return new Nonet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7],
              eles[8]);
        } else if (targetClass == Dectet.class && length >= 10) {
          eles = convertTupleElements(10, eles, argClasses, hints);
          return new Dectet(eles[0], eles[1], eles[2], eles[3], eles[4], eles[5], eles[6], eles[7],
              eles[8], eles[9]);
        }
      }
    }
    throw new IllegalArgumentException(
        "Cannot type for " + targetClass + "<" + Arrays.toString(argClasses) + ">");
  }

  private static Object[] convertTupleElements(int length, Object[] values,
      Class<?>[] elementClasses, Map<String, ?> hints) {
    Object[] elements = new Object[length];
    int len = elementClasses.length;
    for (int i = 0; i < length; i++) {
      elements[i] = i < len ? Conversion.convert(values[i], elementClasses[i], hints) : values[i];
    }
    return elements;
  }

  private static Object[] convertTupleElements(Map<?, ?> values, Class<?>[] elementClasses,
      Map<String, ?> hints, String... keys) {
    int length = keys.length;
    Object[] elements = new Object[keys.length];
    boolean accept = true;
    int len = elementClasses.length;
    for (int i = 0; i < length; i++) {
      if (values.containsKey(keys[i])) {
        elements[i] = i < len ? Conversion.convert(values.get(keys[i]), elementClasses[i], hints)
            : values.get(keys[i]);
      } else {
        accept = false;
        break;
      }
    }
    return accept ? elements : null;
  }
}
