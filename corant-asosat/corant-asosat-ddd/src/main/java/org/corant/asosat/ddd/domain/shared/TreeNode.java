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
 * corant-asosat-ddd
 *
 * @author bingo 下午1:44:37
 *
 */
public interface TreeNode {
  int FIRST_LEVEL = 1;
  String FIRST_TREE_PATH = "";
  String TREE_PATHINFO_SEPARATOR = ";";

  /**
   * 子对象
   *
   * @return
   */
  Iterable<? extends TreeNode> getChilds();

  /**
   * 父对象
   *
   * @return
   */
  TreeNode getParent();

  /**
   * 获取树路径深度
   *
   * @return
   */
  int getPathDeep();

  /**
   * 树路径索引，一般情况下是主键的索引
   *
   * @return
   */
  String getPathIndex();

  /**
   * 相邻
   *
   * @return
   */
  Iterable<? extends TreeNode> getSiblings();
}
