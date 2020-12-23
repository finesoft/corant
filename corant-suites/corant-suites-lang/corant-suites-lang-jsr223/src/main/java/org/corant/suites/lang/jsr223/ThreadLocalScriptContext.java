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
package org.corant.suites.lang.jsr223;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

/**
 * corant-suites-lang-jsr223
 *
 * @author bingo 下午4:05:32
 *
 */
public class ThreadLocalScriptContext implements ScriptContext {

  static ThreadLocal<ScriptContext> CTXS = ThreadLocal.withInitial(SimpleScriptContext::new);

  public ThreadLocalScriptContext() {
    if (CTXS.get() == null) {
      CTXS.set(new SimpleScriptContext());
    }
  }

  public void destroy() {
    CTXS.remove();
  }

  @Override
  public Object getAttribute(String name) {
    return CTXS.get().getAttribute(name);
  }

  @Override
  public Object getAttribute(String name, int scope) {
    return CTXS.get().getAttribute(name, scope);
  }

  @Override
  public int getAttributesScope(String name) {
    return CTXS.get().getAttributesScope(name);
  }

  @Override
  public Bindings getBindings(int scope) {
    return CTXS.get().getBindings(scope);
  }

  @Override
  public Writer getErrorWriter() {
    return CTXS.get().getErrorWriter();
  }

  @Override
  public Reader getReader() {
    return CTXS.get().getReader();
  }

  @Override
  public List<Integer> getScopes() {
    return CTXS.get().getScopes();
  }

  @Override
  public Writer getWriter() {
    return CTXS.get().getWriter();
  }

  @Override
  public Object removeAttribute(String name, int scope) {
    return CTXS.get().removeAttribute(name, scope);
  }

  @Override
  public void setAttribute(String name, Object value, int scope) {
    CTXS.get().setAttribute(name, value, scope);
  }

  @Override
  public void setBindings(Bindings bindings, int scope) {
    CTXS.get().setBindings(bindings, scope);
  }

  @Override
  public void setErrorWriter(Writer writer) {
    CTXS.get().setErrorWriter(writer);
  }

  @Override
  public void setReader(Reader reader) {
    CTXS.get().setReader(reader);
  }

  @Override
  public void setWriter(Writer writer) {
    CTXS.get().setWriter(writer);
  }

}
