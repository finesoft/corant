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
package org.corant.shared.conversion;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.min;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.conversion.converter.IdentityConverter;

/**
 * corant-shared
 *
 * @author bingo 15:14:49
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ExperimentalConverters {

  private static final Logger LOGGER = Logger.getLogger(ExperimentalConverters.class.getName());

  static <S, T> Converter<S, T> getMatchedConverter(Class<S> sourceClass, Class<T> targetClass,
      int maxNestingDepth, Consumer<Set<ConverterType<?, ?>>> consumer) {
    // Only original converter type to compose
    Set<ConverterType<?, ?>> converterTypes = ConverterRegistry.getNotSyntheticConverterTypes();
    Queue<ConverterPipe> candidatedPipes = new LinkedList<>();
    Queue<ConverterPipe> matchedPipes = new LinkedList<>();
    if (quickMatch(converterTypes, sourceClass, targetClass)) {
      for (ConverterType candidate : converterTypes) {
        if (Converters.match(candidate.getTargetClass(), targetClass)) {
          candidatedPipes.add(ConverterPipe.of(candidate));
        }
      }
      loop(converterTypes, sourceClass, candidatedPipes, matchedPipes, maxNestingDepth);
      ConverterPipe matchedPipe =
          matchedPipes.stream().map(cp -> cp.complete(ConverterRegistry.getConverters()::get))
              .sorted((cp1, cp2) -> Integer.compare(cp1.getMatchedScore(), cp2.getMatchedScore()))
              .findFirst().orElse(null);
      if (matchedPipe != null) {
        Converter converter = IdentityConverter.INSTANCE;
        while (!matchedPipe.getConverters().isEmpty()) {
          converter = converter.compose(matchedPipe.getConverters().remove(0));
        }
        consumer.accept(matchedPipe.getStack());
        LOGGER.fine(() -> String.format(
            "Can not find the direct converter for %s -> %s, use converter pipe [%s] !",
            sourceClass, targetClass, String.join("->", matchedPipe.getStack().stream()
                .map(ConverterType::toString).toArray(String[]::new))));
        return converter;
      }
    }
    return null;
  }

  static <S, T> Converter<S, T> getMatchedConverterx(Class<S> sourceClass, Class<T> targetClass,
      Consumer<Set<ConverterType<?, ?>>> consumer) {
    Stack<Class<?>> convertibleClasses =
        ConverterHunt.getConvertibleClasses(sourceClass, targetClass);
    if (isEmpty(convertibleClasses)) {
      return null;
    }
    Stack<Converter> converters = ConverterHunt.transformConverterStack(convertibleClasses);
    Set<ConverterType<?, ?>> converterTypes =
        ConverterHunt.transformConverterTypeSet(convertibleClasses);
    Converter converter = IdentityConverter.INSTANCE;
    while (!converters.isEmpty()) {
      converter = converter.compose(converters.pop());
    }
    consumer.accept(converterTypes);
    LOGGER.fine(() -> String.format(
        "Can not find the direct converter for %s -> %s, use converter pipe [%s] !", sourceClass,
        targetClass, String.join("->",
            converterTypes.stream().map(ConverterType::toString).toArray(String[]::new))));
    return converter;
  }

  static void loop(Set<ConverterType<?, ?>> converterTypes, Class<?> src,
      Queue<ConverterPipe> pipes, Queue<ConverterPipe> matchedPipes, int nestingDepth) {
    ConverterPipe pipe = null;
    int maxNestingDepth = nestingDepth;
    while ((pipe = pipes.poll()) != null) {
      if (pipe.isBroken()) {
        continue;
      }
      ConverterType tail = pipe.getTail();
      List<ConverterType> candidates = new ArrayList<>();
      boolean hasCandidates = false;
      for (ConverterType candidate : converterTypes) {
        if (Converters.match(tail.getSourceClass(), candidate.getTargetClass())
            && !pipe.contains(candidate)) {
          candidates.add(candidate);
          hasCandidates = true;
        }
      }
      if (hasCandidates) {
        ConverterType matched = null;
        for (ConverterType candidate : candidates) {
          if (Converters.match(candidate.getSourceClass(), src)) {
            matched = candidate;
            break;
          }
        }
        if (matched != null && pipe.append(matched)) {
          pipe.setMatch(true);
          matchedPipes.add(pipe);
          maxNestingDepth = min(pipe.getStack().size(), maxNestingDepth);
        } else {
          if (pipe.getStack().size() < maxNestingDepth) {
            for (ConverterType candidate : candidates) {
              ConverterPipe newPipe = ConverterPipe.of(pipe);
              if (newPipe.append(candidate)) {
                pipes.add(newPipe);
              }
            }
          }
        }
      } else {
        pipe.setBroken(true);
      }
    } // end while
  }

  static boolean quickMatch(Set<ConverterType<?, ?>> converterTypes, Class<?> src, Class<?> tag) {
    return converterTypes.stream().map(ConverterType::getSourceClass)
        .anyMatch(supportSourceClass -> Converters.match(supportSourceClass, src))
        && converterTypes.stream().map(ConverterType::getTargetClass)
            .anyMatch(supportTargetClass -> Converters.match(tag, supportTargetClass));
  }

  /**
   * corant-shared
   *
   * @author bingo 下午8:45:28
   */
  public static class ConverterPipe {

    private final Set<ConverterType<?, ?>> stack = new LinkedHashSet<>();
    private final List<Converter<?, ?>> converters = new LinkedList<>();

    private boolean match = false;
    private boolean broken = false;
    private ConverterType<?, ?> tail;

    ConverterPipe(ConverterPipe pipe) {
      stack.addAll(shouldNotNull(pipe).getStack());
      tail = pipe.tail;
    }

    ConverterPipe(ConverterType<?, ?> tail) {
      super();
      stack.add(tail);
      this.tail = tail;
    }

    public static ConverterPipe of(ConverterPipe pipe) {
      return new ConverterPipe(pipe);
    }

    public static ConverterPipe of(ConverterType tail) {
      return new ConverterPipe(tail);
    }

    public boolean isBroken() {
      return broken;
    }

    public boolean isMatch() {
      return match;
    }

    @Override
    public String toString() {
      return "ConverterPipe [" + stack.size() + "] [stack=" + stack + "]";
    }

    protected boolean append(ConverterType<?, ?> ct) {
      return !contains(ct) && getStack().add(ct);
    }

    protected ConverterPipe complete(Function<ConverterType<?, ?>, Converter<?, ?>> map) {
      shouldBeTrue(isMatch());
      getConverters().clear();
      for (ConverterType<?, ?> ct : getStack()) {
        getConverters().add(shouldNotNull(map.apply(ct)));
      }
      return this;
    }

    protected boolean contains(ConverterType<?, ?> converterType) {
      return stack.contains(converterType);
    }

    /** @return the converters */
    protected List<Converter<?, ?>> getConverters() {
      return converters;
    }

    protected int getMatchedScore() {
      return getConverters().stream()
          .map(c -> (c.isPossibleDistortion() ? 13 : 3) * getStack().size())
          .reduce(0, Integer::sum);
    }

    protected Set<ConverterType<?, ?>> getStack() {
      return stack;
    }

    protected ConverterType getTail() {
      return tail;
    }

    protected void setBroken(boolean broken) {
      if (broken) {
        stack.clear();
        converters.clear();
      }
      this.broken = broken;
    }

    protected void setMatch(boolean match) {
      this.match = match;
    }
  }

  static class ConverterHunt {

    static Stack<Class<?>> getConvertibleClasses(Class<?> sourceClass, Class<?> targetClass) {
      Map<Class<?>, Set<Class<?>>> srcClassMappedItsTagClasses = new HashMap<>();
      Map<Class<?>, Set<Class<?>>> tagClassMappedItsSrcClasses = new HashMap<>();
      ConverterRegistry.getNotSyntheticConverterTypes().forEach(ct -> {
        srcClassMappedItsTagClasses.computeIfAbsent(ct.getSourceClass(), k -> new HashSet<>())
            .add(ct.getTargetClass());
        tagClassMappedItsSrcClasses.computeIfAbsent(ct.getTargetClass(), k -> new HashSet<>())
            .add(ct.getSourceClass());
      });

      List<Class<?>> srcClsAndItsParentClasses = srcClassMappedItsTagClasses.keySet().stream()
          .filter(src -> Converters.match(src, sourceClass)).collect(Collectors.toList());
      List<Class<?>> tagClsAndItsParentClasses = tagClassMappedItsSrcClasses.keySet().stream()
          .filter(tag -> Converters.match(targetClass, tag)).collect(Collectors.toList());
      for (Class<?> tagCls : tagClsAndItsParentClasses) {
        Set<Class<?>> endClassesCanConvertTargetClass = tagClassMappedItsSrcClasses.get(tagCls);
        for (Class<?> srcCls : srcClsAndItsParentClasses) {
          Set<Class<?>> classesCanBeConvertedBySrcClass = srcClassMappedItsTagClasses.get(srcCls);
          for (Class<?> canBeConvertedBySrcClass : classesCanBeConvertedBySrcClass) {
            Stack<Class<?>> classesPipe = searchMatchedClass(srcClassMappedItsTagClasses,
                canBeConvertedBySrcClass, endClassesCanConvertTargetClass, new Stack<>());
            if (isNotEmpty(classesPipe)) {
              Stack<Class<?>> stack = new Stack<>();
              stack.push(srcCls);
              stack.addAll(classesPipe);
              stack.push(tagCls);
              return stack;
            }
          }
        }
      }
      return null;
    }

    static Stack<Class<?>> searchMatchedClass(
        Map<Class<?>, Set<Class<?>>> srcClassMappedItsTagClasses,
        Class<?> classCanBeConvertedBySrcClass, Set<Class<?>> endClassesSetCanConvertTargetClass,
        Stack<Class<?>> stack) {
      Stack<Class<?>> classSurvival = new Stack<>();
      classSurvival.addAll(stack);
      if (classSurvival.contains(classCanBeConvertedBySrcClass)) {
        // avoid closed-loop
        return classSurvival;
      }
      classSurvival.push(classCanBeConvertedBySrcClass);
      // if (endClassesSetCanConvertTargetClass.stream()
      // .anyMatch(x -> Converters.match(x, classCanBeConvertedBySrcClass))) {
      if (endClassesSetCanConvertTargetClass.contains(classCanBeConvertedBySrcClass)) {
        return classSurvival;
      } else {
        Set<Class<?>> children = srcClassMappedItsTagClasses.get(classCanBeConvertedBySrcClass);
        // Set<Class<?>> children = new HashSet<>();
        // srcClassMappedItsTagClasses.entrySet().stream()
        // .filter(e -> Converters.match(e.getKey(), classCanBeConvertedBySrcClass))
        // .map(Map.Entry::getValue).forEach(children::addAll);
        if (isEmpty(children)) {
          classSurvival.pop();
        } else {
          Stack<Class<?>> childSurvivalStack = new Stack<>();
          boolean childFound = false;
          for (Class<?> child : children) {
            childSurvivalStack = searchMatchedClass(srcClassMappedItsTagClasses, child,
                endClassesSetCanConvertTargetClass, classSurvival);
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

    @SuppressWarnings("rawtypes")
    static Stack<Converter> transformConverterStack(Stack<Class<?>> stack) {
      Stack<Converter> converters = new Stack<>();
      for (int i = 1; i <= stack.size() - 1; i++) {
        Class<?> tag = stack.get(i);
        Class<?> src = stack.get(i - 1);
        Converter converter = Converters.getMatchedConverter(src, tag);
        converters.push(converter);
      }
      return converters;
    }

    static Set<ConverterType<?, ?>> transformConverterTypeSet(Stack<Class<?>> stack) {
      Set<ConverterType<?, ?>> converters = new LinkedHashSet<>();
      for (int i = stack.size() - 1; i > 0; i--) {
        Class<?> tag = stack.get(i);
        Class<?> src = stack.get(i - 1);
        ConverterType<?, ?> converterType = ConverterType.of(src, tag);
        converters.add(converterType);
      }
      return converters;
    }
  }
}
