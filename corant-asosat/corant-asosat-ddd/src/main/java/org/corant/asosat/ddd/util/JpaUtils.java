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
package org.corant.asosat.ddd.util;

import static org.corant.shared.util.ClassUtils.getAllSuperclassesAndInterfaces;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Set;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import org.corant.shared.util.ClassPaths;
import org.corant.shared.util.ClassPaths.ClassInfo;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午9:59:56
 *
 */
public class JpaUtils {

  static final Set<Class<? extends Annotation>> PERSIS_ANN =
      asSet(Entity.class, Embeddable.class, MappedSuperclass.class, Converter.class);

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
    String path = shouldNotNull(pkg).replaceAll("\\.", "/");
    ClassPaths.from(path).getClasses().map(ClassInfo::load).filter(JpaUtils::isPersistenceClass)
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
    String path = shouldNotNull(pkg).replaceAll("\\.", "/");
    ClassPaths.from(path).getResources().filter(r -> r.getResourceName().endsWith("JpaOrm.xml"))
        .map(r -> r.getResourceName()).map(s -> new StringBuilder().append("<mapping-file>")
            .append(s.substring(s.indexOf(path))).append("</mapping-file>"))
        .forEach(ps::println);
  }
}
