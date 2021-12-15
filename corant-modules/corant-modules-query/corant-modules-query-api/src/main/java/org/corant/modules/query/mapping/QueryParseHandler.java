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
package org.corant.modules.query.mapping;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toEnum;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.strip;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Properties.Property;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.mapping.QueryHint.QueryHintParameter;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.SourceType;
import org.corant.shared.resource.URLResource;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Texts;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午4:24:51
 *
 */
public class QueryParseHandler extends DefaultHandler {

  private final String url;
  private final List<Query> queries = new ArrayList<>();
  private final List<ParameterMapping> paraMappings = new ArrayList<>();
  private String commonSegment;
  private QueryMapping mapping;

  private final Stack<Object> valueStack = new Stack<>();
  private final Stack<String> nameStack = new Stack<>();
  private final StringBuilder charStack = new StringBuilder();

  public QueryParseHandler(String url) {
    this.url = url;
    mapping = new QueryMapping();
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String cqn = currentQName();
    if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(cqn) || SchemaNames.X_DESC.equalsIgnoreCase(cqn)
        || SchemaNames.X_SCRIPT.equalsIgnoreCase(cqn)
        || SchemaNames.FQE_ELE_INJECTION_SCRIPT.equalsIgnoreCase(cqn)
        || SchemaNames.FQE_ELE_PREDICATE_SCRIPT.equalsIgnoreCase(cqn)) {
      charStack.append(ch, start, length);
    }
  }

  @Override
  public void endDocument() throws SAXException {
    mapping.setCommonSegment(commonSegment);
    mapping.paraMapping
        .putAll(paraMappings.stream().collect(Collectors.toMap(ParameterMapping::getName, p -> p)));
    mapping.queries.addAll(queries);
    mapping.assembly();// FIXME
    valueStack.clear();
    nameStack.clear();
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (SchemaNames.X_ENTRY.equalsIgnoreCase(qName)) {
      handleParamEntry(false, qName, null);
    } else if (SchemaNames.QUE_ELE.equalsIgnoreCase(qName)) {
      handleQuery(false, qName, null);
    } else if (SchemaNames.QUE_FQE_ELE.equalsIgnoreCase(qName)) {
      handleFetchQuery(false, qName, null);
    } else if (SchemaNames.X_PARAM.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof FetchQueryParameter) {
        handleFetchQueryParameter(false, qName, null);
      } else if (currentObject() instanceof QueryHintParameter) {
        handleQueryHintParameter(false, qName, null);
      }
    } else if (SchemaNames.QUE_HINT_ELE.equalsIgnoreCase(qName)) {
      handleQueryHint(false, qName, null);
    } else if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(qName)) {
      handleCommonSegment(false, qName, null);
    } else if (SchemaNames.X_DESC.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryDesc(false, qName, null);
      }
    } else if (SchemaNames.X_PROS.equalsIgnoreCase(qName)) {
      handleQueryProperties(false, qName, null);
    } else if (SchemaNames.X_PRO.equalsIgnoreCase(qName)) {
      handleProperty(false, qName, null);
    } else if (SchemaNames.X_SCRIPT.equalsIgnoreCase(qName)
        || SchemaNames.FQE_ELE_PREDICATE_SCRIPT.equalsIgnoreCase(qName)
        || SchemaNames.FQE_ELE_INJECTION_SCRIPT.equalsIgnoreCase(qName)) {
      handleScript(false, qName, null);
    }
  }

  public QueryMapping getMapping() {
    mapping.getQueries().forEach(q -> {
      q.setParamMappings(mapping.getParaMapping());
      q.postConstruct();
    });
    return mapping;
  }

  @Override
  public void startDocument() throws SAXException {
    mapping.url = url;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (SchemaNames.X_ENTRY.equalsIgnoreCase(qName)) {
      handleParamEntry(true, qName, attributes);
    } else if (SchemaNames.QUE_ELE.equalsIgnoreCase(qName)) {
      handleQuery(true, qName, attributes);
    } else if (SchemaNames.QUE_FQE_ELE.equalsIgnoreCase(qName)) {
      handleFetchQuery(true, qName, attributes);
    } else if (SchemaNames.X_PARAM.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof FetchQuery) {
        handleFetchQueryParameter(true, qName, attributes);
      } else if (currentObject() instanceof QueryHint) {
        handleQueryHintParameter(true, qName, attributes);
      }
    } else if (SchemaNames.QUE_HINT_ELE.equalsIgnoreCase(qName)) {
      handleQueryHint(true, qName, attributes);
    } else if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(qName)) {
      handleCommonSegment(true, qName, attributes);
    } else if (SchemaNames.X_DESC.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryDesc(true, qName, attributes);
      }
    } else if (SchemaNames.X_PROS.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryProperties(true, qName, attributes);
      }
    } else if (SchemaNames.X_PRO.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Properties) {
        handleProperty(true, qName, attributes);
      }
    } else if (SchemaNames.X_SCRIPT.equalsIgnoreCase(qName)
        || SchemaNames.FQE_ELE_PREDICATE_SCRIPT.equalsIgnoreCase(qName)
        || SchemaNames.FQE_ELE_INJECTION_SCRIPT.equalsIgnoreCase(qName)) {
      handleScript(true, qName, attributes);
    }
  }

  void handleCommonSegment(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String segment = charStack.toString();
      charStack.delete(0, charStack.length());
      commonSegment = segment.trim();
      nameStack.pop();
    }
  }

  void handleFetchQuery(boolean start, String qName, Attributes attributes) {
    if (start) {
      FetchQuery fq = new FetchQuery();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.FQE_ATT_REF_QUE.equalsIgnoreCase(aqn)) {
            fq.setReferenceQueryName(atv);
          } else if (SchemaNames.FQE_ATT_REF_QUE_TYP.equalsIgnoreCase(aqn)) {
            fq.setReferenceQueryType(toEnum(atv, QueryType.class));
          } else if (SchemaNames.FQE_ATT_REF_QUE_QUA.equalsIgnoreCase(aqn)) {
            fq.setReferenceQueryQualifier(atv);
          } else if (SchemaNames.FQE_ATT_MAX_SIZE.equalsIgnoreCase(aqn)) {
            fq.setMaxSize(toInteger(atv));
          } else if (SchemaNames.FQE_ATT_PRO_NAME.equalsIgnoreCase(aqn)) {
            fq.setInjectPropertyName(atv);
          } else if (SchemaNames.FQE_ATT_EAGER_INJECT_NAME.equalsIgnoreCase(aqn)) {
            fq.setEagerInject(isBlank(atv) ? true : toBoolean(atv));
          } else if (SchemaNames.FQE_ATT_VER.equalsIgnoreCase(aqn)) {
            fq.setReferenceQueryversion(defaultString(atv));
          } else if (SchemaNames.QUE_ATT_RST_CLS.equalsIgnoreCase(aqn)) {
            fq.setResultClass(isBlank(atv) ? java.util.Map.class : asClass(atv));
          } else if (SchemaNames.FQE_ATT_MULT_RECORDS.equalsIgnoreCase(aqn)) {
            fq.setMultiRecords(isBlank(atv) ? true : toBoolean(atv));
          }
        }
      }
      valueStack.push(fq);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse %s error the fetch query must be in query element!",
            url);
      }
      q.addFetchQuery((FetchQuery) obj);
      nameStack.pop();
    }
  }

  void handleFetchQueryParameter(boolean start, String qName, Attributes attributes) {
    if (start) {
      FetchQueryParameter fqp = new FetchQueryParameter();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
            fqp.setName(atv);
          } else if (SchemaNames.FQE_ELE_PARAM_ATT_SRC.equalsIgnoreCase(aqn)) {
            fqp.setSource(toEnum(atv, FetchQueryParameterSource.class));
          } else if (SchemaNames.FQE_ELE_PARAM_ATT_SRC_NME.equalsIgnoreCase(aqn)) {
            fqp.setSourceName(atv);
          } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
            fqp.setValue(atv);
          } else if (SchemaNames.X_DISTINCT.equalsIgnoreCase(aqn)) {
            fqp.setDistinct(toBoolean(atv));
          } else if (SchemaNames.X_SINGLE_AS_LIST.equalsIgnoreCase(aqn)) {
            fqp.setSingleAsList(toBoolean(atv));
          } else if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn)) {
            if (isNotBlank(atv)) {
              fqp.setType(asClass(atv));
            } else {
              fqp.setType(null);
            }
          }
        }
      }
      valueStack.push(fqp);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      FetchQuery q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException(
            "Parse %s error the fetch query parameter must be in fetch query element!", url);
      }
      q.addParameter((FetchQueryParameter) obj);
      nameStack.pop();
    }
  }

  void handleParamEntry(boolean start, String qName, Attributes attributes) {
    if (start) {
      ParameterMapping pm = new ParameterMapping();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
            pm.setName(atv);
          } else if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn)) {
            pm.setType(asClass(atv));
          }
        }
      }
      valueStack.push(pm);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      paraMappings.add((ParameterMapping) obj);
      nameStack.pop();
    }
  }

  void handleProperty(boolean start, String qName, Attributes attributes) {
    if (start) {
      Property pm = new Property();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
            pm.setName(atv);
          } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
            pm.setValue(atv);
          }
        }
      }
      valueStack.push(pm);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Properties ps = shouldNotNull(this.currentObject());
      ps.add((Property) obj);
      nameStack.pop();
    }
  }

  void handleQuery(boolean start, String qName, Attributes attributes) {
    if (start) {
      Query q = new Query(url);
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
            q.setName(atv);
          } else if (SchemaNames.QUE_ATT_CACHE.equalsIgnoreCase(aqn)) {
            q.setCache(toBoolean(atv));
          } else if (SchemaNames.QUE_ATT_CACHE_RS_MD.equalsIgnoreCase(aqn)) {
            q.setCacheResultSetMetadata(toBoolean(atv));
          } else if (SchemaNames.QUE_ATT_RST_CLS.equalsIgnoreCase(aqn)) {
            q.setResultClass(isBlank(atv) ? java.util.Map.class : asClass(atv));
          } else if (SchemaNames.QUE_ATT_RST_SET_CLS.equalsIgnoreCase(aqn)) {
            q.setResultSetMapping(isBlank(atv) ? null : asClass(atv));
          } else if (SchemaNames.QUE_ATT_VER.equalsIgnoreCase(aqn)) {
            q.setVersion(defaultString(atv));
          }
        }
      }
      valueStack.push(q);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      queries.add((Query) obj);
      nameStack.pop();
    }
  }

  void handleQueryDesc(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String desc = charStack.toString();
      charStack.delete(0, charStack.length());
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException(
            "Parse %s error the query description must be in query element!", url);
      }
      q.setDescription(desc.trim());
      nameStack.pop();
    }
  }

  void handleQueryHint(boolean start, String qName, Attributes attributes) {
    if (start) {
      QueryHint hit = new QueryHint();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_KEY.equalsIgnoreCase(aqn)) {
            hit.setKey(atv);
          }
        }
      }
      valueStack.push(hit);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse %s error the query hit must be in query element!",
            url);
      }
      q.addHint((QueryHint) obj);
      nameStack.pop();
    }
  }

  void handleQueryHintParameter(boolean start, String qName, Attributes attributes) {
    if (start) {
      QueryHintParameter qhp = new QueryHintParameter();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
            qhp.setName(atv);
          } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
            qhp.setValue(atv);
          } else if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn) && isNotBlank(atv)) {
            qhp.setType(asClass(atv));
          }
        }
      }
      valueStack.push(qhp);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      QueryHint qh = this.currentObject();
      if (qh == null) {
        throw new QueryRuntimeException(
            "Parse %s error the query hint parameter must be in query hint element!", url);
      }
      if (obj instanceof QueryHintParameter) {
        qh.addParameter((QueryHintParameter) obj);
      }
      nameStack.pop();
    }
  }

  void handleQueryProperties(boolean start, String qName, Attributes attributes) {
    if (start) {
      Properties fq = new Properties();
      valueStack.push(fq);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse %s error the fetch query must be in query element!",
            url);
      }
      ((Properties) obj).toMap().forEach(q::addProperty);
      nameStack.pop();
    }
  }

  void handleScript(boolean start, String qName, Attributes attributes) {
    if (start) {
      Script st = new Script();
      if (attributes != null) {
        ScriptType typ = null;
        String src = null;
        for (int i = 0; i < attributes.getLength(); i++) {
          String aqn = attributes.getQName(i);
          String atv = attributes.getValue(i);
          if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn)) {
            typ = toObject(atv, ScriptType.class);
          } else if (SchemaNames.X_SRC.equalsIgnoreCase(aqn)) {
            src = atv;
          }
        }
        st.setType(
            typ == null ? this.currentObject() instanceof Query ? ScriptType.FM : ScriptType.JS
                : typ);
        st.setSrc(src);
      }
      valueStack.push(st);
      nameStack.push(qName);
    } else {
      Script obj = forceCast(valueStack.pop());
      String scriptCode;
      if (isNotBlank(obj.getSrc())) {
        scriptCode = resolveScript(obj.getSrc());
      } else {
        scriptCode = charStack.toString();
      }
      if (isBlank(scriptCode)) {
        throw new QueryRuntimeException("Parse %s error the script code can't null!", url);
      }
      charStack.delete(0, charStack.length());
      obj.setCode(scriptCode);
      if (SchemaNames.X_SCRIPT.equalsIgnoreCase(qName)) {
        if (this.currentObject() instanceof Query) {
          Query q = this.currentObject();
          if (q == null || !obj.isValid()) {
            throw new QueryRuntimeException(
                "Parse %s error the query script must be in query element and script can't null!",
                url);
          }
          q.setScript(obj);
        } else if (this.currentObject() instanceof QueryHint) {
          QueryHint q = this.currentObject();
          if (q == null || !obj.isValid()) {
            throw new QueryRuntimeException(
                "Parse %s error the query hit script must be in query element and script can't null!",
                url);
          }
          q.setScript(obj);
        } else if (this.currentObject() instanceof FetchQueryParameter) {
          FetchQueryParameter p = this.currentObject();
          if (p == null || p.getSource() != FetchQueryParameterSource.S) {
            throw new QueryRuntimeException(
                "Parse %s error the fetch query parameter script must be in parameter element and the source must be 'S'!",
                url);
          }
          p.setScript(obj);
        }
      } else if (SchemaNames.FQE_ELE_PREDICATE_SCRIPT.equalsIgnoreCase(qName)) {
        if (this.currentObject() instanceof FetchQuery) {
          FetchQuery q = this.currentObject();
          if (q == null || !obj.isValid()) {
            throw new QueryRuntimeException(
                "Parse %s error the fetch query predicate script must be in predicate-script element and script can't null!",
                url);
          }
          q.setPredicateScript(obj);
        }
      } else if (SchemaNames.FQE_ELE_INJECTION_SCRIPT.equalsIgnoreCase(qName)
          && this.currentObject() instanceof FetchQuery) {
        FetchQuery q = this.currentObject();
        if (q == null || !obj.isValid()) {
          throw new QueryRuntimeException(
              "Parse %s error the fetch query injection script must be in predicate-script element and script can't null!",
              url);
        }
        q.setInjectionScript(obj);
      }

      nameStack.pop();
    }
  }

  String resolveScript(String src) {
    Optional<SourceType> st = SourceType.decide(strip(src));
    if (st.isPresent()) {
      try {
        Optional<URLResource> or = Resources.from(strip(src)).findFirst();
        if (or.isPresent()) {
          try (InputStream is = or.get().openInputStream()) {
            return Texts.fromInputStream(is);
          }
        }
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private <T> T currentObject() {
    return valueStack.isEmpty() ? null : (T) valueStack.peek();
  }

  private String currentQName() {
    return nameStack.isEmpty() ? null : nameStack.peek();
  }
}
