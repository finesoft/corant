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
package org.corant.config;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.wrap;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.strip;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.inject.Provider;
import org.corant.shared.conversion.ConverterRegistry;
import org.corant.shared.conversion.ConverterType;
import org.corant.shared.conversion.Converters;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Objects;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * corant-config
 *
 * @author bingo 下午4:27:06
 *
 */
public class CorantConfigConversion implements Serializable {

  public static final String EMPTY_ARRAY_VALUE = "None";
  public static final int BUILT_IN_CONVERTER_ORDINAL = 1;
  public static final int CUSTOMER_CONVERTER_ORDINAL = 100;
  public static final List<OrdinalConverter> BUILT_IN_CONVERTERS; // static?
  private static final long serialVersionUID = -2708805756022227289L;
  private static final Type[] objectClasses = {Object.class};
  static {
    // FIXME 6.1. Built-in Converters
    List<OrdinalConverter> builtInCvts = new LinkedList<>();
    ConverterRegistry.getSupportConverters().keySet().stream()
        .filter(ct -> ct.getSourceClass().isAssignableFrom(String.class))
        .map(ConverterType::getTargetClass).map(OrdinalConverter::builtIn)
        .forEach(builtInCvts::add);
    builtInCvts.add(OrdinalConverter.builtIn(String.class));
    BUILT_IN_CONVERTERS = Collections.unmodifiableList(builtInCvts);
  }

  private final AtomicReference<Map<Type, Converter<?>>> converters;

  /**
   * Assembly discovered converters and built in converters
   *
   * @param converters the discovered converters
   */
  CorantConfigConversion(List<OrdinalConverter> converters) {
    converters.sort(Sortable::reverseCompare);
    Map<Type, Converter<?>> useConverters = new HashMap<>();
    for (OrdinalConverter oc : converters) {
      useConverters.computeIfAbsent(oc.type, t -> oc.converter);
    }
    this.converters = new AtomicReference<>(Collections.unmodifiableMap(useConverters));
  }

  /**
   * Find the customer priority if not found then return CUSTOMER_CONVERTER_ORDINAL
   *
   * @param clazz the class with Priority annotation
   * @return the annotation Priority value or 100(the default customer converter ordinal)
   */
  public static int findPriority(Class<?> clazz) {
    Priority priorityAnnot = clazz.getAnnotation(Priority.class);
    return null != priorityAnnot ? priorityAnnot.value() : CUSTOMER_CONVERTER_ORDINAL;
  }

  /**
   * Get target class type from Converter class
   */
  public static Type getTypeOfConverter(Class<?> clazz) {
    if (clazz.equals(Object.class)) {
      return null;
    }
    Type[] genericInterfaces = clazz.getGenericInterfaces();
    for (Type genericInterface : genericInterfaces) {
      if (genericInterface instanceof ParameterizedType) {
        ParameterizedType pt = (ParameterizedType) genericInterface;
        if (pt.getRawType().equals(Converter.class)) {
          Type[] typeArguments = pt.getActualTypeArguments();
          if (typeArguments.length != 1) {
            throw new IllegalStateException("Converter " + clazz + " must be a ParameterizedType.");
          }
          return typeArguments[0];
        }
      }
    }
    return getTypeOfConverter(clazz.getSuperclass());
  }

  public Object convert(String rawValue, Type type) {
    Object result = null;
    if (isNotEmpty(rawValue)) {
      final Class<?> typeClass;
      final Type[] argTypes;
      final boolean parameterized;
      if (type instanceof Class) {
        parameterized = false;
        typeClass = forceCast(type);
        argTypes = objectClasses;
      } else if (type instanceof ParameterizedType) {
        parameterized = true;
        typeClass = forceCast(((ParameterizedType) type).getRawType());
        argTypes = ((ParameterizedType) type).getActualTypeArguments();
      } else {
        throw new IllegalArgumentException("Cannot support config property for " + type);
      }

      try {
        if (typeClass.isArray() && !typeClass.equals(char[].class)) {
          if (!parameterized) {
            result = convertArray(rawValue, typeClass.getComponentType());
          } else {
            throw new IllegalArgumentException("Cannot convert config property for type "
                + typeClass + "<" + Arrays.toString(argTypes) + ">");
          }
        } else if (Class.class.isAssignableFrom(typeClass)) {
          result = convertSingle(rawValue, Class.class);
        } else if (List.class.isAssignableFrom(typeClass) && argTypes.length == 1) {
          result = convertCollection(rawValue, argTypes[0], ArrayList::new);
        } else if (Set.class.isAssignableFrom(typeClass) && argTypes.length == 1) {
          result = convertCollection(rawValue, argTypes[0], HashSet::new);
        } else if (Optional.class.isAssignableFrom(typeClass) && argTypes.length == 1) {
          result = Optional.ofNullable(convert(rawValue, argTypes[0]));
        } else if (Supplier.class.isAssignableFrom(typeClass) && argTypes.length == 1) {
          result = (Supplier<?>) () -> convert(rawValue, argTypes[0]);
        } else if (Provider.class.isAssignableFrom(typeClass) && argTypes.length == 1) {
          result = (Provider<?>) () -> convert(rawValue, argTypes[0]);
        } else if (Map.class.isAssignableFrom(typeClass)) {
          result = parameterized ? convertMap(rawValue, (ParameterizedType) type)
              : convertMap(rawValue, (Class<?>) argTypes[0], (Class<?>) argTypes[1]);
        } else if (!parameterized) {
          result = convertSingle(rawValue, typeClass);
        } else {
          throw new IllegalArgumentException("Cannot convert config property for type " + typeClass
              + "<" + Arrays.toString(argTypes) + ">");
        }
      } catch (IllegalArgumentException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(
            String.format("Cannot convert config property value %s with type %s.", rawValue, type),
            e);
      }
    }
    return result;
  }

