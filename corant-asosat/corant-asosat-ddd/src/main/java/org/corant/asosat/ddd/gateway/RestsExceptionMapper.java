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
package org.corant.asosat.ddd.gateway;

import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.Logger;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.kernel.exception.GeneralRuntimeExceptionMessager;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author bingo 下午7:14:45
 *
 */
@Provider
@ApplicationServices
@ApplicationScoped
public class RestsExceptionMapper implements ExceptionMapper<Exception> {

  final static Locale locale = Locale.CHINA;// FIXME locale singleton request??

  @Inject
  Logger logger;

  @Inject
  GeneralRuntimeExceptionMessager exceptionMessager;

  @Inject
  @ConfigProperty(name = "gateway.exprose-error-cause", defaultValue = "true")
  Boolean exproseErrorCause;

  public RestsExceptionMapper() {}

  @Override
  public Response toResponse(Exception exception) {
    logger.error(exception.getLocalizedMessage(), exception);
    if (exception instanceof GeneralRuntimeException) {
      GeneralRuntimeException gre = GeneralRuntimeException.class.cast(exception);
      return Response.serverError()
          .entity(mapOf("message", gre.getLocalizedMessage(locale, exceptionMessager), "attributes",
              gre.getAttributes(), "code", GlobalMessageCodes.genMessageCode(gre)))
          .type(MediaType.APPLICATION_JSON).build();
    } else if (exception instanceof WebApplicationException) {
      return WebApplicationException.class.cast(exception).getResponse();
    } else {
      Object res = exproseErrorCause
          ? mapOf("message", exceptionMessager.getUnknowErrorMessage(locale), "cause",
              mapOf("exception:", exception.getClass().getName(), "message",
                  exception.getLocalizedMessage()))
          : mapOf("message", exceptionMessager.getUnknowErrorMessage(locale));
      return Response.serverError().entity(res).type(MediaType.APPLICATION_JSON).build();
    }
  }

}
