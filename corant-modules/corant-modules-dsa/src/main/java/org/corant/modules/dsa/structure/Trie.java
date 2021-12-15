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
package org.corant.modules.dsa.structure;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-dsa
 *
 * Unfinish yet
 *
 * @author bingo 20:03:54
 *
 */
public class Trie<T> implements Comparable<Trie<T>>, Serializable {

  private static final long serialVersionUID = 5237256403224749003L;
  public static final int MAX_SIZE = 65536;

  protected Trie<T>[] childNodes = null;
  protected double rate = 0.9;
  protected char index;
  protected byte status = 1;
  protected T value = null;

  public Trie() {}

  public Trie(char index, int status, T value) {
    this.index = index;
    this.status = (byte) status;
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  public Trie(double rate) {
    childNodes = new Trie[MAX_SIZE];
    this.rate = rate;
  }

  private Trie(char index) {
    this.index = index;
  }

  public static <T> Iterator<Pair<String, T>> tokenize(Trie<T> trie, String text) {

    return null;
  }

  @SuppressWarnings("unchecked")
  public Trie<T> addChild(Trie<T> child) {
    if (childNodes == null) {
      childNodes = new Trie[0];
    }
    int idx = indexOf(child.getIndex());
    if (idx >= 0) {
      if (this.childNodes[idx] == null) {
        this.childNodes[idx] = child;
      }
      Trie<T> node = this.childNodes[idx];
      switch (child.getStatus()) {
        case -1:
          node.setStatus(1);
          break;
        case 1:
          if (node.getStatus() == 3) {
            node.setStatus(2);
          }
          break;
        case 3:
          if (node.getStatus() != 3) {
            node.setStatus(2);
          }
          node.setValue(child.getValue());
      }
      return node;
    } else if (childNodes != null && childNodes.length >= MAX_SIZE * rate) {
      Trie<T>[] tempChildNodes = new Trie[MAX_SIZE];
      for (Trie<T> node : childNodes) {
        tempChildNodes[node.getIndex()] = node;
      }
      tempChildNodes[child.getIndex()] = child;
      childNodes = null;
      childNodes = tempChildNodes;
    } else {
      Trie<T>[] newChildNodes = new Trie[childNodes.length + 1];
      int insert = -(idx + 1);
      System.arraycopy(this.childNodes, 0, newChildNodes, 0, insert);
      System.arraycopy(childNodes, insert, newChildNodes, insert + 1, childNodes.length - insert);
      newChildNodes[insert] = child;
      this.childNodes = newChildNodes;
    }
    return child;
  }

  @SuppressWarnings("unchecked")
  public void clear() {
    childNodes = new Trie[MAX_SIZE];
  }

  @Override
  public int compareTo(Trie<T> o) {
    return Integer.compare(this.index, o.index);
  }

  public boolean contains(Trie<T> node) {
    if (this.childNodes == null || node == null) {
      return false;
    }
    return Arrays.binarySearch(this.childNodes, node) > -1;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Trie other = (Trie) obj;
    if (index != other.index) {
      return false;
    }
    return true;
  }

  public T get(String key) {
    Trie<T> node = getChildNode(key);
    return node == null ? null : node.getValue();
  }

  public Trie<T> getChildNode(String key) {
    return key == null ? null : getChildNode(key.toCharArray());
  }

  public Trie<T>[] getChildNodes() {
    return childNodes;
  }

  public char getIndex() {
    return this.index;
  }

  public byte getStatus() {
    return this.status;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + index;
  }

  public void put(String key, T value) {
    Trie<T> node = this;
    for (int i = 0; i < key.length(); i++) {
      if (key.length() == i + 1) {
        node.addChild(new Trie<>(key.charAt(i), 3, value));
      } else {
        node.addChild(new Trie<T>(key.charAt(i), 1, null));
      }
      node = node.childNodes[node.indexOf(key.charAt(i))];
    }
  }

  public void remove(String key) {
    getChildNode(key).status = 1;
    getChildNode(key).value = null;
  }

  protected Trie<T> getChildNode(char[] chars) {
    Trie<T> node = this;
    int index;
    for (char idx : chars) {
      index = node.indexOf(idx);
      if (index < 0) {
        return null;
      }
      if ((node = node.childNodes[index]) == null) {
        return null;
      }
    }
    return node;
  }

  protected T getValue() {
    return this.value;
  }

  protected int indexOf(char c) {
    if (childNodes == null) {
      return -1;
    }
    if (childNodes.length == MAX_SIZE) {
      return c;
    }
    return Arrays.binarySearch(this.childNodes, new Trie<T>(c));
  }

  protected void setStatus(int status) {
    this.status = (byte) status;
  }

  protected void setValue(T value) {
    this.value = value;
  }
}
