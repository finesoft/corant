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
package org.corant.suites.lang.javascript;

import static org.corant.shared.util.Lists.listOf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.StopWatch;

/**
 * corant-suites-lang-javascript
 *
 * @author bingo 上午10:37:13
 *
 */
public class ThreadSafeTest {

  static ThreadLocal<Map<Object, Consumer<Object[]>>> compiledScripts =
      ThreadLocal.withInitial(HashMap::new);
  static List<Integer> list1 = listOf(1, 1, 1, 1, 1, 1);
  static List<Integer> list11 = new ArrayList<>();
  static List<Integer> list2 = listOf(2, 2, 2, 2, 2, 2);
  static List<Integer> list22 = new ArrayList<>();

  static String script1 =
      "(function(l1,l2) {" + "l1.clear();for each(l in l2){l1.add(l);}" + "})(l1,l2);";
  static String script2 =
      "(function(l1,l2) {" + "l1.clear();for each(l in l2){l1.add(l);}" + "})(l1,l2);";

  static Consumer<Object[]> exe = NashornScriptEngines.createConsumer(script1, "l1", "l2");

  static Runnable run1 = () -> {
    long i = 1000000000000L;
    StopWatch sw = StopWatch.press();
    while (--i > 0) {
      // exe.accept(new Object[] {list11, list1});
      NashornScriptEngines.complieConsumer("1", () -> Pair.of(script1, new String[] {"l1", "l2"}))
          .accept(new Object[] {list11, list1});
      int s = list11.stream().reduce(Integer::sum).get();
      int a = list1.stream().reduce(Integer::sum).get();
      if (s != 6) {
        throw new CorantRuntimeException("error list1 %s %s", s, a);
      }
      if (i % 100000 == 0) {
        long t = sw.stop().getLastTaskInfo().getTimeMillis();
        System.out.println(
            String.format("We are %s %s, time use %s ms", Thread.currentThread().getName(), s, t));
        sw.start();
      }
    }
  };
  static Runnable run2 = () -> {
    long i = 1000000000000L;
    StopWatch sw = StopWatch.press();
    while (--i > 0) {
      // exe.accept(new Object[] {list22, list2});
      NashornScriptEngines.complieConsumer("2", () -> Pair.of(script1, new String[] {"l1", "l2"}))
          .accept(new Object[] {list22, list2});
      int s = list22.stream().reduce(Integer::sum).get();
      int a = list2.stream().reduce(Integer::sum).get();
      if (s != 12) {
        throw new CorantRuntimeException("error list2 %s %s", s, a);
      }
      if (i % 100000 == 0) {
        long t = sw.stop().getLastTaskInfo().getTimeMillis();
        System.out.println(
            String.format("We are %s %s, time use %s ms", Thread.currentThread().getName(), s, t));
        sw.start();
      }
    }
  };

  public static void main(String... strings) {
    new Thread(run1).start();
    new Thread(run2).start();
  }

}