  /**
   * convert array
   *
   * @param <T> the array component type
   * @param rawValue the source config property string to be converted
   * @param propertyComponentType the array component class
   */
  public <T> T convertArray(String rawValue, Class<T> propertyComponentType) {
    String[] values = CorantConfigResolver.splitValue(rawValue);
    int length = values.length;
    if (length == 0) {
      return null;
    } else if (length == 1 && EMPTY_ARRAY_VALUE.equals(values[0])) {
      length = 0; // MP 2.0 ??
    }
    Object array = Array.newInstance(propertyComponentType, length);
    for (int i = 0; i < length; i++) {
      Array.set(array, i, forceCast(convert(values[i], propertyComponentType)));
    }
    return forceCast(array);
  }

  /**
   * convert collection
   *
   * @param <T> the target element type
   * @param <C> the target collection type
   * @param rawValue the source config property string to be converted
   * @param propertyItemType the target element class
   * @param collectionFactory the target collection constructor
   */
  public <T, C extends Collection<T>> C convertCollection(String rawValue, Type propertyItemType,
      IntFunction<C> collectionFactory) {
    String[] values = CorantConfigResolver.splitValue(rawValue);
    int length = values.length;
    final C collection = collectionFactory.apply(length);
    for (String value : values) {
      collection.add(forceCast(convert(value, propertyItemType)));
    }
    return collection;
  }

