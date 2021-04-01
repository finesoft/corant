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
package org.elasticsearch.plugin;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.MixedZhEnNGramTokenFilterFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.XEnNGramTokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

/**
 * corant-modules-elastic-plugin
 *
 * @author bingo 下午11:21:10
 *
 */
public class MixedZhEnNGramTokenFilterPlugin extends Plugin implements AnalysisPlugin {

  public static final String MIXED_NAME = "mixed_zh_en_ngram_tf";
  public static final String XEN_NAME = "x_en_ngram_tf";

  @Override
  public Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
    Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> ext = new LinkedHashMap<>();
    if (AnalysisPlugin.super.getTokenFilters() != null) {
      ext.putAll(AnalysisPlugin.super.getTokenFilters());
    }
    ext.put(MIXED_NAME, new AnalysisModule.AnalysisProvider<TokenFilterFactory>() {
      @Override
      public MixedZhEnNGramTokenFilterFactory get(IndexSettings indexSettings,
          Environment environment, String name, Settings settings) throws IOException {
        return new MixedZhEnNGramTokenFilterFactory(indexSettings, environment, name, settings);
      }

      @Override
      public boolean requiresAnalysisSettings() {
        return true;
      }
    });
    ext.put(XEN_NAME, new AnalysisModule.AnalysisProvider<TokenFilterFactory>() {
      @Override
      public XEnNGramTokenFilterFactory get(IndexSettings indexSettings, Environment environment,
          String name, Settings settings) throws IOException {
        return new XEnNGramTokenFilterFactory(indexSettings, environment, name, settings);
      }

      @Override
      public boolean requiresAnalysisSettings() {
        return true;
      }
    });
    return ext;
  }

}
