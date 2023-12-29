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
package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

/**
 * corant-modules-elastic-plugin
 *
 * @author bingo 下午11:20:02
 */
public class XEnNGramTokenFilterFactory extends AbstractTokenFilterFactory {

  private final int enMinGram;
  private final int enMaxGram;
  private final boolean retainToken;

  /**
   *
   * @param indexSettings
   * @param env
   * @param name
   * @param settings
   */
  public XEnNGramTokenFilterFactory(IndexSettings indexSettings, Environment env, String name,
      Settings settings) {
    super(indexSettings, name, settings);
    enMinGram = settings.getAsInt("min_gram", XEnNGramTokenFilter.MIN_NGRAM_SIZE);
    enMaxGram = settings.getAsInt("max_gram", XEnNGramTokenFilter.MAX_NGRAM_SIZE);
    retainToken = settings.getAsBoolean("retain_token", XEnNGramTokenFilter.RETAIN_TOKEN);
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    return new XEnNGramTokenFilter(tokenStream, enMinGram, enMaxGram, retainToken);
  }

}
