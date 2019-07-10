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
package org.corant.suites.lang.kotlin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.kernel.service.CompilationService;
import org.corant.shared.exception.CorantRuntimeException;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

/**
 * corant-suites-lang-kotlin
 *
 * @author bingo 下午2:33:02
 *
 */
public class KotlinCompilationService implements CompilationService {

  @Override
  public Set<String> acceptExtensions() {
    return Collections.singleton(".kt");
  }

  @Override
  public void compile(Context context) {
    K2JVMCompilerArguments cas = new K2JVMCompilerArguments();
    cas.setClasspath(context.getClasspaths().stream().map(File::getAbsolutePath)
        .collect(Collectors.joining(File.pathSeparator)));
    cas.setDestination(context.getOutputDirectory().getAbsolutePath());
    cas.setFreeArgs(context.getSourceDirectories().stream().map(File::getAbsolutePath)
        .collect(Collectors.toList()));
    cas.setSuppressWarnings(true);
    final SimpleMessageCollector smc = new SimpleMessageCollector();
    ExitCode exitCode = new K2JVMCompiler().exec(smc, new Services.Builder().build(), cas);
    if (exitCode != ExitCode.OK && exitCode != ExitCode.COMPILATION_ERROR) {
      throw new CorantRuntimeException("Unable to invoke Kotlin compiler");
    }
    if (smc.hasErrors()) {
      throw new CorantRuntimeException("Compilation failed" + String.join("\n", smc.errors));
    }
  }

  public static class SimpleMessageCollector implements MessageCollector {

    final List<String> errors = new ArrayList<>();

    @Override
    public void clear() {}

    @Override
    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    @Override
    public void report(CompilerMessageSeverity severity, String error,
        CompilerMessageLocation location) {
      if (severity.isError()) {
        if (location != null && location.getLineContent() != null) {
          errors.add(String.format("%sn%s:%d:%d", location.getLineContent(), location.getPath(),
              location.getLine(), location.getColumn()));
        } else {
          errors.add(error);
        }
      }
    }
  }
}
