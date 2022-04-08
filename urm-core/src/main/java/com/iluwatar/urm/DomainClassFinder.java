package com.iluwatar.urm;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class DomainClassFinder {

  private static final Logger logger = Logger.getLogger(DomainClassFinder.class.getName());

  private static final String URM_PACKAGE = "com.iluwatar.urm";
  public static boolean ALLOW_FINDING_INTERNAL_CLASSES;

  public static ClassLoader[] classLoaders;

  private Reflections reflections;

  public DomainClassFinder(Reflections reflections) {
    this.reflections = reflections;
  }

  /**
   * method to find and filter classes using reflections.
   * @param packages list of packages
   * @param ignores list of ignores
   * @param classLoader URL classloader object
   * @return list of classes
   */
  public List<Class<?>> findClasses(final List<String> packages, List<String> ignores,
                                           final URLClassLoader classLoader) {
    System.out.println("packages = " + packages);
    System.out.println("ignores = " + ignores);
    System.out.println("classLoader = " + Arrays.toString(classLoader.getDefinedPackages()));
    List<Class<?>> classList = packages.stream()
        .map(packageName -> getClasses(packageName))
        .flatMap(Collection::stream)
        .filter(DomainClassFinder::isNotPackageInfo)
        .filter(DomainClassFinder::isNotAnonymousClass)
        .filter((Class<?> clazz) -> ignores.stream()
            .noneMatch(ignore -> clazz.getName().matches(ignore) || clazz.getSimpleName().matches(ignore)))
        .sorted(Comparator.comparing(Class::getName))
        .collect(Collectors.toList());

    System.out.println("classList = " + classList);
    return classList;
  }

  private static boolean isNotPackageInfo(Class<?> clazz) {
    return !clazz.getSimpleName().equals("package-info");
  }

  private static boolean isNotAnonymousClass(Class<?> clazz) {
    return !clazz.getSimpleName().equals("");
  }

  private Set<Class<?>> getClasses(String packageName) {
    FilterBuilder filter = new FilterBuilder().includePackage(packageName);
    if (!isAllowFindingInternalClasses()) {
      filter.excludePackage(URM_PACKAGE);
    }


    SetView<Class<?>> classes = Sets.union(reflections.getSubTypesOf(Object.class),
        reflections.getSubTypesOf(Enum.class));

    classes.forEach(System.out::println);
    return classes;
  }

  public boolean isAllowFindingInternalClasses() {
    return ALLOW_FINDING_INTERNAL_CLASSES |= Boolean.parseBoolean(
        System.getProperty("DomainClassFinder.allowFindingInternalClasses", "false"));
  }
}
