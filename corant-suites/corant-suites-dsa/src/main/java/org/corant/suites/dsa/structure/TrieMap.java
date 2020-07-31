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
package org.corant.suites.dsa.structure;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * corant-suites-dsa
 *
 * Unfinish yet
 *
 * @author bingo 上午11:34:26
 *
 */
public class TrieMap<V> extends AbstractMap<CharSequence, V>
    implements Serializable, Map<CharSequence, V> {

  private static final long serialVersionUID = -7138732430310925683L;

  protected final TrieNode<V> root;

  protected transient Set<CharSequence> keySet;

  protected transient Collection<V> values;

  protected transient Set<Entry<CharSequence, V>> entrySet;

  protected int size;

  protected transient int modCount;

  public TrieMap() {
    this(null);
  }

  @SuppressWarnings("unchecked")
  public TrieMap(final Map<CharSequence, ? extends V> map) {
    root = map instanceof TrieMap ? ((TrieMap<V>) map).root.deepClone() : new TrieNode<>(false);
    if (map != null) {
      putAll(map);
    }
  }

  protected static CharSequence checkKey(final Object key) {
    if (key == null) {
      throw new IllegalArgumentException("The key of trie map must not null.");
    } else if (!(key instanceof CharSequence)) {
      throw new IllegalArgumentException("The key of trie map must be instance of CharSequence.");
    }
    return (CharSequence) key;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    root.children.clear();
    root.unset();
    ++modCount;
    size = 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(final Object key) {
    TrieNode<V> node = findNode(checkKey(key));
    return node != null && node.inUse;
  }

  /**
   * Returns whether an entry with the given prefix exists.
   */
  public boolean containsKeyPrefix(final CharSequence prefix) {
    return findNode(checkKey(prefix)) != null;
  }

  @Override
  public Set<Entry<CharSequence, V>> entrySet() {
    Set<Entry<CharSequence, V>> es = entrySet;
    return es != null ? es : (entrySet = new EntrySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V get(final Object key) {
    TrieNode<V> node = findNode(checkKey(key));
    return node == null ? null : node.value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<CharSequence> keySet() {
    Set<CharSequence> ks = keySet;
    return ks != null ? ks : (keySet = new KeySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V put(final CharSequence key, final V value) {
    final CharSequence useKey = checkKey(key);
    final int keyLength = useKey.length();

    V previousValue = null;
    TrieNode<V> currentNode = getRoot();
    TrieNode<V> lastNode = null;
    int i = 0;

    while (i < keyLength && currentNode != null) {
      lastNode = currentNode;
      currentNode = currentNode.children.get(useKey.charAt(i));
      ++i;
    }

    if (currentNode == null) {
      currentNode = lastNode;
      TrieNode<V> newNode = new TrieNode<>(true);
      addNode(currentNode, useKey, --i, newNode);
      updateNodeValue(newNode, value);
    } else if (currentNode.inUse) {
      previousValue = currentNode.value;
      if (previousValue != value && (previousValue == null || !previousValue.equals(value))) {
        currentNode.value = value;
      }
    } else {
      updateNodeValue(currentNode, value);
      currentNode.inUse = true;
    }
    return previousValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V remove(final Object o) {
    CharSequence key = checkKey(o);
    TrieNode<V> currentNode = findPreviousNode(key);
    if (currentNode == null) {
      return null;
    }
    TrieNode<V> node = currentNode.children.get(key.charAt(key.length() - 1));
    if (node == null || !node.inUse) {
      return null;
    }
    V removed = node.value;
    node.unset();
    --size;
    ++modCount;
    if (node.children.isEmpty()) {
      compact(key);
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return size;
  }

  public TrieMap<V> subMap(final CharSequence prefix) {
    return new SubTrieMap(this, checkKey(prefix));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<V> values() {
    Collection<V> v = values;
    return v != null ? v : (values = new Values());
  }

  protected void addNode(final TrieNode<V> node, final CharSequence key, final int beginIndex,
      final TrieNode<V> newNode) {
    int lastKeyIndex = key.length() - 1;
    TrieNode<V> currentNode = node;
    int i = beginIndex;
    for (; i < lastKeyIndex; i++) {
      TrieNode<V> nextNode = new TrieNode<>(false);
      currentNode.children.put(key.charAt(i), nextNode);
      currentNode = nextNode;
    }
    currentNode.children.put(key.charAt(i), newNode);
  }

  protected void compact(final CharSequence key) {
    final int keyLength = key.length();
    TrieNode<V> currentNode = getRoot();
    TrieNode<V> lastInUseNode = currentNode;
    int lastInUseIndex = 0;

    for (int i = 0; i < keyLength && currentNode != null; i++) {
      if (currentNode.inUse) {
        lastInUseNode = currentNode;
        lastInUseIndex = i;
      }
      currentNode = currentNode.children.get(key.charAt(i));
    }

    currentNode = lastInUseNode;
    for (int i = lastInUseIndex; i < keyLength; i++) {
      currentNode = currentNode.children.remove(key.charAt(i)).unset();
    }
  }

  protected TrieNode<V> findNode(final CharSequence key) {
    int strLen = key.length();
    TrieNode<V> currentNode = getRoot();
    for (int i = 0; i < strLen && currentNode != null; i++) {
      currentNode = currentNode.children.get(key.charAt(i));
    }
    return currentNode;
  }

  protected TrieNode<V> findPreviousNode(final CharSequence key) {
    int lastKeyIndex = key.length() - 1;
    TrieNode<V> currentNode = getRoot();
    for (int i = 0; i < lastKeyIndex && currentNode != null; i++) {
      currentNode = currentNode.children.get(key.charAt(i));
    }
    return currentNode;
  }

  /**
   * Returns the root element.
   *
   * @return The current root node.
   */
  protected TrieNode<V> getRoot() {
    return root;
  }

  protected V removeEntry(final Object o) {
    @SuppressWarnings("unchecked")
    Entry<? extends CharSequence, V> e = (Map.Entry<? extends CharSequence, V>) o;
    CharSequence key = checkKey(e.getKey());
    TrieNode<V> currentNode = findPreviousNode(key);
    if (currentNode == null) {
      return null;
    }
    TrieNode<V> node = currentNode.children.get(key.charAt(key.length() - 1));
    if (node == null || !node.inUse) {
      return null;
    }
    V value = e.getValue();
    V removed = node.value;
    if (removed != value && (removed == null || !removed.equals(value))) {
      return null;
    }
    node.unset();
    --size;
    ++modCount;
    if (node.children.isEmpty()) {
      compact(key);
    }
    return removed;
  }

  protected void updateNodeValue(final TrieNode<V> node, final V value) {
    node.value = value;
    ++modCount;
    ++size;
  }

  public static class TrieNode<V> implements Serializable {
    private static final long serialVersionUID = 4225479142841559524L;
    private final Map<Character, TrieNode<V>> children;
    private V value;
    private boolean inUse;

    public TrieNode(final boolean inUse) {
      this(null, inUse);
    }

    public TrieNode(final V value, final boolean inUse) {
      this.children = new HashMap<>();
      this.value = value;
      this.inUse = inUse;
    }

    private TrieNode(final V value, final boolean inUse, int childrenSize) {
      this.children = new HashMap<>(childrenSize);
      this.value = value;
      this.inUse = inUse;
    }

    public TrieNode<V> deepClone() {
      TrieNode<V> node = new TrieNode<>(value, inUse, children.size());
      Map<Character, TrieNode<V>> nodeChildren = node.children;
      for (Map.Entry<Character, TrieNode<V>> entry : children.entrySet()) {
        nodeChildren.put(entry.getKey(), entry.getValue().deepClone());
      }
      return node;
    }

    public TrieNode<V> unset() {
      inUse = false;
      value = null;
      return this;
    }
  }
  class EntryIterator extends TrieIterator<Entry<CharSequence, V>> {

    @Override
    public Entry<CharSequence, V> next() {
      return nextEntry();
    }
  }

  /**
   * {@inheritDoc}
   */
  class EntrySet extends AbstractSet<Entry<CharSequence, V>> {

    @Override
    public void clear() {
      TrieMap.this.clear();
    }

    @Override
    public boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }

      @SuppressWarnings("unchecked")
      final Map.Entry<CharSequence, V> e = (Map.Entry<CharSequence, V>) (Map.Entry<?, ?>) o;
      final V value = get(e.getKey());
      return value != null && value.equals(e.getValue());
    }

    @Override
    public Iterator<Entry<CharSequence, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public boolean remove(final Object o) {
      return removeEntry(o) != null;
    }

    @Override
    public int size() {
      return TrieMap.this.size();
    }
  }

  class KeyIterator extends TrieIterator<CharSequence> {

    @Override
    public CharSequence next() {
      return nextEntry().getKey();
    }
  }

  class KeySet extends AbstractSet<CharSequence> {

    @Override
    public void clear() {
      TrieMap.this.clear();
    }

    @Override
    public boolean contains(final Object o) {
      return containsKey(o);
    }

    @Override
    public Iterator<CharSequence> iterator() {
      return new KeyIterator();
    }

    @Override
    public boolean remove(final Object o) {
      return TrieMap.this.remove(o) != null;
    }

    @Override
    public int size() {
      return TrieMap.this.size();
    }
  }

  class SubTrieMap extends TrieMap<V> {
    private static final long serialVersionUID = 8640747268696329836L;
    private TrieNode<V> subRootNode;
    private TrieMap<V> parent;
    private final CharSequence prefix;

    public SubTrieMap(final TrieMap<V> parent, final CharSequence prefix) {
      this.parent = parent;
      this.prefix = prefix;
      modCount = -1;
    }

    @Override
    public void clear() {
      ensureLatest();
      final int oldSize = size;

      super.clear();
      subRootNode.unset();

      final TrieMap<V> parentMap = parent;
      parentMap.size -= oldSize;
      ++parentMap.modCount;
    }

    @Override
    public V put(CharSequence key, V value) {
      ensureLatest();

      if (subRootNode == null) {
        final CharSequence localPrefix = prefix;
        return parent.put(new StringBuilder(localPrefix.length() + key.length()).append(localPrefix)
            .append(key).toString(), value);
      } else {
        return super.put(key, value);
      }
    }

    @Override
    public V remove(final Object o) {
      ensureLatest();
      final CharSequence key = checkKey(o);

      if (key.length() == 0) {
        final CharSequence localPrefix = prefix;
        return parent.remove(new StringBuilder(localPrefix.length() + key.length())
            .append(localPrefix).append(key).toString());
      } else {
        final int capturedModCount = modCount;
        final V removed = super.remove(key);

        if (capturedModCount != modCount) {
          final TrieMap<V> parentMap = parent;
          ++parentMap.modCount;
          --parentMap.size;
        }

        return removed;
      }
    }

    @Override
    public int size() {
      ensureLatest();
      return super.size();
    }

    @Override
    public TrieMap<V> subMap(final CharSequence prefix) {
      ensureLatest();
      final CharSequence localPrefix = this.prefix;
      return parent.subMap(new StringBuilder(localPrefix.length() + prefix.length())
          .append(localPrefix).append(prefix).toString());
    }

    @Override
    protected TrieNode<V> getRoot() {
      ensureLatest();
      return subRootNode;
    }

    @Override
    protected V removeEntry(final Object o) {
      final CharSequence key = checkKey(o);
      if (key.length() == 0) {
        final CharSequence localPrefix = prefix;
        return parent.removeEntry(new StringBuilder(localPrefix.length() + key.length())
            .append(localPrefix).append(key).toString());
      } else {
        final int capturedModCount = modCount;
        final V removed = super.removeEntry(key);

        if (capturedModCount != modCount) {
          final TrieMap<V> parentMap = parent;
          ++parentMap.modCount;
          --parentMap.size;
        }
        return removed;
      }
    }

    @Override
    protected void updateNodeValue(final TrieNode<V> curNode, final V value) {
      parent.updateNodeValue(curNode, value);
      ++modCount;
      ++size;
    }

    private void ensureLatest() {
      final int parentModCount = parent.modCount;
      if (modCount < parentModCount) {
        modCount = parentModCount;
        size = size(subRootNode = parent.findNode(prefix));
      }
    }

    private int size(final TrieNode<V> node) {
      if (node == null) {
        return 0;
      }
      int newSize = 0;
      if (node.inUse) {
        ++newSize;
      }
      for (final Map.Entry<Character, TrieNode<V>> entry : node.children.entrySet()) {
        newSize += size(entry.getValue());
      }
      return newSize;
    }
  }

  class TrieEntry implements Entry<CharSequence, V> {

    private final CharSequence key;
    private final TrieNode<V> node;

    public TrieEntry(final CharSequence key, final TrieNode<V> node) {
      this.key = key;
      this.node = node;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof Map.Entry<?, ?>)) {
        return false;
      }
      final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;

      final Object k1 = key;
      final Object k2 = other.getKey();

      if (k1 != k2 && (k1 == null || !k1.equals(k2))) {
        return false;
      }

      final Object v1 = node.value;
      final Object v2 = other.getValue();

      if (v1 != v2 && (v1 == null || !v1.equals(v2))) {
        return false;
      }

      return true;
    }

    @Override
    public CharSequence getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return node.value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      final Object k = key;
      final Object v = node.value;
      int result = super.hashCode();
      result = prime * result + (k != null ? k.hashCode() : 0);
      result = prime * result + (v != null ? v.hashCode() : 0);
      return result;
    }

    @Override
    public V setValue(final V value) {
      final V oldValue = node.value;
      node.value = value;
      return oldValue;
    }
  }

  abstract class TrieIterator<E> implements Iterator<E> {

    protected int expectedModCount;
    private final Deque<TrieEntry> deque;
    private Entry<CharSequence, V> next;
    private Entry<CharSequence, V> current;

    public TrieIterator() {
      this(getRoot(), "");
    }

    public TrieIterator(final TrieNode<V> startNode, final CharSequence key) {
      expectedModCount = modCount;
      deque = new ArrayDeque<>();
      deque.add(new TrieEntry(key, startNode));
      fetchEntry();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    public Entry<CharSequence, V> nextEntry() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }

      final Entry<CharSequence, V> entry = next;
      current = entry;

      if (entry == null) {
        throw new NoSuchElementException();
      }

      fetchEntry();

      return entry;
    }

    @Override
    public void remove() {
      final Entry<CharSequence, V> entry = current;

      if (entry == null) {
        throw new IllegalStateException();
      }

      final int localModCount = modCount;

      if (localModCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }

      TrieMap.this.remove(entry.getKey());
      expectedModCount = localModCount;
    }

    private void fetchEntry() {
      final Deque<TrieEntry> localDeque = deque;
      TrieEntry localNext = null;

      while (localNext == null && !localDeque.isEmpty()) {
        final TrieEntry tempEntry = localDeque.removeFirst();
        final CharSequence key = tempEntry.key;
        final TrieNode<V> node = tempEntry.node;
        final StringBuilder sb = new StringBuilder(key.length() + 1);
        sb.append(key).append(' ');

        if (node.inUse) {
          localNext = tempEntry;
        }

        for (final Entry<Character, TrieNode<V>> entry : node.children.entrySet()) {
          sb.setCharAt(key.length(), entry.getKey());
          localDeque.addFirst(new TrieEntry(sb.toString(), entry.getValue()));
        }
      }

      next = localNext;
    }
  }

  class ValueIterator extends TrieIterator<V> {

    @Override
    public V next() {
      return nextEntry().getValue();
    }
  }

  class Values extends AbstractCollection<V> {

    @Override
    public void clear() {
      TrieMap.this.clear();
    }

    @Override
    public boolean contains(final Object o) {
      return containsValue(o);
    }

    @Override
    public Iterator<V> iterator() {
      return new ValueIterator();
    }

    @Override
    public int size() {
      return TrieMap.this.size();
    }
  }
}
