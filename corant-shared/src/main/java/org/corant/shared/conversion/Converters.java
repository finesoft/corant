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
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.ObjectUtils.min;
import static org.corant.shared.util.ObjectUtils.optional;
import static org.corant.shared.util.StreamUtils.asStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import org.corant.shared.conversion.converter.IdentityConverter;

/**
 * corant-shared
 *
 * <pre>
 * Lookup condition:
 * 1.The parameter source class must be equals or extends the source class of converter supported.
 * 2.For converter the target class of converter supported must be equals or extends the parameter
 * target class.
 * 3.For converter factory the target class of parameter must be equals or extends the target class
 * of converter factory supported.
 * 4.In converter pipe, the pre converted value (as next converter parameter) must be equals or
 * extends the source class of the next converter.
 * </pre>
 *
 * @author bingo 下午2:12:57
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Converters {

  private static final Logger LOGGER = Logger.getLogger(Converters.class.getName());

  public static <S, T> Optional<Converter<S, T>> lookup(Class<S> sourceClass, Class<T> targetClass,
      int maxNestingDepth) {
    if (targetClass.isAssignableFrom(sourceClass)) {
      return optional((Converter<S, T>) IdentityConverter.INSTANCE);
    } else if (ConverterRegistry.isSupportType(sourceClass, targetClass)) {
      return optional(forceCast(ConverterRegistry.getConverter(sourceClass, targetClass)));
    } else if (ConverterRegistry.isNotSupportType(sourceClass, targetClass)) {
      return optional(null);
    } else {
      Converter converter = getMatchedConverter(sourceClass, targetClass);
      Set<ConverterType<?, ?>> pipeConverterTypes = new LinkedHashSet<>();
      if (converter == null) {
        // indirect
        converter = getMatchedConverter(sourceClass, targetClass, maxNestingDepth,
            pipeConverterTypes::addAll);
      }
      if (converter != null) {
        ConverterRegistry.register(sourceClass, targetClass, (Converter<S, T>) converter,
            pipeConverterTypes.toArray(new ConverterType[pipeConverterTypes.size()]));
        return optional(forceCast(converter));
      } else {
        ConverterRegistry.registerNotSupportType(sourceClass, targetClass);
        return optional(null);
      }
    }
  }

  static Converter getMatchedConverter(Class<?> sourceClass, Class<?> targetClass) {
    return asStream(ConverterRegistry.getConverters())
        .filter(e -> match(e.getKey(), sourceClass, targetClass)).map(Entry::getValue).findFirst()
        .orElse(getMatchedConverterFromFactory(sourceClass, targetClass));
  }

  static <S, T> Converter<S, T> getMatchedConverter(Class<S> sourceClass, Class<T> targetClass,
      int maxNestingDepth, Consumer<Set<ConverterType<?, ?>>> consumer) {
    // Only original converter type to compose
    Set<ConverterType<?, ?>> converterTypes = ConverterRegistry.getNotSyntheticConverterTypes();
    Queue<ConverterPipe> candidatedPipes = new LinkedList<>();
    Queue<ConverterPipe> matchedPipes = new LinkedList<>();
    if (quickMatch(converterTypes, sourceClass, targetClass)) {
      for (ConverterType candidate : converterTypes) {
        if (match(candidate.getTargetClass(), targetClass)) {
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
            sourceClass, targetClass, String.join("->",
                matchedPipe.getStack().stream().map(ct -> ct.toString()).toArray(String[]::new))));
        return converter;
      }
    }
    return null;
  }

  static Converter getMatchedConverterFromFactory(Class<?> sourceClass, Class<?> targetClass) {
    ConverterFactory factory = ConverterRegistry.getConverterFactories().values().stream()
        .filter(f -> f.isSupportSourceClass(sourceClass) && f.isSupportTargetClass(targetClass))
        .findFirst().orElse(null);

    if (factory != null) {
      // FIXME initialize parameter
      return factory.create(targetClass, null, true);
    } else {
      return null;
    }
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
        if (match(tail.getSourceClass(), candidate.getTargetClass()) && !pipe.contains(candidate)) {
          candidates.add(candidate);
          hasCandidates = true;
        }
      }
      if (hasCandidates) {
        ConverterType matched = null;
        for (ConverterType candidate : candidates) {
          if (match(candidate.getSourceClass(), src)) {
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

  static boolean match(Class<?> a, Class<?> b) {
    return a.isAssignableFrom(b);
  }

  static boolean match(ConverterType<?, ?> converterType, Class<?> sourceClass,
      Class<?> targetClass) {
    return targetClass.isAssignableFrom(converterType.getTargetClass())
        && converterType.getSourceClass().isAssignableFrom(sourceClass);
  }

  static boolean quickMatch(Set<ConverterType<?, ?>> converterTypes, Class<?> src, Class<?> tag) {
    return converterTypes.stream().map(ConverterType::getSourceClass)
        .anyMatch(supportSourceClass -> match(supportSourceClass, src))
        && converterTypes.stream().map(ConverterType::getTargetClass)
            .anyMatch(supportTargetClass -> match(tag, supportTargetClass));
  }

  /**
   * corant-shared
   *
   * @author bingo 下午8:45:28
   *
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

    /**
     *
     * @return the converters
     */
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
}
