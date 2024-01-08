/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.validation;

import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.GroupSequence;
import jakarta.validation.Validator;
import org.corant.context.command.CommandValidator;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.util.Classes;

/**
 * corant-modules-validation
 *
 * <p>
 * The default command validator is used for command validation, validation processing uses
 * {@link Validator}, where the group or list of groups used for validation is extracted from the
 * command class {@link GroupSequence} annotation.
 *
 * @author bingo 下午9:59:38
 */
@Experimental
@ApplicationScoped
@RequiredConfiguration(key = "corant.validation.default-command-validator.enable",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
public class DefaultCommandValidator implements CommandValidator {

  protected Map<Class<?>, Class<?>[]> groups = new ConcurrentHashMap<>();

  @Inject
  protected Validator validator;

  @Override
  public void validate(Object command) {
    Set<ConstraintViolation<Object>> violations =
        validator.validate(command, resolveGroup(command));
    if (isNotEmpty(violations)) {
      throw new ConstraintViolationException(violations);
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    groups.clear();
  }

  protected Class<?>[] resolveGroup(Object command) {
    if (command != null) {
      return groups.computeIfAbsent(getUserClass(command), k -> {
        GroupSequence annotation = k.getAnnotation(GroupSequence.class);
        if (annotation != null) {
          return annotation.value();
        }
        return Classes.EMPTY_ARRAY;
      });
    }
    return Classes.EMPTY_ARRAY;
  }
}
