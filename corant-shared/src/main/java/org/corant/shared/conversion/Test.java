package org.corant.shared.conversion;

import org.corant.shared.conversion.converter.IdentityConverter;
import org.corant.shared.util.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.corant.shared.conversion.Converters.*;

/**
 * cps-m2b <br>
 *
 * @auther sushuaihao 2019/7/26
 * @since
 */
public class Test {

  static Map<Class<?>, Set<Class<?>>> srcClassToTargetClasses = new HashMap<>();
  static Map<Class<?>, Set<Class<?>>> targetClassConvertedFromSrcClasses = new HashMap<>();

  public static void main(String[] args) {
    Set<ConverterType<?, ?>> converterTypes = ConverterRegistry.getNotSyntheticConverterTypes();
    ConverterTypes2Map(converterTypes);
    //        data();
    long start = System.currentTimeMillis();
    Stack<Class<?>> stack = getConvertibleClasses(String.class, Date.class);
    Stack<Converter> converterStack = transformConverterStack(stack);
    Set<ConverterType<?, ?>> converterTypeSet1 = transformConverterTypeSet(stack);
    System.out.println("converterTypeSet1 = " + converterTypeSet1);

    Converter converter = IdentityConverter.INSTANCE;
    while (!converterStack.isEmpty()) {
      converter = converter.compose(converterStack.pop());
    }
    System.out.println("converter = " + converter);
    long end = System.currentTimeMillis() - start;
    System.out.println("time1============== " + end);

    Set<ConverterType<?, ?>> pipeConverterTypes = new LinkedHashSet<>();
    Converter matchedConverter =
        getMatchedConverter(String.class, Date.class, 3, pipeConverterTypes::addAll);

    Converter<String, Date> matchedConverterx =
        getMatchedConverterx(String.class, Date.class, pipeConverterTypes::addAll);
    System.out.println("matchedConverterx = " + matchedConverterx);
    System.out.println("pipeConverterTypes = " + pipeConverterTypes);
  }

