package com.iluwatar.urm.domain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Embedded;

/**
 * Created by moe on 10.04.16.
 */
public class DomainField {
  private Field field;

  private transient List<Annotation> fieldAnnotations;

  public DomainField(Field field) {
    this.field = field;
    this.fieldAnnotations = getFieldAnnotations(field);
  }

  /**
   * get the name of the field.
   * @return
   */
  public String getUmlName(List<String> allowedAnnotations) {
    if (field.isEnumConstant()) {
      // If this is an enum constant, we dont need the type
      return field.getName();
    }

    StringBuffer umlName = new StringBuffer(field.getName()).append(" : ").append(TypeUtils.getSimpleName(field.getGenericType()));

    List<String> annotations = new ArrayList<>();

    if (isStatic()) {
      annotations.add("static");
    }
    if (isAbstract()) {
      annotations.add("abstract");
    }

    annotations.addAll(allowedAnnotations.isEmpty() ? fieldAnnotations.stream().map(a -> a.annotationType().getSimpleName()).collect(
        Collectors.toList()) : fieldAnnotations.stream().filter(ca -> allowedAnnotations.contains(ca.annotationType().getSimpleName()) ||  allowedAnnotations.contains(ca.annotationType().getName())).map(a->a.annotationType().getSimpleName()).collect(
        Collectors.toList()));

    if (!annotations.isEmpty()) {
      umlName.append(" {").append(annotations.stream().map(ca -> String.format("%s", ca)).collect(Collectors.joining(", "))).append("}");
    }

    return umlName.toString();
  }

  public Visibility getVisibility() {
    return TypeUtils.getVisibility(field.getModifiers());
  }

  public DomainClass getType() {
    return new DomainClass(field.getType());
  }

  public boolean isStatic() {
    return Modifier.isStatic(field.getModifiers());
  }

  public boolean isAbstract() {
    return Modifier.isAbstract(field.getModifiers());
  }

  private boolean isFieldAnnotationPresent(Class<?> annotation) {
    return Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType().getSimpleName().equals(annotation.getSimpleName())).collect(
        Collectors.toList()).size() == 1;
  }

  private List<Annotation> getFieldAnnotations (Field field) {
    return List.of(field.getAnnotations());
  }
}
