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
package org.corant.modules.query.mongodb.converter;

import static org.corant.shared.util.Streams.streamOf;
import java.util.Collection;
import java.util.Iterator;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Iterables;
import com.mongodb.Block;
import com.mongodb.Function;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午5:06:12
 *
 */
public class MongoIterableWrapper {

  public static <T> MongoIterable<T> wrap(Iterable<T> iterable) {
    return new SimpleMongoIterable<>(iterable);
  }

  static class SimpleMongoCursor<T> implements MongoCursor<T> {

    final Iterator<T> iterator;

    SimpleMongoCursor(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public void close() {}

    @Override
    public ServerAddress getServerAddress() {
      throw new NotSupportedException();
    }

    @Override
    public ServerCursor getServerCursor() {
      return null;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public T next() {
      return iterator.next();
    }

    @Override
    public T tryNext() {
      return next();
    }

  }

  static class SimpleMongoIterable<T> implements MongoIterable<T> {

    final MongoCursor<T> cursor;
    final Iterable<T> iterable;

    public SimpleMongoIterable(Iterable<T> iterable) {
      this.iterable = iterable;
      cursor = new SimpleMongoCursor<>(iterable.iterator());
    }

    @Override
    public MongoIterable<T> batchSize(int batchSize) {
      return this;
    }

    @Override
    public MongoCursor<T> cursor() {
      return cursor;
    }

    @Override
    public T first() {
      return cursor.next();
    }

    @Override
    public void forEach(Block<? super T> block) {
      streamOf(cursor).forEach(block::apply);
    }

    @Override
    public <A extends Collection<? super T>> A into(A target) {
      streamOf(cursor).forEach(target::add);
      return target;
    }

    @Override
    public MongoCursor<T> iterator() {
      return cursor;
    }

    @Override
    public <U> MongoIterable<U> map(Function<T, U> mapper) {
      return new SimpleMongoIterable<>(Iterables.transform(iterable, mapper::apply));
    }

  }
}
