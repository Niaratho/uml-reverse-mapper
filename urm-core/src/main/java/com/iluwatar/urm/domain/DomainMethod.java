package com.iluwatar.urm.domain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by moe on 26.04.16.
 */
public class DomainMethod extends DomainExecutable<Method> {

  private static final String VOID_TYPE_NAME = Void.TYPE.getSimpleName();

  private transient List<Annotation> methodAnnotations;

  public DomainMethod(Method method) {
    super(method);
    this.methodAnnotations = getClassAnnotations(method);
  }

  @Override
  public String getUmlName() {
    // Executable by itself has no return type, so add it here
    Type returnType = getExecutable().getGenericReturnType();
    if (Void.class.equals(returnType) || Void.TYPE.equals(returnType)) {
      // But if it returns nothing (aka void) we done output any type
      return super.getUmlName();
    }

    return super.getUmlName() + " : " + TypeUtils.getSimpleName(returnType);
  }

  private List<Annotation> getClassAnnotations (Method method) {
    return List.of(method.getAnnotations());
  }
}
