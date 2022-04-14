package com.iluwatar.urm.domain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by moe on 09.04.16.
 */
public class DomainClass {

  private static final Logger log = LoggerFactory.getLogger(DomainClass.class);
  protected static final List<String> IGNORED_METHODS = Arrays.asList("$jacocoInit");
  protected static final List<String> IGNORED_FIELDS = Arrays.asList("$jacocoData");

  private Class<?> clazz;
  private String description;
  private transient List<DomainField> fieldList;
  private transient List<Annotation> classAnnotations;
  private transient List<DomainConstructor> constructorList;
  private transient List<DomainMethod> methodList;

  public DomainClass(Class<?> clazz, String description) {
    this.clazz = clazz;
    this.description = description;
    this.classAnnotations = getClassAnnotation(clazz);
  }

  public DomainClass(Class<?> clazz) {
    this(clazz, null);
  }

  private boolean isLambda(String s) {
    return s.contains("lambda$");
  }

  public String getPackageName() {
    return clazz.getPackage().getName();
  }

  public String getUmlName(List<String> allowedAnnotations) {

    List<Annotation> annotations = allowedAnnotations.isEmpty() ? classAnnotations : classAnnotations.stream().filter(ca -> allowedAnnotations.contains(ca.annotationType().getSimpleName()) ||  allowedAnnotations.contains(ca.annotationType().getName())).collect(
        Collectors.toList());

    StringBuffer umlName = new StringBuffer(TypeUtils.getSimpleName(clazz));

    umlName.append(annotations.stream().map(ca -> String.format(" << %s >>", ca.annotationType().getSimpleName())).collect(Collectors.joining(",")));

    return umlName.toString();
  }

  public String getClassName() {
    return clazz.getSimpleName();
  }

  public String getDescription() {
    return description;
  }

  /**
   * method to get declared fields of the class.
   * @return
   */
  public List<DomainField> getFields(List<String> allowedAnnotations) {
    if (fieldList == null) {
      fieldList = Arrays.stream(clazz.getDeclaredFields())
          .filter(f -> !(f.getDeclaringClass().isEnum() && f.getName().equals("$VALUES")))
          .filter(f -> !f.isSynthetic())
          .filter(f -> !IGNORED_FIELDS.contains(f.getName()))
          .map(DomainField::new)
          .sorted(Comparator.comparing(df -> df.getUmlName(allowedAnnotations)))
          .collect(Collectors.toList());
    }
    return fieldList;
  }

  /**
   * method to get declared constructors of the class.
   * @return
   */
  public List<DomainConstructor> getConstructors() {
    if (constructorList == null) {
      if (clazz.isEnum()) {
        // Enums only have the Native Constructor...
        constructorList = Collections.emptyList();
      } else {
        constructorList = Arrays.stream(clazz.getDeclaredConstructors())
            .filter(c -> !c.isSynthetic())
            .map(DomainConstructor::new)
            .sorted(Comparator.comparing(DomainConstructor::getUmlName))
            .collect(Collectors.toList());
      }
    }
    return constructorList;
  }

  /**
   * method to get declared methods of the class.
   * @return
   */
  public List<DomainMethod> getMethods() {
    if (methodList == null) {
      methodList = Arrays.stream(clazz.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .map(DomainMethod::new)
          .filter(m -> !IGNORED_METHODS.contains(m.getName()) && !isLambda(m.getName()))
          .sorted(Comparator.comparing(DomainExecutable::getUmlName))
          .collect(Collectors.toList());
    }
    return methodList;
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public Visibility getVisibility() {
    return TypeUtils.getVisibility(clazz.getModifiers());
  }

  public boolean isEmbeddable () {
    return isClassAnnotationPresent(Embeddable.class);
  }

  /**
   * method to get classtype of the class.
   * @return
   */
  public DomainClassType getClassType() {
    if (clazz.isInterface()) {
      return DomainClassType.INTERFACE;
    } else if (clazz.isEnum()) {
      return DomainClassType.ENUM;
    } else if (clazz.isAnnotation()) {
      return DomainClassType.ANNOTATION;
    } else {
      return DomainClassType.CLASS;
    }


  }

  public boolean isAbstract() {
    return Modifier.isAbstract(clazz.getModifiers());
  }

  private List<Annotation> getClassAnnotation (Class<?> clazz) {
    return List.of(clazz.getAnnotations());
  }

  private boolean isClassAnnotationPresent(Class<?> annotation) {
    return Arrays.stream(clazz.getAnnotations()).filter(a -> a.annotationType().getSimpleName().equals(annotation.getSimpleName())).collect(
        Collectors.toList()).size() == 1;
  }
}
