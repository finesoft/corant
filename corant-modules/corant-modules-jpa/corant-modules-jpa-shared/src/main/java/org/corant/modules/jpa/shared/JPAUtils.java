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
package org.corant.modules.jpa.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getAllSuperclassesAndInterfaces;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.replace;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceUnit;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.PersistenceNames;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.resource.ClassResource;
import org.corant.shared.util.Resources;

/**
 *
 * corant-modules-jpa-shared
 *
 * @author bingo 下午6:27:53
 *
 */
public class JPAUtils {

  static final Set<Class<? extends Annotation>> PERSIS_ANN =
      setOf(Entity.class, Embeddable.class, MappedSuperclass.class, Converter.class);

  public static String getMixedPuName(PersistenceUnit pu) {
    String usePuName = defaultString(pu.unitName(), PersistenceNames.PU_DFLT_NME);
    return isEmpty(pu.name()) ? usePuName : usePuName + "." + pu.name();
  }

  public static Set<Class<?>> getPersistenceClasses(String packages) {
    Set<Class<?>> clses = new LinkedHashSet<>();
    try {
      Resources.fromClassPath(replace(packages, ".", "/")).filter(c -> c instanceof ClassResource)
          .map(c -> (ClassResource) c).map(ClassResource::load).filter(JPAUtils::isPersistenceClass)
          .forEach(clses::add);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return clses;
  }

  public static Set<String> getPersistenceMappingFiles(String... pathExpressions) {
    Set<String> paths = new LinkedHashSet<>();
    try {
      for (String pathExpression : pathExpressions) {
        Resources.fromClassPath(pathExpression).filter(r -> !(r instanceof ClassResource))
            .map(ClassPathResource::getClassPath).forEach(paths::add);
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return paths;
  }

  public static boolean isPersistenceClass(Class<?> cls) {
    return cls != null && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())
        && (PERSIS_ANN.stream().anyMatch(pn -> cls.isAnnotationPresent(pn))
            || getAllSuperclassesAndInterfaces(cls).stream()
                .anyMatch(c -> PERSIS_ANN.stream().anyMatch(pn -> c.isAnnotationPresent(pn))));
  }

  public static boolean isPersistenceClass(String clsName) {
    return isPersistenceClass(tryAsClass(clsName));
  }

  public static boolean isPersistenceEntityClass(Class<?> cls) {
    return cls != null && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())
        && (cls.isAnnotationPresent(Entity.class) || getAllSuperclassesAndInterfaces(cls).stream()
            .anyMatch(c -> c.isAnnotationPresent(Entity.class)));
  }

  public static boolean isPersistenceEntityClass(String clsName) {
    return isPersistenceEntityClass(tryAsClass(clsName));
  }

  public static void stdoutPersistClasses(String pkg, PrintStream ps) throws IOException {
    String path = shouldNotNull(pkg).replace('.', '/');
    Resources.fromClassPath(path).filter(c -> c instanceof ClassResource)
        .map(c -> (ClassResource) c).map(ClassResource::load).filter(JPAUtils::isPersistenceClass)
        .map(Class::getName).sorted(String::compareTo)
        .map(x -> new StringBuilder("<class>").append(x).append("</class>").toString())
        .forEach(ps::println);
  }

  public static void stdoutPersistes(String pkg, PrintStream ps) throws IOException {
    ps.println("<!-- mapping files -->");
    stdoutPersistJpaOrmXml(pkg, ps);
    ps.println("<!-- mapping classes -->");
    stdoutPersistClasses(pkg, ps);
  }

  public static void stdoutPersistJpaOrmXml(String pkg, PrintStream ps) throws IOException {
    String path = shouldNotNull(pkg).replace('.', '/');
    Resources.fromClassPath(path).filter(r -> r.getClassPath().endsWith("JpaOrm.xml"))
        .map(ClassPathResource::getClassPath).map(s -> new StringBuilder().append("<mapping-file>")
            .append(s.substring(s.indexOf(path))).append("</mapping-file>"))
        .forEach(ps::println);
  }
}
