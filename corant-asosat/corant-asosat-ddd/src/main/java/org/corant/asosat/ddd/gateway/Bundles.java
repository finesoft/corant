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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.linkedHashMapOf;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.bundle.EnumerationBundle;
import org.corant.suites.bundle.MessageBundle;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:42:51
 *
 */
@Path("/bundles")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Bundles extends AbstractRests {

  @Context
  HttpServletRequest request;

  @Inject
  ConversionService cs;

  @Inject
  @Any
  Instance<EnumerationBundle> enums;

  @Inject
  @Any
  Instance<MessageBundle> messages;

  @SuppressWarnings("rawtypes")
  @Path("/getAllEnumContents/")
  @GET
  public Response getAllEnumContents() {
    if (enums.isResolvable()) {
      Map<String, Map<String, Map<String, Object>>> result = new HashMap<>();
      EnumerationBundle bundle = enums.get();
      List<Class<Enum>> enumClasses = bundle.getAllEnumClass();
      Locale locale = resolveLocale();
      enumClasses.forEach(cls -> {
        Map<String, Map<String, Object>> items =
            result.computeIfAbsent(cls.getName(), (k) -> new LinkedHashMap<>());
        for (Enum e : cls.getEnumConstants()) {
          items.put(e.name(),
              linkedHashMapOf("ordinal", e.ordinal(), "literal", bundle.getEnumItemLiteral(e, locale)));
        }
      });
      return ok(result);
    }
    return noContent();
  }

  @Path("/getEnumClassLiteral/{enumCls}/")
  @GET
  public Response getEnumClassLiteral(@PathParam("enumCls") String enumCls) {
    if (isNotBlank(enumCls) && enums.isResolvable()) {
      Class<?> cls = tryAsClass(enumCls);
      if (cls != null && cls.isEnum()) {
        return ok(enums.get().getEnumClassLiteral(cls, resolveLocale()));
      }
    }
    return noContent();
  }

  @Path("/getEnumItemLiteral/{enumItem}/")
  @GET
  public Response getEnumItemLiteral(@PathParam("enumItem") String enumItem) {
    int pos = -1;
    if (isNotBlank(enumItem) && enums.isResolvable() && (pos = enumItem.lastIndexOf('.')) > 0) {
      int len = enumItem.length();
      if (len - pos > 1) {
        String item = enumItem.substring(pos + 1);
        Class<?> cls = tryAsClass(enumItem.substring(0, pos));
        if (cls != null && cls.isEnum()) {
          return ok(
              enums.get().getEnumItemLiteral((Enum<?>) cs.convert(item, cls), resolveLocale()));
        }
      }
    }
    return noContent();
  }

  @Path("/getEnumItemLiterals/{enumCls}/")
  @GET
  public Response getEnumItemLiterals(@PathParam("enumCls") String enumCls) {
    if (isNotBlank(enumCls) && enums.isResolvable()) {
      Class<?> cls = tryAsClass(enumCls);
      if (cls != null && cls.isEnum()) {
        return ok(enums.get().getEnumItemLiterals(forceCast(cls), resolveLocale()));
      }
    }
    return noContent();
  }

  @Path("/getMessage")
  @GET
  public Response getMessage(@QueryParam("msgCode") String msgCode,
      @QueryParam("args") List<String> args) {
    if (isNotBlank(msgCode) && messages.isResolvable()) {
      Object[] obj = new Object[0];
      if (!isEmpty(args)) {
        obj = args.toArray(new String[args.size()]);
      }
      return ok(messages.get().getMessage(resolveLocale(), msgCode, obj));
    }
    return noContent();
  }

  protected Locale resolveLocale() {
    if (request == null) {
      return Locale.CHINA;
    }
    return defaultObject(request.getLocale(), Locale.CHINA);
  }

}
