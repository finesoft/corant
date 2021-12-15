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

package org.corant.shared.util;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.tryCast;
import static org.corant.shared.util.Strings.NULL;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.TypeLiteral;

/**
 * corant-shared
 *
 * Extracted from google gson.
 *
 * Static methods for working with types.
 *
 */
public class Types {

  static final Type[] EMPTY_TYPE_ARRAY = {};

  private Types() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an array type whose elements are all instances of {@code componentType}.
   *
   * @return a {@link java.io.Serializable serializable} generic array type.
   */
  public static GenericArrayType arrayOf(Type componentType) {
    return new GenericArrayTypeImpl(componentType);
  }

  /**
   * Returns a type that is functionally equal but not necessarily equal according to
   * {@link Object#equals(Object) Object.equals()}. The returned type is
   * {@link java.io.Serializable}.
   */
  public static Type canonicalize(Type type) {
    if (type instanceof Class) {
      Class<?> c = forceCast(type);
      return c.isArray() ? new GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c;

    } else if (type instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) type;
      return new ParameterizedTypeImpl(p.getOwnerType(), p.getRawType(),
          p.getActualTypeArguments());

    } else if (type instanceof GenericArrayType) {
      GenericArrayType g = (GenericArrayType) type;
      return new GenericArrayTypeImpl(g.getGenericComponentType());

    } else if (type instanceof WildcardType) {
      WildcardType w = (WildcardType) type;
      return new WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());

    } else {
      // type is either serializable as-is or unsupported
      return type;
    }
  }

  /**
   * Returns true if {@code a} and {@code b} are equal.
   */
  public static boolean equals(Type a, Type b) {
    if (a == b) {
      return true;
    } else if (a instanceof Class) {
      return a.equals(b);
    } else if (a instanceof ParameterizedType) {
      if (!(b instanceof ParameterizedType)) {
        return false;
      }
      // TODO: save a .clone() call
      ParameterizedType pa = (ParameterizedType) a;
      ParameterizedType pb = (ParameterizedType) b;
      return equal(pa.getOwnerType(), pb.getOwnerType()) && pa.getRawType().equals(pb.getRawType())
          && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());

    } else if (a instanceof GenericArrayType) {
      if (!(b instanceof GenericArrayType)) {
        return false;
      }

      GenericArrayType ga = (GenericArrayType) a;
      GenericArrayType gb = (GenericArrayType) b;
      return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

    } else if (a instanceof WildcardType) {
      if (!(b instanceof WildcardType)) {
        return false;
      }

      WildcardType wa = (WildcardType) a;
      WildcardType wb = (WildcardType) b;
      return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
          && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

    } else if (a instanceof TypeVariable) {
      if (!(b instanceof TypeVariable)) {
        return false;
      }
      TypeVariable<?> va = (TypeVariable<?>) a;
      TypeVariable<?> vb = (TypeVariable<?>) b;
      return va.getGenericDeclaration() == vb.getGenericDeclaration()
          && va.getName().equals(vb.getName());

    } else {
      // This isn't a type we support. Could be a generic array type, wildcard type, etc.
      return false;
    }
  }

  /**
   * Returns the component type of this array type.
   *
   * @throws ClassCastException if this type is not an array.
   */
  public static Type getArrayComponentType(Type array) {
    return array instanceof GenericArrayType ? ((GenericArrayType) array).getGenericComponentType()
        : tryCast(array, Class.class).getComponentType();
  }

  /**
   * Returns the element type of this collection type.
   *
   * @throws IllegalArgumentException if this type is not a collection.
   */
  public static Type getCollectionElementType(Type context, Class<?> contextRawType) {
    Type collectionType = getSupertype(context, contextRawType, Collection.class);

    if (collectionType instanceof WildcardType) {
      collectionType = ((WildcardType) collectionType).getUpperBounds()[0];
    }
    if (collectionType instanceof ParameterizedType) {
      return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
    }
    return Object.class;
  }

  /**
   * Returns a two element array containing this map's key and value types in positions 0 and 1
   * respectively.
   */
  public static Type[] getMapKeyAndValueTypes(Type context, Class<?> contextRawType) {
    /*
     * Work around a problem with the declaration of java.util.Properties. That class should extend
     * Hashtable<String, String>, but it's declared to extend Hashtable<Object, Object>.
     */
    if (context == Properties.class) {
      return new Type[] {String.class, String.class}; // TODO: test subclasses of Properties!
    }

    Type mapType = getSupertype(context, contextRawType, Map.class);
    // TODO: strip wildcards?
    if (mapType instanceof ParameterizedType) {
      ParameterizedType mapParameterizedType = (ParameterizedType) mapType;
      return mapParameterizedType.getActualTypeArguments();
    }
    return new Type[] {Object.class, Object.class};
  }

  public static Type[] getParameterizedTypes(Class<?> clazz, Class<?> supertype) {
    Type supTyp = getSupertype(canonicalize(shouldNotNull(clazz)),
        clazz.isArray() ? clazz.getComponentType() : clazz, supertype);
    if (supTyp instanceof ParameterizedType) {
      return ((ParameterizedType) supTyp).getActualTypeArguments();
    }
    return TypeLiteral.EMPTY_ARRAY;
  }

  public static Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      // type is a normal class.
      return forceCast(type);

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      // I'm not exactly sure why getRawType() returns Type instead of Class.
      // Neal isn't either but suspects some pathological case related
      // to nested classes exists.
      Type rawType = parameterizedType.getRawType();
      shouldBeTrue(rawType instanceof Class);
      return (Class<?>) rawType;

    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();

    } else if (type instanceof TypeVariable) {
      // we could use the variable's bounds, but that won't work if there are multiple.
      // having a raw type that's more general than necessary is okay
      return Object.class;

    } else if (type instanceof WildcardType) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);

    } else {
      String className = type == null ? NULL : type.getClass().getName();
      throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
          + "GenericArrayType, but <" + type + "> is of type " + className);
    }
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType} and
   * enclosed by {@code ownerType}.
   *
   * @return a {@link java.io.Serializable serializable} parameterized type.
   */
  public static ParameterizedType newParameterizedTypeWithOwner(Type ownerType, Type rawType,
      Type... typeArguments) {
    return new ParameterizedTypeImpl(ownerType, rawType, typeArguments);
  }

  @SuppressWarnings("rawtypes")
  public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
    return resolve(context, contextRawType, toResolve, new HashSet<TypeVariable>());
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for
   * {@code ? extends Object}.
   */
  public static WildcardType subtypeOf(Type bound) {
    Type[] upperBounds;
    if (bound instanceof WildcardType) {
      upperBounds = ((WildcardType) bound).getUpperBounds();
    } else {
      upperBounds = new Type[] {bound};
    }
    return new WildcardTypeImpl(upperBounds, EMPTY_TYPE_ARRAY);
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if
   * {@code bound} is {@code String.class}, this returns {@code ?
   * super String}.
   */
  public static WildcardType supertypeOf(Type bound) {
    Type[] lowerBounds;
    if (bound instanceof WildcardType) {
      lowerBounds = ((WildcardType) bound).getLowerBounds();
    } else {
      lowerBounds = new Type[] {bound};
    }
    return new WildcardTypeImpl(new Type[] {Object.class}, lowerBounds);
  }

  public static String typeToString(Type type) {
    return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
  }

  static void checkNotPrimitive(Type type) {
    shouldBeTrue(!(type instanceof Class<?>) || !((Class<?>) type).isPrimitive());
  }

  static boolean equal(Object a, Object b) {
    return a == b || a != null && a.equals(b);
  }

  /**
   * Returns the generic supertype for {@code supertype}. For example, given a class {@code
   * IntegerSet}, the result for when supertype is {@code Set.class} is {@code Set<Integer>} and the
   * result when the supertype is {@code Collection.class} is {@code Collection<Integer>}.
   */
  static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
    if (toResolve == rawType) {
      return context;
    }

    // we skip searching through interfaces if unknown is an interface
    if (toResolve.isInterface()) {
      Class<?>[] interfaces = rawType.getInterfaces();
      for (int i = 0, length = interfaces.length; i < length; i++) {
        if (interfaces[i] == toResolve) {
          return rawType.getGenericInterfaces()[i];
        } else if (toResolve.isAssignableFrom(interfaces[i])) {
          return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
        }
      }
    }

    // check our supertypes
    Class<?> usedRawType = rawType;
    if (!usedRawType.isInterface()) {
      while (usedRawType != Object.class) {
        Class<?> rawSupertype = usedRawType.getSuperclass();
        if (rawSupertype == toResolve) {
          return usedRawType.getGenericSuperclass();
        } else if (toResolve.isAssignableFrom(rawSupertype)) {
          return getGenericSupertype(usedRawType.getGenericSuperclass(), rawSupertype, toResolve);
        }
        usedRawType = rawSupertype;
      }
    }

    // we can't resolve this further
    return toResolve;
  }

  /**
   * Returns the generic form of {@code supertype}. For example, if this is {@code
   * ArrayList<String>}, this returns {@code Iterable<String>} given the input {@code
   * Iterable.class}.
   *
   * @param supertype a superclass of, or interface implemented by, this.
   */
  static Type getSupertype(Type context, Class<?> contextRawType, Class<?> supertype) {
    Type usedContext = context;
    if (context instanceof WildcardType) {
      // wildcards are useless for resolving supertypes. As the upper bound has the same raw type,
      // use it instead
      usedContext = ((WildcardType) context).getUpperBounds()[0];
    }
    shouldBeTrue(supertype.isAssignableFrom(contextRawType));
    return resolve(usedContext, contextRawType,
        Types.getGenericSupertype(usedContext, contextRawType, supertype));
  }

  static int hashCodeOrZero(Object o) {
    return o != null ? o.hashCode() : 0;
  }

  static Type resolveTypeVariable(Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
    Class<?> declaredByRaw = declaringClassOf(unknown);

    // we can't reduce this further
    if (declaredByRaw == null) {
      return unknown;
    }

    Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
    if (declaredBy instanceof ParameterizedType) {
      int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
      return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
    }

    return unknown;
  }

  /**
   * Returns the declaring class of {@code typeVariable}, or {@code null} if it was not declared by
   * a class.
   */
  private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
    GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
    return genericDeclaration instanceof Class ? (Class<?>) genericDeclaration : null;
  }

  private static int indexOf(Object[] array, Object toFind) {
    for (int i = 0, length = array.length; i < length; i++) {
      if (toFind.equals(array[i])) {
        return i;
      }
    }
    throw new NoSuchElementException();
  }

  @SuppressWarnings("rawtypes")
  private static Type resolve(Type context, Class<?> contextRawType, Type toResolve,
      Collection<TypeVariable> visitedTypeVariables) {
    // this implementation is made a little more complicated in an attempt to avoid object-creation
    Type usedToResolve = toResolve;
    while (true) {
      if (usedToResolve instanceof TypeVariable) {
        TypeVariable<?> typeVariable = (TypeVariable<?>) usedToResolve;
        if (visitedTypeVariables.contains(typeVariable)) {
          // cannot reduce due to infinite recursion
          return usedToResolve;
        } else {
          visitedTypeVariables.add(typeVariable);
        }
        usedToResolve = resolveTypeVariable(context, contextRawType, typeVariable);
        if (usedToResolve == typeVariable) {
          return usedToResolve;
        }

      } else if (usedToResolve instanceof Class && ((Class<?>) usedToResolve).isArray()) {
        Class<?> original = (Class<?>) usedToResolve;
        Type componentType = original.getComponentType();
        Type newComponentType =
            resolve(context, contextRawType, componentType, visitedTypeVariables);
        return componentType == newComponentType ? original : arrayOf(newComponentType);

      } else if (usedToResolve instanceof GenericArrayType) {
        GenericArrayType original = (GenericArrayType) usedToResolve;
        Type componentType = original.getGenericComponentType();
        Type newComponentType =
            resolve(context, contextRawType, componentType, visitedTypeVariables);
        return componentType == newComponentType ? original : arrayOf(newComponentType);

      } else if (usedToResolve instanceof ParameterizedType) {
        ParameterizedType original = (ParameterizedType) usedToResolve;
        Type ownerType = original.getOwnerType();
        Type newOwnerType = resolve(context, contextRawType, ownerType, visitedTypeVariables);
        boolean changed = newOwnerType != ownerType;

        Type[] args = original.getActualTypeArguments();
        for (int t = 0, length = args.length; t < length; t++) {
          Type resolvedTypeArgument =
              resolve(context, contextRawType, args[t], visitedTypeVariables);
          if (resolvedTypeArgument != args[t]) {
            if (!changed) {
              args = args.clone();
              changed = true;
            }
            args[t] = resolvedTypeArgument;
          }
        }

        return changed ? newParameterizedTypeWithOwner(newOwnerType, original.getRawType(), args)
            : original;

      } else if (usedToResolve instanceof WildcardType) {
        WildcardType original = (WildcardType) usedToResolve;
        Type[] originalLowerBound = original.getLowerBounds();
        Type[] originalUpperBound = original.getUpperBounds();

        if (originalLowerBound.length == 1) {
          Type lowerBound =
              resolve(context, contextRawType, originalLowerBound[0], visitedTypeVariables);
          if (lowerBound != originalLowerBound[0]) {
            return supertypeOf(lowerBound);
          }
        } else if (originalUpperBound.length == 1) {
          Type upperBound =
              resolve(context, contextRawType, originalUpperBound[0], visitedTypeVariables);
          if (upperBound != originalUpperBound[0]) {
            return subtypeOf(upperBound);
          }
        }
        return original;

      } else {
        return usedToResolve;
      }
    }
  }

  /**
   *
   * Extracted from google gson.
   *
   * Represents a generic type {@code T}. Java doesn't yet provide a way to represent generic types,
   * so this class does. Forces clients to create a subclass of this class which enables retrieval
   * the type information even at runtime.
   *
   * <p>
   * For example, to create a type literal for {@code List<String>}, you can create an empty
   * anonymous inner class:
   *
   * <p>
   * {@code TypeToken<List<String>> list = new TypeToken<List<String>>() {};}
   *
   * <p>
   * This syntax cannot be used to create type literals that have wildcard parameters, such as
   * {@code Class<?>} or {@code List<? extends CharSequence>}.
   *
   * @author Bob Lee
   * @author Sven Mawson
   * @author Jesse Wilson
   */
  public static class TypeToken<T> {

    final Class<? super T> rawType;

    final Type type;

    final int hashCode;

    /**
     * Constructs a new type literal. Derives represented class from type parameter.
     *
     * <p>
     * Clients create an empty anonymous subclass. Doing so embeds the type parameter in the
     * anonymous class's type hierarchy so we can reconstitute it at runtime despite erasure.
     */
    @SuppressWarnings("unchecked")
    public TypeToken() {
      this.type = getSuperclassTypeParameter(getClass());
      this.rawType = (Class<? super T>) Types.getRawType(type);
      this.hashCode = type.hashCode();
    }

    /**
     * Unsafe. Constructs a type literal manually.
     */
    @SuppressWarnings("unchecked")
    protected TypeToken(Type type) {
      this.type = Types.canonicalize(shouldNotNull(type));
      this.rawType = (Class<? super T>) Types.getRawType(this.type);
      this.hashCode = this.type.hashCode();
    }

    /**
     * Gets type literal for the given {@code Class} instance.
     */
    public static <T> TypeToken<T> get(Class<T> type) {
      return new TypeToken<>(type);
    }

    /**
     * Gets type literal for the given {@code Type} instance.
     */
    public static TypeToken<?> get(Type type) {
      return new TypeToken<>(type);
    }

    /**
     * Gets type literal for the array type whose elements are all instances of
     * {@code componentType}.
     */
    public static TypeToken<?> getArray(Type componentType) {
      return new TypeToken<>(Types.arrayOf(componentType));
    }

    /**
     * Gets type literal for the parameterized type represented by applying {@code typeArguments} to
     * {@code rawType}.
     */
    public static TypeToken<?> getParameterized(Type rawType, Type... typeArguments) {
      return new TypeToken<>(Types.newParameterizedTypeWithOwner(null, rawType, typeArguments));
    }

    /**
     * Returns the type from super class's type parameter in {@link Types#canonicalize canonical
     * form}.
     */
    static Type getSuperclassTypeParameter(Class<?> subclass) {
      Type superclass = subclass.getGenericSuperclass();
      if (superclass instanceof Class) {
        throw new CorantRuntimeException("Missing type parameter.");
      }
      ParameterizedType parameterized = (ParameterizedType) superclass;
      return Types.canonicalize(parameterized.getActualTypeArguments()[0]);
    }

    @Override
    public final boolean equals(Object o) {
      return o instanceof TypeToken<?> && Types.equals(type, ((TypeToken<?>) o).type);
    }

    /**
     * Returns the raw (non-generic) type for this type.
     */
    public final Class<? super T> getRawType() {
      return rawType;
    }

    /**
     * Gets underlying {@code Type} instance.
     */
    public final Type getType() {
      return type;
    }

    @Override
    public final int hashCode() {
      return this.hashCode;
    }

  }

  private static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
    private static final long serialVersionUID = 0;
    private final Type componentType;

    public GenericArrayTypeImpl(Type componentType) {
      this.componentType = canonicalize(componentType);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof GenericArrayType && Types.equals(this, (GenericArrayType) o);
    }

    @Override
    public Type getGenericComponentType() {
      return componentType;
    }

    @Override
    public int hashCode() {
      return componentType.hashCode();
    }

    @Override
    public String toString() {
      return typeToString(componentType) + "[]";
    }
  }

  private static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
    private static final long serialVersionUID = 0;
    private final Type ownerType;
    private final Type rawType;
    private final Type[] typeArguments;

    public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
      // require an owner type if the raw type needs it
      if (rawType instanceof Class<?>) {
        Class<?> rawTypeAsClass = (Class<?>) rawType;
        boolean isStaticOrTopLevelClass = Modifier.isStatic(rawTypeAsClass.getModifiers())
            || rawTypeAsClass.getEnclosingClass() == null;
        shouldBeTrue(ownerType != null || isStaticOrTopLevelClass);
      }

      this.ownerType = ownerType == null ? null : canonicalize(ownerType);
      this.rawType = canonicalize(rawType);
      this.typeArguments = typeArguments.clone();
      for (int t = 0, length = this.typeArguments.length; t < length; t++) {
        shouldNotNull(this.typeArguments[t]);
        checkNotPrimitive(this.typeArguments[t]);
        this.typeArguments[t] = canonicalize(this.typeArguments[t]);
      }
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ParameterizedType && Types.equals(this, (ParameterizedType) other);
    }

    @Override
    public Type[] getActualTypeArguments() {
      return typeArguments.clone();
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(typeArguments) ^ rawType.hashCode() ^ hashCodeOrZero(ownerType);
    }

    @Override
    public String toString() {
      int length = typeArguments.length;
      if (length == 0) {
        return typeToString(rawType);
      }

      StringBuilder stringBuilder = new StringBuilder(30 * (length + 1));
      stringBuilder.append(typeToString(rawType)).append("<")
          .append(typeToString(typeArguments[0]));
      for (int i = 1; i < length; i++) {
        stringBuilder.append(", ").append(typeToString(typeArguments[i]));
      }
      return stringBuilder.append(">").toString();
    }
  }

  /**
   * The WildcardType interface supports multiple upper bounds and multiple lower bounds. We only
   * support what the Java 6 language needs - at most one bound. If a lower bound is set, the upper
   * bound must be Object.class.
   */
  private static final class WildcardTypeImpl implements WildcardType, Serializable {
    private static final long serialVersionUID = 0;
    private final Type upperBound;
    private final Type lowerBound;

    public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
      shouldBeTrue(lowerBounds.length <= 1);
      shouldBeTrue(upperBounds.length == 1);

      if (lowerBounds.length == 1) {
        shouldNotNull(lowerBounds[0]);
        checkNotPrimitive(lowerBounds[0]);
        shouldBeTrue(upperBounds[0] == Object.class);
        lowerBound = canonicalize(lowerBounds[0]);
        upperBound = Object.class;

      } else {
        shouldNotNull(upperBounds[0]);
        checkNotPrimitive(upperBounds[0]);
        lowerBound = null;
        upperBound = canonicalize(upperBounds[0]);
      }
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof WildcardType && Types.equals(this, (WildcardType) other);
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBound != null ? new Type[] {lowerBound} : EMPTY_TYPE_ARRAY;
    }

    @Override
    public Type[] getUpperBounds() {
      return new Type[] {upperBound};
    }

    @Override
    public int hashCode() {
      // this equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds());
      return (lowerBound != null ? 31 + lowerBound.hashCode() : 1) ^ 31 + upperBound.hashCode();
    }

    @Override
    public String toString() {
      if (lowerBound != null) {
        return "? super " + typeToString(lowerBound);
      } else if (upperBound == Object.class) {
        return "?";
      } else {
        return "? extends " + typeToString(upperBound);
      }
    }
  }
}
