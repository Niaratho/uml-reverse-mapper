package com.iluwatar.urm;

import com.iluwatar.urm.domain.DomainClass;
import com.iluwatar.urm.domain.Edge;
import com.iluwatar.urm.presenters.Presenter;
import com.iluwatar.urm.presenters.Representation;
import com.iluwatar.urm.scanners.FieldScanner;
import com.iluwatar.urm.scanners.HierarchyScanner;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainMapper {

  private final FieldScanner fieldScanner;
  private final HierarchyScanner hierarchyScanner;
  private List<Class<?>> classes;
  private final Presenter presenter;
  public ClassLoader[] classLoaders;
  public static Reflections reflections;

  /**
   * method to get representation.
   *
   * @return Representation type
   * @throws ClassNotFoundException exception
   */
  public Representation describeDomain() throws ClassNotFoundException {
    List<Edge> edges = new ArrayList<>();
    edges.addAll(fieldScanner.getEdges());
    edges.addAll(hierarchyScanner.getEdges());
    edges=mergeBuilderAndClass(edges);
    List<DomainClass> domainObjects = classes.stream().map(DomainClass::new)
        .collect(Collectors.toList());
    return presenter.describe(domainObjects, edges);
  }

  private List<Edge> mergeBuilderAndClass(List<Edge> edges) {
    List<Class<?>> listOfBuilders = classes.stream()
        .filter(c -> c.getSimpleName().endsWith("Builder"))
        .collect(Collectors.toList());

    Set<Edge> newEdges= new HashSet<>();

    for (Class<?> builder : listOfBuilders) {
      Class<?> clazz = findClass(builder);
      if (clazz != null) {
        newEdges.clear();
        for (Edge e :edges){
          boolean sourcebuilder=e.source.getClassName().equals(builder.getSimpleName());
          boolean targetbuilder=e.target.getClassName().equals(builder.getSimpleName());
          boolean sourcenormal=e.source.getClassName().equals(clazz.getSimpleName());
          boolean targetnormal=e.target.getClassName().equals(clazz.getSimpleName());
          if ((sourcebuilder && targetnormal )||(targetbuilder && sourcenormal)){
            continue;
          }else if (sourcebuilder){
            newEdges.add(new Edge(new DomainClass(clazz,e.source.getDescription()),e.target,e.type,e.direction));
          }else if( targetbuilder){
            newEdges.add(new Edge(e.source,new DomainClass(clazz,e.source.getDescription()),e.type,e.direction));
          }else{
            newEdges.add(e);
          }
        }
        classes.remove(builder);
        edges= newEdges.stream().collect(Collectors.toList());
      }
    }
    return edges;
  }



  private Class<?> findClass(Class<?> builder) {
    for (Class<?> clazz : classes) {
      if (String.format("%sBuilder", clazz.getSimpleName()).equals(builder.getSimpleName())) {
        return clazz;
      }
    }
    return null;
  }


  public DomainMapper(Presenter presenter, List<String> packages,
      List<String> ignores, URLClassLoader classLoader)
      throws ClassNotFoundException {
    this.presenter = presenter;
    List<ClassLoader> classLoadersList = new LinkedList<>();
    classLoadersList.add(ClasspathHelper.contextClassLoader());
    classLoadersList.add(ClasspathHelper.staticClassLoader());
    if (classLoader != null) {
      classLoadersList.add(classLoader);
    }
    classLoaders = classLoadersList.toArray(new ClassLoader[0]);

    FilterBuilder filter = new FilterBuilder();
    for (String p : packages){
      filter.includePackage(p);
    }
//  if (!isAllowFindingInternalClasses()) {
//      filter.excludePackage(URM_PACKAGE);
//    }

    reflections = new Reflections(new ConfigurationBuilder()
        .forPackages(packages.toArray(new String[packages.size()]))
        .setUrls(ClasspathHelper.forClassLoader(classLoaders))
        .setScanners(new SubTypesScanner(false), new ResourcesScanner())
        .addClassLoaders(classLoaders).filterInputsBy(filter));
    DomainClassFinder domainClassFinder= new DomainClassFinder(reflections);
    classes = domainClassFinder.findClasses(packages, ignores, classLoader);

    this.fieldScanner= new FieldScanner(classes,reflections);
    this.hierarchyScanner=new HierarchyScanner(classes,reflections);

  }
}