  private static void data() {
    targetClassConvertedFromSrcClasses.computeIfAbsent(
        ValidateUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(UnsafeAccessors.class);
          return set;
        });
    srcClassToTargetClasses.computeIfPresent(
        Timestamp.class,
        (k, set) -> {
          set.add(AnnotationUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfPresent(
        Timestamp.class,
        (k, set) -> {
          set.add(Assertions.class);
          return set;
        });
    srcClassToTargetClasses.computeIfPresent(
        Timestamp.class,
        (k, set) -> {
          set.add(BitUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        AnnotationUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ClassPaths.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        AnnotationUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ClassUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        AnnotationUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ClassUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        AnnotationUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(CollectionUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        Assertions.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(Conversions.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        Assertions.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(Empties.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        BitUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(EncryptUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        BitUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(FieldUtils.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        ClassUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(FileUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        CollectionUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(Identifiers.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        Conversions.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(IterableUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        Empties.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(MacAddrUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        EncryptUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(LaunchUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        FieldUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(MapUtils.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        FileUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(MethodUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        Identifiers.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ObjectUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        IterableUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(PathUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        MacAddrUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(RandomUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        LaunchUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(Resources.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        MapUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ClassPaths.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        Resources.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(SerializationUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        SerializationUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(StopWatch.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        StopWatch.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(StreamUtils.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        StreamUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(StringUtils.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        StringUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(Throwables.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        Throwables.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(TypeUtils.class);
          return set;
        });
    srcClassToTargetClasses.computeIfAbsent(
        TypeUtils.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(UnsafeAccessors.class);
          return set;
        });

    srcClassToTargetClasses.computeIfAbsent(
        UnsafeAccessors.class,
        k -> {
          HashSet<Class<?>> set = new HashSet<>();
          set.add(ValidateUtils.class);
          return set;
        });
  }

  static Stack<Class<?>> getConvertibleClasses(Class<?> sourceClass, Class<?> targetClass) {
    List<Class<?>> srcMatchedList =
        srcClassToTargetClasses.keySet().stream()
            .filter(src -> match(sourceClass, src))
            .collect(Collectors.toList());
    List<Class<?>> tagMatchedList =
        targetClassConvertedFromSrcClasses.keySet().stream()
            .filter(tag -> match(targetClass, tag))
            .collect(Collectors.toList());
    for (Class<?> tagCls : tagMatchedList) {
      Set<Class<?>> endClassesCanConvertTargetClass =
          targetClassConvertedFromSrcClasses.get(tagCls);
      for (Class<?> srcCls : srcMatchedList) {
        Set<Class<?>> classesCanBeConvertedBySrcClass = srcClassToTargetClasses.get(srcCls);
        for (Class<?> canBeConvertedBySrcClass : classesCanBeConvertedBySrcClass) {
          Stack classesPipe =
              searchMatchedClass(
                  canBeConvertedBySrcClass, endClassesCanConvertTargetClass, new Stack());
          if (classesPipe.size() > 0) {
            Stack<Class<?>> stack = new Stack();
            stack.push(srcCls);
            stack.addAll(classesPipe);
            stack.push(tagCls);
            //            break;
            return stack;
          }
        }
      }
    }
    return null;
  }

  static Set<ConverterType<?, ?>> transformConverterTypeSet(Stack<Class<?>> stack) {
    if (stack == null || stack.size() == 0) return null;
    Set<ConverterType<?, ?>> converters = new LinkedHashSet();
    for (int i = stack.size() - 1; i > 0; i--) {
      Class<?> tag = stack.get(i);
      Class<?> src = stack.get(i - 1);
      ConverterType<?, ?> converterType = ConverterType.of(src, tag);
      converters.add(converterType);
    }
    return converters;
  }

  static Stack<ConverterType> transformConverterTypeStack(Stack<Class<?>> stack) {
    if (stack == null || stack.size() == 0) return null;
    Stack<ConverterType> converters = new Stack();
    for (int i = stack.size() - 1; i > 0; i--) {
      Class<?> tag = stack.get(i);
      Class<?> src = stack.get(i - 1);
      ConverterType<?, ?> converterType = ConverterType.of(src, tag);
      converters.push(converterType);
    }
    return converters;
  }

  static Stack<Converter> transformConverterStack(Stack<Class<?>> stack) {
    if (stack == null || stack.size() == 0) return null;
    Stack<Converter> converters = new Stack();
    for (int i = 1; i <= stack.size() - 1; i++) {
      Class<?> tag = stack.get(i);
      Class<?> src = stack.get(i - 1);
      Converter converter = getMatchedConverter(src, tag);
      converters.push(converter);
    }
    return converters;
  }

  static Stack searchMatchedClass(
      Class<?> classCanBeConvertedBySrcClass,
      Set<Class<?>> endClassesSetCanConvertTargetClass,
      Stack stack) {
    Stack classSurvival = new Stack();
    classSurvival.addAll(stack);
    if (classSurvival.contains(classCanBeConvertedBySrcClass)) {
      // avoid closed-loop
      return classSurvival;
    }
    classSurvival.push(classCanBeConvertedBySrcClass);
    if (endClassesSetCanConvertTargetClass.contains(classCanBeConvertedBySrcClass))
      return classSurvival;
    else {
      Set<Class<?>> children = srcClassToTargetClasses.get(classCanBeConvertedBySrcClass);
      if (children == null || children.size() == 0) {
        classSurvival.pop();
      } else {
        Stack childSurvivalStack = new Stack<>();
        boolean childFound = false;
        for (Class<?> child : children) {
          childSurvivalStack =
              searchMatchedClass(child, endClassesSetCanConvertTargetClass, classSurvival);
          if (childSurvivalStack.size() > classSurvival.size()) {
            childFound = true;
            break;
          }
        }
        if (childFound) {
          classSurvival = childSurvivalStack;
        } else {
          classSurvival.pop();
        }
      }
      return classSurvival;
    }
  }

  public static void ConverterTypes2Map(Set<ConverterType<?, ?>> converterTypes) {
    converterTypes.forEach(
        converterType -> {
          srcClassToTargetClasses
              .computeIfAbsent(converterType.getSourceClass(), k -> new HashSet<>())
              .add(converterType.getTargetClass());
          targetClassConvertedFromSrcClasses
              .computeIfAbsent(converterType.getTargetClass(), k -> new HashSet<>())
              .add(converterType.getSourceClass());
        });
  }
}
