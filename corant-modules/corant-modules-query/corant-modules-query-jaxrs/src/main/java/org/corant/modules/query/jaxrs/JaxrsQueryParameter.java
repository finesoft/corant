/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import static java.util.Collections.singletonList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyPredicate;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.beans.Transient;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.MediaType;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:13:50
 */
public class JaxrsQueryParameter {

  public static final List<MediaType> DEFAULT_MEDIA_TYPES =
      singletonList(MediaType.APPLICATION_JSON_TYPE);
  public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;
  public static final String DEFAULT_HTTP_METHOD = "POST";

  protected String path;

  protected Map<String, Object> templateVariables;
  protected Map<String, Object> encodeSlashTemplateVariables;
  protected Map<String, Object> formEncodedTemplateVariables;
  protected Map<String, Object> queryParameters;
  protected Map<String, Object> matrixParameters;

  protected String httpMethod = DEFAULT_HTTP_METHOD;
  protected String propagateHeaderNames;
  protected List<MediaType> requestMediaTypes = DEFAULT_MEDIA_TYPES;
  protected List<MediaType> acceptMediaTypes = DEFAULT_MEDIA_TYPES;

  protected boolean onlyUseEmptyMapAsParameter;
  protected boolean onlyUsePathParameters;

  protected MediaType entityMediaType = DEFAULT_MEDIA_TYPE;
  protected Object entity;

  protected transient MediaType[] requestMediaTypeArray = {};
  protected transient MediaType[] acceptMediaTypeArray = {};
  protected transient Predicate<String> propagateHeaderNameFilter = null;

  public static Predicate<String> parsePropagateHeaderNameFilter(
      String propagateHeaderNameWildcards) {
    if (isNotBlank(propagateHeaderNameWildcards)) {
      String[] pps = split(propagateHeaderNameWildcards, ",", true, true);
      if (isNotEmpty(pps)) {
        Predicate<String> propagateHeaderNameFilter = emptyPredicate(false);
        for (String s : pps) {
          if (WildcardMatcher.hasWildcard(s)) {
            WildcardMatcher wm = WildcardMatcher.of(false, s);
            propagateHeaderNameFilter = propagateHeaderNameFilter.or(wm);
          } else {
            propagateHeaderNameFilter = propagateHeaderNameFilter.or(s::equalsIgnoreCase);
          }
        }
        return propagateHeaderNameFilter;
      }
    }
    return null;
  }

  public boolean containsTempleOrParameter() {
    return isNotEmpty(templateVariables) || isNotEmpty(encodeSlashTemplateVariables)
        || isNotEmpty(formEncodedTemplateVariables) || isNotEmpty(queryParameters)
        || isNotEmpty(matrixParameters);
  }

  @Transient
  public MediaType[] getAcceptMediaTypeArray() {
    return acceptMediaTypeArray;
  }

  public List<MediaType> getAcceptMediaTypes() {
    return isEmpty(acceptMediaTypes) ? DEFAULT_MEDIA_TYPES : acceptMediaTypes;
  }

  public Map<String, Object> getEncodeSlashTemplateVariables() {
    return encodeSlashTemplateVariables;
  }

  public Object getEntity() {
    return entity;
  }

  public MediaType getEntityMediaType() {
    return defaultObject(entityMediaType, DEFAULT_MEDIA_TYPE);
  }

  public Map<String, Object> getFormEncodedTemplateVariables() {
    return formEncodedTemplateVariables;
  }

  public String getHttpMethod() {
    return defaultBlank(httpMethod, DEFAULT_HTTP_METHOD);
  }

  public Map<String, Object> getMatrixParameters() {
    return matrixParameters;
  }

  public String getPath() {
    return path;
  }

  @Transient
  public Predicate<String> getPropagateHeaderNameFilter() {
    return propagateHeaderNameFilter;
  }

  public String getPropagateHeaderNames() {
    return propagateHeaderNames;
  }

  public Map<String, Object> getQueryParameters() {
    return queryParameters;
  }

  @Transient
  public MediaType[] getRequestMediaTypeArray() {
    return requestMediaTypeArray;
  }

  public List<MediaType> getRequestMediaTypes() {
    return isEmpty(requestMediaTypes) ? DEFAULT_MEDIA_TYPES : requestMediaTypes;
  }

  public Map<String, Object> getTemplateVariables() {
    return templateVariables;
  }

  public boolean isOnlyUseEmptyMapAsParameter() {
    return onlyUseEmptyMapAsParameter;
  }

  public boolean isOnlyUsePathParameters() {
    return onlyUsePathParameters;
  }

  @PostConstruct
  protected void postConstruct() {
    requestMediaTypeArray = getRequestMediaTypes().toArray(MediaType[]::new);
    acceptMediaTypeArray = getAcceptMediaTypes().toArray(MediaType[]::new);
    propagateHeaderNameFilter = parsePropagateHeaderNameFilter(propagateHeaderNames);
  }
}
