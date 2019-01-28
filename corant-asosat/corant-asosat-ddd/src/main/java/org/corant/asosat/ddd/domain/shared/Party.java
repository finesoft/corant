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
package org.corant.asosat.ddd.domain.shared;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.corant.suites.ddd.model.Entity;

/**
 * @author bingo 下午7:22:18
 *
 */
public interface Party extends Nameable, Entity {

  /**
   * 委托关系列表
   *
   * @param predicate 条件限定
   * @param includeHierarchy 是否罗列所有关系
   * @return
   */
  List<? extends Party> getEntrustingParties(Predicate<PartyAccountability> predicate,
      boolean includeHierarchy);

  /**
   * 根法人组织
   *
   * @return
   */
  Party getHierarchyParty();

  /**
   * 责任关系列表
   *
   * @param predicate 条件限定
   * @param includeHierarchy 是否罗列所有关系
   * @return
   */
  List<? extends Party> getResponsibleParties(Predicate<PartyAccountability> predicate,
      boolean includeHierarchy);

  /**
   * 法人
   *
   * @author bingo 2016年9月21日
   * @since
   */
  public interface Corporation extends Party {

    /**
     * 法人经营业务范围
     *
     * @return
     */
    Set<?> getRealms();
  }

  /**
   * 用户
   *
   * @author bingo 2016年6月13日
   * @since
   */
  public interface Individual extends Party {
  }

  /**
   * 业务类型
   *
   * @author bingo 2016年9月20日
   * @since
   */
  public interface PartyRealm extends Nameable {

  }

}