  public Object convertIfNecessary(Object object, Type type) {
    if (object != null) {
      return object;
    }
    Object result = null;
    if (type instanceof Class) {
      Class<?> typeClass = (Class<?>) type;
      if (typeClass.isAssignableFrom(OptionalInt.class)) {
        result = typeClass.cast(OptionalInt.empty());
      } else if (typeClass.isAssignableFrom(OptionalLong.class)) {
        result = typeClass.cast(OptionalLong.empty());
      } else if (typeClass.isAssignableFrom(OptionalDouble.class)) {
        result = typeClass.cast(OptionalDouble.empty());
      }
    } else if (type instanceof ParameterizedType) {
      if (Optional.class.equals(((ParameterizedType) type).getRawType())) {
        result = Optional.empty();
      }
    } else {
      throw new NoSuchElementException("Can not create config property for " + type);
    }
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Map convertMap(Map<String, Optional<String>> rawMap, Supplier<Map<?, ?>> factory,
      Type keyType, Type valueType) {
    Map map = factory.get();
    rawMap.forEach((rk, rv) -> {
      Object key = convert(rk, keyType);
      Object value = rv.map(s -> convert(s, valueType)).orElse(null);
      map.put(key, value);
    });
    return map;
  }

  /**
   * Convert string to map, use {@link #tryConvertStringMap(String)} to transform string to
   * Map&lt;String,String&gt; and then convert Map&lt;String,String&gt; value to the specified
   * {@code keyType} {@code valueType} type.
   *
   * @param rawValue the source config property string to be converted
   * @param keyType the target key type
   * @param valueType the target value type
   * @return convertMap
   */
  public Map<Object, Object> convertMap(String rawValue, Class<?> keyType, Class<?> valueType) {
    Map<String, String> temp = tryConvertStringMap(rawValue);
    Map<Object, Object> result = new HashMap<>(temp.size());
    temp.forEach((k, v) -> result.put(convert(k, keyType), convert(v, valueType)));
    return result;
  }

  /**
   * Convert string to map, use {@link #tryConvertStringMap(String)} to transform string to
   * Map&lt;String,String&gt; and then convert Map&lt;String,String&gt; value to the specified
   * ParameterizedType type.
   *
   * @see #tryConvertStringMap(String)
   *
   * @param rawValue the source config property string to be converted
   * @param propertyType the target type
   * @return convertMap
   */
  public Map<Object, Object> convertMap(String rawValue, ParameterizedType propertyType) {
    if (propertyType.getActualTypeArguments().length == 0) {
      return convertMap(rawValue, Object.class, Object.class);
    } else if (propertyType.getActualTypeArguments().length == 2) {
      Class<?> kt = Object.class;
      Class<?> vt = Object.class;
      if (propertyType.getActualTypeArguments()[0] instanceof Class) {
        kt = (Class<?>) propertyType.getActualTypeArguments()[0];
      }
      if (propertyType.getActualTypeArguments()[1] instanceof Class) {
        kt = (Class<?>) propertyType.getActualTypeArguments()[0];
      }
      return convertMap(rawValue, kt, vt);
    }
    return null;
  }

  public <T> T convertSingle(String value, Class<T> propertyType) {
    if (propertyType == String.class || propertyType == Object.class
        || propertyType == CharSequence.class) {
      return forceCast(value);
    } else {
      final Converter<?> converter = converters.get().get(wrap(propertyType));
      if (null != converter) {
        return forceCast(converter.convert(value));
      } else {
        return forceCast(ImplicitConverters.of(propertyType)
            .orElse(forceCast(
                OptionalsConverters.of(propertyType, this).orElse(s -> toObject(s, propertyType))))
            .convert(value));
      }
    }
  }

  public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
    // TODO MP 2.0
    Converter<?> converter;
    if (forType == String.class || forType == Object.class) {
      converter = s -> s;
      return Optional.of(forceCast(converter));
    }

    if (forType.isArray() && getConverter(forType.getComponentType()).isPresent()) {
      converter = v -> convertArray(v, forType.getComponentType());
      return Optional.of(forceCast(converter));
    }

    if ((converter = converters.get().get(wrap(forType))) != null) {
      return Optional.of(forceCast(converter));
    }

    if ((converter = OptionalsConverters.of(forType, this).orElse(null)) != null) {
      return Optional.of(forceCast(converter));
    }

    if ((converter = ImplicitConverters.of(forType).orElse(null)) != null) {
      return Optional.of(forceCast(converter));
    }

    Optional<org.corant.shared.conversion.Converter<String, T>> corantConverter =
        Converters.lookup(String.class, forType);
    if (corantConverter.isPresent()) {
      converter = s -> corantConverter.get().apply(s, null);
      return Optional.of(forceCast(converter));
    }
    return Optional.empty();
  }

  /**
   * Supports string to Map&lt;String,String&gt;.
   *
   * <p>
   * Use a {@code ','} to cut the string into segments,if each segment contains {@code '='} (and
   * does not start with {@code '='}) then each segment is treated as a key-value pair; otherwise
   * the odd segment is the key of the key-value pair , The next even segment is the value of the
   * key-value pair. All key and value are trimmed.
   * </p>
   *
   * <pre>
   * "key"                                  =>  { "key":"" }
   * "key=value"                            =>  { "key":"value" }
   * "key,value"                            =>  { "key":"value" }
   * "key1=value1,key2=value2,key3="        =>  { "key1":"value1", "key2":"value2", "key3":"" }
   * "key1=value1,key2=value2,key3"         =>  { "key1=value1":"key2=value2", "key3":"" }
   * "key1=value1,key2=value2&x=1&y=2"      =>  { "key1":"value1", "key2":"value2&x=1&y=2" }
   * "key1,value1,key2,value2"              =>  { "key1":"value1", "key2":"value2" }
   * "key1,value1,key2,value2&x=1&y=2"      =>  { "key1":"value1", "key2":"value2&x=1&y=2" }
   * </pre>
   *
   * @param rawValue the source config property string to be converted
   * @return tryConvertStringMap
   */
  protected Map<String, String> tryConvertStringMap(String rawValue) {
    String[] values = CorantConfigResolver.splitValue(rawValue);
    List<String> vals = new ArrayList<>();
    boolean useKvs = true;
    for (String val : values) {
      int s = val.indexOf('=');
      if (s > 0 && s < val.length()) {
        String kvKey = strip(val.substring(0, s));
        String kvVal = strip(val.substring(s + 1));
        if (isNotBlank(kvKey)) {
          vals.add(kvKey);// key
          vals.add(kvVal);// value
        }
      } else {
        useKvs = false;
        break;
      }
    }
    if (useKvs) {
      values = vals.toArray(new String[0]);
      vals.clear();
    }
    return mapOf((Object[]) values);
  }

