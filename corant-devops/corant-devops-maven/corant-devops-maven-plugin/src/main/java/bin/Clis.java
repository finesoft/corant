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
package bin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * corant-devops-maven-plugin
 *
 * TODO
 *
 * @author bingo 下午1:01:36
 *
 */
public class Clis {

  static final Map<String, CommandLine> CLIS = new HashMap<>();

  static {
    CLIS.put("!cwd", (s) -> {
    });
  }

  public interface CommandLine extends Consumer<String> {

  }

}
