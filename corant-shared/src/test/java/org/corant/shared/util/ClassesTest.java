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
package org.corant.shared.util;

import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Set;
import org.corant.shared.conversion.Conversion;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 上午10:25:38
 *
 */
public class ClassesTest extends TestCase {

  public static void main(String... args) {
    int[] ee = null;
    int[] inta = new int[] {1, 2, 3, 4};
    Object x = inta;
    final String[] arr = new String[0];
    Object[] objsss = new Object[] {"123"};
    Object objsss1 = new Object[] {"123"};
    System.out.println(inta.getClass().isArray());
    System.out.println(arr.getClass().isArray());
    System.out.println(x.getClass().isArray());
    System.out.println(objsss.getClass().isArray());
    System.out.println(objsss1.getClass().isArray());
    System.out.println("===============================");
    System.out.println(x instanceof Object[]);
    System.out.println("===============================");
    Object[] intaw = wrapArray(inta);
    for (Object obj : intaw) {
      System.out.print(obj + ",");
    }
    System.out.println(ee);
    System.out.println("===============================");
    String[] ints = Conversion.convertArray(intaw, String.class, null);
    for (String obj : ints) {
      System.out.print(obj + ",");
    }
  }

  @Test
  public void testGetAllSuperClasses() {
    Set<Class<?>> supers = Classes.getAllSuperClasses(ArrayList.class);
    Set<Class<?>> actual =
        linkedHashSetOf(AbstractList.class, AbstractCollection.class, Object.class);
    assertEquals(supers, actual);
  }

  @Test
  public void testTraverseAllSuperClasses() {
    Classes.traverseAllSuperClasses(ArrayList.class, c -> {
      System.out.println(c);
      return true;
    });
  }
}
