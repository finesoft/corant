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
package org.corant.modules.rpc.feign;

import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.BeanManager;
import org.corant.context.AbstractBean;
import org.corant.context.Beans;
import org.corant.modules.json.ObjectMappers;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Capability;
import feign.Client;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.Feign.Builder;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.Request.Options;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * corant-modules-rpc-feign
 *
 * @author bingo 10:05:01
 */
public class FeignClientBean extends AbstractBean<Object> {

  public static final String PREFIX = "%s/feign/";
  public static final String URI_FMT = PREFIX + "uri";
  public static final String URL_FMT = PREFIX + "url";
  public static final String CAPABILITIES_FMT = PREFIX + "capabilities";
  public static final String CLOSE_AFTER_DECODE_FMT = PREFIX + "close-after-decode";
  public static final String CONNECT_TIMEOUT_FMT = PREFIX + "connect-timeout";
  public static final String CONTRACT_FMT = PREFIX + "contract";
  public static final String DECODER_FMT = PREFIX + "decoder";
  public static final String DISMISS404_FMT = PREFIX + "dismiss404";
  public static final String ENCODER_FMT = PREFIX + "encoder";
  public static final String ERROR_DECODER_FMT = PREFIX + "error-decoder";
  public static final String EXCEPTION_PROPAGATION_POLICY_FMT =
      PREFIX + "exception-propagation-policy";
  public static final String FOLLOW_REDIRECTS_FMT = PREFIX + "follow-redirects";
  public static final String INVOCATION_HANDLER_FACTORY_FMT = PREFIX + "invocation-handler-factory";
  public static final String LOGGER_FMT = PREFIX + "logger";
  public static final String LOG_LEVEL_FMT = PREFIX + "log-level";
  public static final String QUERY_MAP_ENCODER_FMT = PREFIX + "query-map-encoder";
  public static final String READ_TIMEOUT_FMT = PREFIX + "read-timeout";
  public static final String REQUEST_INTERCEPTORS_FMT = "request-interceptors";
  public static final String RESPONSE_INTERCEPTORS_FMT = "response-interceptors";
  public static final String RETRYER_FMT = "retryer";

  protected final Class<?> beanClass;
  protected final String beanClassName;
  protected final RegisterFeignClient metadata;
  protected final ObjectMapper objectMapper = ObjectMappers.copyDefaultObjectMapper();
  protected final Encoder jacksonEncoder = new JacksonEncoder(objectMapper);
  protected final Decoder jacksonDecoder = new JacksonDecoder(objectMapper);

  public FeignClientBean(BeanManager beanManager, Class<?> beanClass) {
    super(beanManager);
    this.beanClass = beanClass;
    beanClassName = Classes.getUserClass(beanClass).getCanonicalName();
    metadata = beanClass.getAnnotation(RegisterFeignClient.class);
    scope = defaultObject(Beans.resolveScope(beanClass), Dependent.class);
    Class<? extends Annotation> stere = Beans.resolveStereoType(beanClass);
    if (stere != null) {
      stereotypes.add(stere);
    }
    types.add(beanClass);
    qualifiers.add(FeignClient.LITERAL);
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext) {
    Builder builder = Beans.find(Feign.Builder.class).orElseGet(Feign::builder);
    config(builder);
    return builder.target(beanClass, resolveURI());
  }

  @Override
  public Class<?> getBeanClass() {
    return beanClass;
  }

  protected void config(Builder builder) {
    builder.client(Beans.find(Client.class).orElseGet(() -> new Client.Default(null, null)));
    builder.options(resolveRequestOptions());
    resolveConfigInstances(CAPABILITIES_FMT, Capability.class).forEach(builder::addCapability);
    resolveConfigInstance(CONTRACT_FMT, Contract.class).ifPresent(builder::contract);
    // resolveConfigInstance(DECODER_FMT, Decoder.class).ifPresent(builder::decoder);
    builder.decoder(resolveConfigInstance(DECODER_FMT, Decoder.class, () -> jacksonDecoder));
    if (resolveConfigValue(DISMISS404_FMT, Boolean.class, false)) {
      builder.dismiss404();
    }
    if (!resolveConfigValue(CLOSE_AFTER_DECODE_FMT, Boolean.class, true)) {
      builder.doNotCloseAfterDecode();
    }
    // resolveConfigInstance(ENCODER_FMT, Encoder.class).ifPresent(builder::encoder);
    builder.encoder(resolveConfigInstance(ENCODER_FMT, Encoder.class, () -> jacksonEncoder));
    resolveConfigInstance(ERROR_DECODER_FMT, ErrorDecoder.class).ifPresent(builder::errorDecoder);
    ExceptionPropagationPolicy epp =
        resolveConfigValue(EXCEPTION_PROPAGATION_POLICY_FMT, ExceptionPropagationPolicy.class);
    if (epp != null) {
      builder.exceptionPropagationPolicy(epp);
    }
    resolveConfigInstance(INVOCATION_HANDLER_FACTORY_FMT, InvocationHandlerFactory.class)
        .ifPresent(builder::invocationHandlerFactory);
    resolveConfigInstance(LOGGER_FMT, Logger.class).ifPresent(builder::logger);
    Logger.Level logLevel = resolveConfigValue(LOG_LEVEL_FMT, Logger.Level.class);
    if (logLevel != null) {
      builder.logLevel(logLevel);
    }
    resolveConfigInstance(QUERY_MAP_ENCODER_FMT, QueryMapEncoder.class)
        .ifPresent(builder::queryMapEncoder);
    List<RequestInterceptor> ris =
        resolveConfigInstances(REQUEST_INTERCEPTORS_FMT, RequestInterceptor.class);
    if (isNotEmpty(ris)) {
      builder.requestInterceptors(ris);
    }
    List<ResponseInterceptor> rsis =
        resolveConfigInstances(RESPONSE_INTERCEPTORS_FMT, ResponseInterceptor.class);
    if (isNotEmpty(rsis)) {
      builder.responseInterceptors(rsis);
    }
    resolveConfigInstance(RETRYER_FMT, Retryer.class).ifPresent(builder::retryer);
  }

