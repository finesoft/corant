/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.dsa.structure;

import static org.corant.shared.util.Strings.defaultString;
import java.util.Iterator;
import org.corant.shared.util.Strings;

/**
 * corant-modules-dsa
 *
 * @author bingo 下午5:47:44
 */
public interface Ngrams extends Iterator<String> {

  public static class Ngram implements Ngrams {

    final String[] terms;
    final String splitor;
    final int n;
    int pos = 0;

    public Ngram(int n, String splitor, String[] terms) {
      this.n = n;
      this.splitor = defaultString(splitor, Strings.SPACE);
      this.terms = terms;
    }

    public Ngram(int n, String[] terms) {
      this(n, null, terms);
    }

    @Override
    public boolean hasNext() {
      return pos < terms.length - n + 1;
    }

    @Override
    public String next() {
      StringBuilder sb = new StringBuilder();
      for (int i = pos; i < pos + n; i++) {
        sb.append((i > pos ? splitor : "") + terms[i]);
      }
      pos++;
      return sb.toString();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
