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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Classes.getUserClass;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Classes;

/**
 * corant-shared
 *
 * <p>
 * Interface that can be used for sorting, note that in general, the smaller the priority number
 * {@link #getPriority()}, the more preferred, for the instances with the same priority value, the
 * name of instance class will be used for sorting according to string sorting criteria.
 *
 * @author bingo 上午10:44:33
 */
public interface Sortable {

  /**
   * Compares two {@code Sortable} values. The value returned is identical to what would be returned
   * by:
   *
   * <pre>
   * Integer.compare(s1.getPriority(), s2.getPriority())
   * or s1.getClass().getName().compareTo(s2.getClass().getName()) if priority are same.
   * </pre>
   *
   * @param s1 the first {@code Sortable} to compare
   * @param s2 the second {@code Sortable} to compare
   *
   * @see Integer#compare(int, int)
   * @see Classes#getUserClass(Object)
   * @see String#compareTo(String)
   *
   */
  static int compare(Sortable s1, Sortable s2) {
    int result;
    if ((result = Integer.compare(s1.getPriority(), s2.getPriority())) == 0) {
      result = getUserClass(s1).getName().compareTo(getUserClass(s2).getName());
    }
    return result;
  }

  /**
   * Returns the reverse compares two {@code Sortable} values. The value returned is identical to
   * what would be returned by:
   *
   * <pre>
   * -1 * compare(s1, s2)
   * </pre>
   *
   * @param s1 the first {@code Sortable} to compare
   * @param s2 the second {@code Sortable} to compare
   * @see #compare(Sortable, Sortable)
   */
  static int reverseCompare(Sortable s1, Sortable s2) {
    return -1 * compare(s1, s2);
  }

  /**
   * Returns the priority, used to identify the priority , the smaller the value, the more
   * preferred. Default is {@link Priorities#FRAMEWORK_LOWER}
   *
   * @return the priority
   */
  default int getPriority() {
    return Priorities.FRAMEWORK_LOWER;
  }
}
