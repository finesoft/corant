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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Stereotype;
import feign.Contract;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

/**
 * corant-modules-rpc-feign
 *
 * A marker annotation to register a feign client at runtime. This marker must be applied to any CDI
 * managed clients.
 *
 * All values can be overridden by MicroProfile Config.
 *
 * Note that the annotated interface indicates a service-centric view. Thus users would invoke
 * methods on this interface as if it were running in the same VM as the remote service.
 *
 * @author bingo 10:00:31
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
@Dependent
public @interface RegisterFeignClient {

  String baseUri() default "";

  /**
   * Associates the annotated feign client interface with this configuration key. By specifying a
   * non-empty value, this interface can be configured more simply using the configuration key
   * rather than the fully-qualified class name of the interface.
   *
   * @return the configuration key in use by this client interface. An empty value means that this
   *         interface is not associated with a configuration key.
   */
  String configKey() default "";

  int connectTimeoutMillis() default 10 * 1000;

  Class<? extends Contract> contract() default Contract.Default.class;

  boolean decode404() default false;

  Class<? extends Decoder> decoder() default Decoder.Default.class;

  Class<? extends Encoder> encoder() default Encoder.Default.class;

  Class<? extends ErrorDecoder> errorDecoder() default ErrorDecoder.Default.class;

  Class<? extends InvocationHandlerFactory> invocationHandlerFactory() default InvocationHandlerFactory.Default.class;

  Class<? extends Logger> logger() default Logger.NoOpLogger.class;

  int readTimeoutMillis() default 60 * 1000;

  Class<? extends RequestInterceptor>[] requestInterceptors() default {};

  Class<? extends Retryer> retryer() default Retryer.Default.class;
}
