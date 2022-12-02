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
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
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
 * {@link MessageSource}, and the resolving process will process message sources and message
 * parameters according to the respective priorities of {@link MessageSource} and
 * {@link MessageParameterResolver}.
 *
 * <p>
 * In message resolving, a {@link MessageInterpreter} is used to process the raw message source and
 * return the final message. In message parameter resolving, a {@link MessageParameterResolver}
 * chain is used to process the message parameter before it pass to the {@link MessageInterpreter}.
 *
 * <p>
 * Note: The default implementation uses a caching mechanism, some message sources may be updated in
 * real time, so caller can call {@link #refresh()} to refresh the entire cache.
 *
 * @see MessageSource#getMessage(Locale, Object, Function)
 * @see MessageInterpreter#apply(Object[], Locale)
 * @see MessageParameterResolver#handle(Locale, Object)
 *
 * @author bingo 下午8:01:13
 */
@ApplicationScoped
public class DefaultMessageResolver implements MessageResolver {

  protected final Map<Locale, Map<String, MessageInterpreter>> holder = new ConcurrentHashMap<>();
  protected final ReadWriteLock rwl = new ReentrantReadWriteLock();

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<MessageSource> messageSources;

  @Inject
  @Any
  protected Instance<MessageParameterResolver> parameterResolver;

  @Override
  public void close() throws Exception {
    Lock writeLock = rwl.writeLock();
    try {
      writeLock.lock();
      this.holder.clear();
      if (!messageSources.isUnsatisfied()) {
        messageSources.stream().sorted(Sortable::compare).forEach(t -> {
          try {
            t.close();
          } catch (Exception e) {
            throw new CorantRuntimeException(e);
          }
        });
      }
      logger.info(() -> "Close message resolver, all caches are cleared.");
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getMessage(Locale locale, Object code, Object[] params,
      Function<Locale, String> dfltMsgResolver) {
    Locale useLocale = defaultObject(locale, Locale::getDefault);
    Object[] parameters = resolveParameters(useLocale, params);
    Lock readLock = rwl.readLock();
    try {
      readLock.lock();
      String rawMessage = resolveRawMessage(useLocale, code);
      String message = rawMessage == null ? null
          : resolveMessage(useLocale, code.toString(), rawMessage, parameters);
      if (message == null) {
        logger.warning(() -> String.format("Can't find any message for %s", code));
        if (dfltMsgResolver != null) {
          message = dfltMsgResolver.apply(locale);
        } else {
          throw new NoSuchMessageException("Can't find any message for %s.", code);
        }
      }
      return message;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void refresh() {
    logger.info(() -> "Refresh message resolver, all caches will be refreshed.");
    Lock writeLock = rwl.writeLock();
    try {
      writeLock.lock();
      holder.clear();
      if (!messageSources.isUnsatisfied()) {
        messageSources.stream().sorted(Sortable::compare).forEach(MessageSource::refresh);
      }
    } finally {
      writeLock.unlock();
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    try {
      close();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Produces
  @MessageCodes
  @Dependent
  protected String produce(InjectionPoint ip) {
    String codes = null;
    Locale locale = Locale.getDefault();
    Object[] parameters = Objects.EMPTY_ARRAY;
    for (Annotation ann : ip.getQualifiers()) {
      if (ann instanceof MessageCodes) {
        MessageCodes mc = (MessageCodes) ann;
        codes = mc.value();
        if (isNotBlank(mc.locale())) {
          locale = LocaleUtils.langToLocale(mc.locale(), PropertyResourceBundle.LOCALE_SPT_CHAR);
        }
        parameters = mc.parameters();
        break;
      }
    }
    if (isNotBlank(codes)) {
      return getMessage(locale, codes, parameters);
    }
    throw new NoSuchMessageException("Can't find any message");
  }

  protected MessageInterpreter resolveInterpreter(String pattern, Locale locale) {
    return new DefaultMessageInterpreter(pattern, locale);
  }

  protected String resolveMessage(Locale locale, String codes, String rawMessage, Object[] params) {
    return holder.computeIfAbsent(locale, l -> new ConcurrentHashMap<>(256))
        .computeIfAbsent(codes, c -> resolveInterpreter(rawMessage, locale)).apply(params, locale);
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

  protected String resolveRawMessage(Locale locale, Object code) {
    if (!messageSources.isUnsatisfied()) {
      return messageSources.stream().sorted(Sortable::compare)
          .map(b -> b.getMessage(locale, code, s -> null)).filter(Strings::isNotBlank).findFirst()
          .orElse(null);
    }
    return null;
  }
}
