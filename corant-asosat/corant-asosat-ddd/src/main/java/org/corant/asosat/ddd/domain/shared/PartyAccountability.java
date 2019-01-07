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
package org.corant.asosat.ddd.domain.shared;

/**
 * @author bingo 下午7:22:55
 *
 */
public interface PartyAccountability {

  /**
   * 委托方
   *
   * @return getEntrustingParty
   */
  Party getEntrustingParty();

  /**
   * 责任方
   *
   * @return
   */
  Party getResponsibleParty();

  /**
   * 责任类型
   *
   * @return getType
   */
  AccountabilityType getType();

  /**
   *
   * @author bingo 下午7:23:43
   *
   */
  public interface AccountabilityType extends Nameable {

    /**
     * 是否含有该类型的委托责任关系
     *
     * @param entrustingParty
     * @param responsibleParty
     * @return
     */
    boolean hasConnection(final Party entrustingParty, final Party responsibleParty);

    /**
     * 检查是否可以建立委托责任关系
     *
     * @param entrustingParty
     * @param responsibleParty
     */
    void verifyConnection(final Party entrustingParty, final Party responsibleParty);
  }
}