  void closeCloseableConverters() {
    Map<Type, Converter<?>> map = converters.get();
    if (map != null) {
      map.values().stream()
          .filter(c -> BUILT_IN_CONVERTERS.stream().noneMatch(o -> areEqual(o.converter, c))
              && c instanceof AutoCloseable)
          .forEach(c -> {
            try {
              ((Closeable) c).close();
            } catch (IOException e) {
              // Noop!
            }
          });
    }
  }

  Class<?> resolveActualTypeArguments(ParameterizedType ptype, int idx) {
    Type argType = ptype.getActualTypeArguments()[idx];
    if (argType instanceof Class) {
      return (Class<?>) argType;
    } else if (argType instanceof WildcardType) {
      return Object.class;
    } else {
      throw new IllegalStateException(
          String.format("Can not resolve parameterized type %s.", ptype));
    }
  }

  /**
   * corant-config
   *
   * @author bingo 上午11:25:26
   *
   */
  static class ImplicitConverters {

    private ImplicitConverters() {}

    public static <T> Optional<Converter<T>> of(Type generalType) {
      if (!(generalType instanceof Class)) {
        return Optional.empty();
      }
      @SuppressWarnings("unchecked")
      Class<T> type = (Class<T>) generalType;
      return Stream
          .<Supplier<Converter<T>>>of(() -> forMethod(type, "of", String.class),
              () -> forMethod(type, "valueOf", String.class),
              () -> forMethod(type, "parse", CharSequence.class),
              () -> forConstructor(type, String.class))
          .map(Supplier::get).filter(Objects::isNotNull).findFirst();
    }

    static <T> Converter<T> forConstructor(Class<T> type, Class<?>... argumentTypes) {
      try {
        Constructor<T> constructor = type.getConstructor(argumentTypes);
        if (Modifier.isPublic(constructor.getModifiers())) {
          return s -> {
            try {
              return constructor.newInstance(s);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
              throw new IllegalArgumentException(
                  "Unable to convert value to type " + type + " for value " + s, ex);
            }
          };
        } else {
          return null;
        }
      } catch (NoSuchMethodException | SecurityException e) {
        return null;
      }
    }

    static <T> Converter<T> forMethod(Class<T> type, String method, Class<?>... argumentTypes) {
      try {
        final Method factoryMethod = type.getMethod(method, argumentTypes);
        if (Modifier.isStatic(factoryMethod.getModifiers())
            && Modifier.isPublic(factoryMethod.getModifiers())) {
          return s -> {
            try {
              return forceCast(factoryMethod.invoke(null, s));
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
              throw new IllegalArgumentException(
                  "Unable to convert value to type " + type + " for value " + s, ex);
            }
          };
        } else {
          return null;
        }
      } catch (NoSuchMethodException | SecurityException e) {
        return null;
      }
    }
  }

  static class OptionalsConverters {

    OptionalsConverters() {}

    public static <T> Optional<Converter<T>> of(Class<T> type, CorantConfigConversion conversion) {
      Converter<T> converter = null;
      if (type.equals(OptionalInt.class)) {
        converter = s -> {
          Integer fs = conversion.convertSingle(s, Integer.class);
          if (fs == null) {
            return forceCast(OptionalInt.empty());
          } else {
            return forceCast(OptionalInt.of(fs));
          }
        };
      } else if (type.equals(OptionalDouble.class)) {
        converter = s -> {
          Double fs = conversion.convertSingle(s, Double.class);
          if (fs == null) {
            return forceCast(OptionalDouble.empty());
          } else {
            return forceCast(OptionalDouble.of(fs));
          }
        };
      } else if (type.equals(OptionalLong.class)) {
        converter = s -> {
          Long fs = conversion.convertSingle(s, Long.class);
          if (fs == null) {
            return forceCast(OptionalLong.empty());
          } else {
            return forceCast(OptionalLong.of(fs));
          }
        };
      }
      return converter == null ? Optional.empty() : Optional.of(converter);
    }
  }

  /**
   * corant-config
   *
   * @author bingo 上午11:25:40
   *
   */
  static class OrdinalConverter implements Sortable {
    final Class<?> type;
    final Converter<?> converter;
    final int ordinal;

    OrdinalConverter(Class<?> type, Converter<?> converter, int ordinal) {
      this.type = wrap(type);
      this.converter = converter;
      this.ordinal = ordinal;
    }

    static OrdinalConverter builtIn(Class<?> type) {
      return new OrdinalConverter(type, s -> {
        if (s == null) {
          throw new NullPointerException();
        }
        try {
          return toObject(s, type);
        } catch (Exception e) {
          throw new IllegalArgumentException(
              "Unable to convert value to type " + type + " for value " + s);
        }
      }, BUILT_IN_CONVERTER_ORDINAL);
    }

    @Override
    public int getPriority() {
      return ordinal;
    }
  }

}