  protected Options resolveRequestOptions() {
    Duration connectionTimeout = defaultObject(
        resolveConfigValue(CONNECT_TIMEOUT_FMT, Duration.class), Duration.ofSeconds(30));
    Duration readTimeout =
        defaultObject(resolveConfigValue(READ_TIMEOUT_FMT, Duration.class), Duration.ofSeconds(30));
    return new Request.Options(connectionTimeout, readTimeout,
        resolveConfigValue(FOLLOW_REDIRECTS_FMT, Boolean.class, false));
  }

  @SuppressWarnings("unchecked")
  <T> Optional<T> resolveConfigInstance(String format, Class<T> clazz) {
    T instance = null;
    String className = resolveConfigValue(format, String.class);
    if (isNotBlank(className)) {
      String[] array = split(className, "?", true, true);
      Class<T> cls = (Class<T>) Classes.asClass(array[0]);
      if (array.length > 1) {
        String[] qualiferNames = split(array[1], "&", true, true);
        Annotation[] qualifiers =
            Arrays.stream(qualiferNames).map(NamedLiteral::of).toArray(Annotation[]::new);
        instance = Beans.find(cls, qualifiers).orElse(null);
      } else {
        instance = Beans.find(cls).orElse(null);
      }
      if (instance == null) {
        instance = Objects.newInstance(cls);
      }
    }
    return Optional.ofNullable(instance);
  }

  <T> T resolveConfigInstance(String format, Class<T> clazz, Supplier<T> supplier) {
    return resolveConfigInstance(format, clazz).orElseGet(supplier);
  }

  @SuppressWarnings("unchecked")
  <T> List<T> resolveConfigInstances(String format, Class<T> clazz) {
    List<T> list = new ArrayList<>();
    List<String> classNames = resolveConfigValue(format, new TypeLiteral<List<String>>() {});
    if (isNotEmpty(classNames)) {
      for (String className : classNames) {
        T instance = null;
        String[] array = split(className, "?", true, true);
        Class<T> cls = (Class<T>) Classes.asClass(array[0]);
        if (array.length > 1) {
          String[] qualiferNames = split(array[1], "&", true, true);
          Annotation[] qualifiers =
              Arrays.stream(qualiferNames).map(NamedLiteral::of).toArray(Annotation[]::new);
          instance = Beans.find(cls, qualifiers).orElse(null);
        } else {
          instance = Beans.find(cls).orElse(null);
        }
        if (instance == null) {
          instance = Objects.newInstance(cls);
        }
        list.add(instance);
      }
    }
    return list;
  }

  <T> T resolveConfigValue(String format, Class<T> clazz) {
    return resolveConfigValue(format, clazz, null);
  }

  <T> T resolveConfigValue(String format, Class<T> clazz, T alt) {
    T value = getConfigValue(String.format(format, beanClassName), clazz);
    if (value != null) {
      return value;
    } else if (!"".equals(metadata.configKey())) {
      return defaultObject(getConfigValue(String.format(format, metadata.configKey()), clazz), alt);
    }
    return alt;
  }

  <T> T resolveConfigValue(String format, TypeLiteral<T> type) {
    T value = getConfigValue(String.format(format, beanClassName), type);
    if (value != null) {
      return value;
    } else if (!"".equals(metadata.configKey())) {
      return getConfigValue(String.format(format, metadata.configKey()), type);
    }
    return null;
  }

  String resolveURI() {
    String uri = resolveConfigValue(URI_FMT, String.class);
    if (uri == null) {
      uri = resolveConfigValue(URL_FMT, String.class);
    }
    return defaultObject(uri, metadata.baseUri());
  }

}
