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
package org.corant.modules.bundle;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.newConcurrentHashSet;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.modules.bundle.MessageSource.MessageSourceRefreshedEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;

/**
 * corant-modules-bundle
 *
 * <p>
 * Default message resolver implementation, it is used to resolve messages, support for the
 * parameterization and internationalization of such messages. All message sources come from
 * {@link MessageSourceManager#stream()}, and the resolving process will process message sources and
 * message parameters according to the respective priorities of {@link MessageSource} and
 * {@link MessageParameterResolver}.
 *
 * <p>
 * In message resolving, a {@link MessageInterpreter} is used to process the raw message source and
 * return the final message. In message parameter resolving, a {@link MessageParameterResolver}
 * chain is used to process the message parameter before it pass to the {@link MessageInterpreter}.
 *
 * @see MessageSourceManager#stream()
 * @see MessageSource#getMessage(Locale, Object, Function)
 * @see MessageInterpreter#apply(Object[], Locale)
 * @see MessageParameterResolver#handle(Locale, Object)
 *
 * @author bingo 下午8:01:13
 */
@ApplicationScoped
public class DefaultMessageResolver implements MessageResolver {

  protected final Map<Locale, Map<String, MessageInterpreter>> holder = new ConcurrentHashMap<>();
  protected final Set<String> validKeys = newConcurrentHashSet();
  protected final AtomicLong lastSourceVersion = new AtomicLong(0);
  protected volatile long cachedSourceVersion = 0;

  @Inject
  protected Logger logger;

  @Inject
  protected MessageSourceManager sourceManager;

  @Inject
  @Any
  protected Instance<MessageSourceFilter> sourceFilters;

  @Inject
  @Any
  protected Instance<MessageParameterResolver> parameterResolver;

  @Override
  public void close() throws Exception {
    holder.clear();
    validKeys.clear();
    sourceManager.release();
    logger.info(() -> "Close message resolver, all caches are cleared.");
  }

  @Override
  public String getMessage(Locale locale, Object key, Object[] params,
      Function<Locale, String> failLookupHandler) {
    Locale useLocale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = resolveParameters(useLocale, params);
    String rawMsg = resolveRawMessage(useLocale, key);
    String msg = rawMsg == null ? null : resolveMessage(useLocale, key, rawMsg, parameters);
    if (msg == null) {
      logger.warning(() -> String.format("Can't find any message for %s", key));
      if (failLookupHandler != null) {
        msg = failLookupHandler.apply(locale);
      } else {
        throw new NoSuchMessageException("Can't find any message for %s.", key);
      }
    }
    return msg;
  }

  protected void onMessageSourceRefreshed(@Observes MessageSourceRefreshedEvent e) {
    lastSourceVersion.incrementAndGet();
  }

  @PreDestroy
  protected void onPreDestroy() {
    try {
      holder.clear();
      validKeys.clear();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Produces
  @MessageKey
  @Dependent
  protected String produce(InjectionPoint ip) {
    String key = null;
    Locale locale = Locale.getDefault();
    Object[] parameters = Objects.EMPTY_ARRAY;
    for (Annotation ann : ip.getQualifiers()) {
      if (ann instanceof MessageKey) {
        MessageKey mc = (MessageKey) ann;
        key = mc.value();
        if (isNotBlank(mc.locale())) {
          locale = LocaleUtils.langToLocale(mc.locale(), PropertyResourceBundle.LOCALE_SPT_CHAR);
        }
        parameters = mc.parameters();
        break;
      }
    }
    if (isNotBlank(key)) {
      return getMessage(locale, key, parameters);
    }
    throw new NoSuchMessageException("Can't find any message");
  }

  protected MessageInterpreter resolveInterpreter(String pattern, Locale locale) {
    return new DefaultMessageInterpreter(pattern, locale);
  }

  protected String resolveMessage(Locale locale, Object key, String rawMessage, Object[] params) {
    if (key == null || !verifyKeyAndMessage(key.toString(), rawMessage)) {
      return null;
    }
    String msg = holder.computeIfAbsent(locale, l -> new ConcurrentHashMap<>(256))
        .computeIfAbsent(key.toString(), c -> resolveInterpreter(rawMessage, locale))
        .apply(params, locale);
    // FIXME Use another ways to avoid the holder invalid or dirty
    final long lsv = lastSourceVersion.get();
    if (cachedSourceVersion != lsv) {
      holder.clear();
      validKeys.clear();
      synchronized (this) {
        if (cachedSourceVersion != lsv) {
          cachedSourceVersion = lsv;
        }
      }
    }
    return msg;
  }

  protected Object resolveParameter(Locale locale, Object obj) {
    MutableObject<Object> mo = new MutableObject<>(obj);
    if (!parameterResolver.isUnsatisfied()) {
      parameterResolver.stream().filter(h -> h.supports(obj)).sorted(Sortable::compare)
          .forEach(h -> mo.apply(o -> h.handle(locale, o)));
    }
    return mo.get();
  }

  protected Object[] resolveParameters(Locale locale, Object[] parameters) {
    if (parameters != null && parameters.length > 0) {
      return Arrays.stream(parameters).map(p -> resolveParameter(locale, p)).toArray();
    }
    return Objects.EMPTY_ARRAY;
  }

  protected String resolveRawMessage(Locale locale, Object key) {
    return sourceManager.stream().sorted(Sortable::compare)
        .map(b -> b.getMessage(locale, key, s -> null)).filter(Strings::isNotBlank).findFirst()
        .orElse(null);
  }

  protected boolean verifyKeyAndMessage(String key, String rawMessage) {
    if (!validKeys.contains(key)) {
      if (!sourceFilters.isUnsatisfied()) {
        MessageSourceFilter filter = sourceFilters.stream().max(Sortable::compare).orElse(null);
        if (filter != null && !filter.test(key, rawMessage)) {
          return false;
        }
      }
      validKeys.add(key);
    }
    return true;
  }
}
