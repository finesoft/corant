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
package org.corant.suites.cdi;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.trim;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import org.corant.shared.util.StringUtils;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午2:10:11
 *
 */
public class Qualifiers {

  public static final Map<String, Annotation[]> resolveNameds(Set<String> names) {
    Map<String, Annotation[]> nameds = new HashMap<>();
    if (isNotEmpty(names)) {
      Set<String> tNames = names.stream().map(StringUtils::defaultTrim).collect(Collectors.toSet());
      if (tNames.size() == 1) {
        String name = defaultTrim(tNames.iterator().next());
        if (isBlank(name)) {
          nameds.put(name, new Annotation[] {Default.Literal.INSTANCE, Any.Literal.INSTANCE});
        } else {
          nameds.put(name, new Annotation[] {Default.Literal.INSTANCE, Any.Literal.INSTANCE,
              NamedLiteral.of(name)});
        }
      } else {
        for (String name : tNames) {
          nameds.put(name, resolveNameds(name));
        }
      }
    }
    return nameds;
  }

  static final Annotation[] resolveNameds(String name) {
    return isBlank(name) ? new Annotation[] {Unnamed.INST, Any.Literal.INSTANCE}
        : new Annotation[] {NamedLiteral.of(trim(name)), Any.Literal.INSTANCE,
            Default.Literal.INSTANCE};
  }

  public static class DefaultNamedQualifierObjectManager<T extends NamedObject>
      implements NamedQualifierObjectManager<T> {

    protected final Map<String, T> objects;
    protected final Map<String, Annotation[]> nameAndQualifiers;
    protected final Map<T, Annotation[]> objectAndQualifiers;

    public DefaultNamedQualifierObjectManager(Iterable<T> configs) {
      Map<String, T> tmpObjects = new HashMap<>();
      if (isNotEmpty(configs)) {
        for (T t : configs) {
          if (t != null) {
            tmpObjects.put(defaultTrim(t.getName()), t);
          }
        }
      }
      // this.objects = Collections.unmodifiableMap(tmpObjects);
      this.objects = tmpObjects;
      // this.nameAndQualifiers = Collections.unmodifiableMap(resolveNameds(tmpObjects.keySet()));
      this.nameAndQualifiers = resolveNameds(tmpObjects.keySet());
      Map<T, Annotation[]> tmpObjectAndQualifiers = new HashMap<>();
      nameAndQualifiers.forEach((k, v) -> tmpObjectAndQualifiers.put(this.objects.get(k), v));
      // this.objectAndQualifiers = Collections.unmodifiableMap(tmpObjectAndQualifiers);
      this.objectAndQualifiers = tmpObjectAndQualifiers;

    }

    @Override
    public void destroy() {
      objects.clear();
      nameAndQualifiers.clear();
      objectAndQualifiers.clear();
    }

    @Override
    public T get(String name) {
      return objects.get(name);
    }

    @Override
    public Set<String> getAllDisplayNames() {
      return objects.keySet().stream().map(n -> isBlank(n) ? "Unamed" : n)
          .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllNames() {
      return Collections.unmodifiableSet(objects.keySet());
    }

    @Override
    public Map<String, T> getAllWithNames() {
      return Collections.unmodifiableMap(objects);
    }

    @Override
    public Map<T, Annotation[]> getAllWithQualifiers() {
      return Collections.unmodifiableMap(objectAndQualifiers);
    }

    @Override
    public Annotation[] getQualifiers(String name) {
      return nameAndQualifiers.getOrDefault(defaultTrim(name), new Annotation[0]);
    }

    @Override
    public boolean isEmpty() {
      return objects.isEmpty();
    }

    @Override
    public int size() {
      return objects.size();
    }
  }

  public interface NamedObject {
    String getName();
  }

  public interface NamedQualifierObjectManager<T extends NamedObject> {

    @SuppressWarnings("rawtypes")
    NamedQualifierObjectManager EMPTY = new NamedQualifierObjectManager() {};

    @SuppressWarnings("unchecked")
    static <X extends NamedObject> NamedQualifierObjectManager<X> empty() {
      return EMPTY;
    }

    default void destroy() {}

    default T get(String name) {
      return null;
    }

    default Set<String> getAllDisplayNames() {
      return Collections.emptySet();
    }

    default Set<String> getAllNames() {
      return Collections.emptySet();
    }

    default Map<String, T> getAllWithNames() {
      return Collections.emptyMap();
    }

    default Map<T, Annotation[]> getAllWithQualifiers() {
      return Collections.emptyMap();
    }

    default Annotation[] getQualifiers(String name) {
      return new Annotation[0];
    }

    default boolean isEmpty() {
      return true;
    }

    default int size() {
      return 0;
    }

    public abstract static class AbstractNamedObject implements NamedObject {

      private String name;

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
        AbstractNamedObject other = (AbstractNamedObject) obj;
        if (name == null) {
          return other.name == null;
        } else if (!name.equals(other.name)) {
          return false;
        }
        return true;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
      }

      protected void setName(String name) {
        this.name = defaultTrim(name);
      }

    }

  }

}
