package org.corant.modules.lang.javascript;

import javax.script.ScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

/**
 * <br>
 *
 * @author sushuaihao 2021/6/10
 * @since 1.7.0
 */
public class GraalJSScriptEngines {
  public static ScriptEngine createEngine() {
    return GraalJSScriptEngine.create(null,
        Context.newBuilder("js").allowAllAccess(true).allowExperimentalOptions(true)
            .allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true)
            .option("js.nashorn-compat", "true"));
  }
}
