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

import static org.corant.shared.util.Empties.isNotEmpty;
import java.io.StringWriter;
import java.util.Map.Entry;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;
import org.corant.modules.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.modules.query.shared.dynamic.freemarker.FreemarkerExecutions;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Tuple.Pair;
import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:55:32
 */
@Experimental
public class FreemarkerJaxrsQuerierBuilder extends AbstractNamedQuerierBuilder<JaxrsNamedQuerier> {

  protected final Client client;
  protected final JaxrsNamedQueryClientConfig clientConfig;
  protected final Template execution;
  protected final String originalScript;

  protected FreemarkerJaxrsQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler, Client client,
      JaxrsNamedQueryClientConfig clientConfig) {
    super(query, queryHandler, fetchQueryHandler);
    this.client = client;
    this.clientConfig = clientConfig;
    Pair<Template, String> tns = FreemarkerExecutions.resolveExecution(query);
    execution = tns.left();
    originalScript = tns.right();
  }

  @Override
  public DefaultJaxrsNamedQuerier build(QueryParameter queryParameter) {
    WebTarget target = client.target(clientConfig.getRoot());
    JaxrsQueryParameter parameter = resolveJaxrsQueryParameter(queryParameter);
    parameter.postConstruct();
    return new DefaultJaxrsNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), clientConfig, target, parameter);
  }

  protected JaxrsQueryParameter resolveJaxrsQueryParameter(QueryParameter param) {
    try (StringWriter sw = new StringWriter()) {
      Environment e = execution.createProcessingEnvironment(param, sw);
      // Inject configuration retrieve template method model
      DynamicTemplateMethodModelEx<Object> cmm = DynamicTemplateMethodModelEx.CONFIG_TMM_INST;
      String cmmTyp = cmm.getType();
      checkVarNames(e, cmmTyp);
      e.setVariable(cmmTyp, cmm);
      // Inject query context
      ObjectWrapper ow = execution.getObjectWrapper();
      if (isNotEmpty(param.getContext())) {
        for (Entry<String, Object> ctx : param.getContext().entrySet()) {
          checkVarNames(e, ctx.getKey());
          TemplateModel val = ctx.getValue() == null ? new SimpleHash(ow) : ow.wrap(ctx.getValue());
          e.setVariable(ctx.getKey(), val);
        }
      }
      e.process();
      return getQueryHandler().getObjectMapper().fromJsonString(sw.toString(),
          JaxrsQueryParameter.class);
    } catch (TemplateException te) {
      throw new QueryRuntimeException(te,
          "Freemarker dynamic querier builder [%s] execute occurred error! %s",
          getQuery().getVersionedName(),
          FreemarkerExecutions.resolveScriptExceptionInfo(originalScript, te));
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "Freemarker dynamic querier builder [%s] execute occurred error!", getQuery().getName());
    }
  }

  void checkVarNames(Environment e, String... varNames) throws TemplateModelException {
    for (String varName : varNames) {
      if (e.getKnownVariableNames().contains(varName)) {
        throw new QueryRuntimeException(
            "Freemarker dynamic querier buildr [%s] error, the key [%s] name conflict.",
            query.getVersionedName(), varName);
      }
    }
  }
}
