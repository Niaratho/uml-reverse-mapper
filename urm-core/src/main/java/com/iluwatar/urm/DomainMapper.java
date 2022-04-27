package com.iluwatar.urm;

import com.iluwatar.urm.domain.Direction;
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
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

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
    List<DomainClass> domainObjects = classes.stream().map(DomainClass::new)
        .collect(Collectors.toList());
    edges=mergeBuilderAndClass(edges, domainObjects);
    return presenter.describe(domainObjects, edges);
  }

  // necessary for Lombok Builder Pattern
  private List<Edge> mergeBuilderAndClass(List<Edge> edges, List<DomainClass> domainObjects) {
    List<DomainClass> listOfBuilders = domainObjects.stream()
        .filter(c -> c.getClassName().endsWith("Builder"))
        .collect(Collectors.toList());

    Set<Edge> newEdges= new HashSet<>();

    for (DomainClass builder : listOfBuilders) {
      DomainClass clazz = findClass(builder, domainObjects);
      if (clazz != null) {
        newEdges.clear();
        clazz.setBuilderFlag(true);
        for (Edge e :edges) {
          boolean sourcebuilder = e.source.getClassName().equals(builder.getClassName());
          boolean targetbuilder = e.target.getClassName().equals(builder.getClassName());
          boolean sourcenormal = e.source.getClassName().equals(clazz.getClassName());
          boolean targetnormal = e.target.getClassName().equals(clazz.getClassName());
          if ((sourcebuilder && targetnormal) || (targetbuilder && sourcenormal)) {
            continue;
          } else if (sourcebuilder) {
            newEdges.add(new Edge(clazz, e.target, e.type, e.direction));
          } else if (targetbuilder) {
            newEdges.add(new Edge(e.source, clazz, e.type, e.direction));
          } else{
            newEdges.add(e);
          }
        }
        domainObjects.remove(builder);
        edges = newEdges.stream().collect(Collectors.toList());
      }
    }

    //filter if bi-directional and remove edge without decription

    List<Edge> edgesNew = new ArrayList<>();

    for (int i = 0; i < edges.size(); i++) {
       Edge e = edges.get(i);
       boolean keepEdge = false;
       for (int j = i+1; j < edges.size(); j++) {
         Edge compareEdge = edges.get(j);
         String eSourceName = e.source.getClassName();
         String cTargetName = compareEdge.target.getClassName();
         String eTargetName = e.target.getClassName();
         String cSourceName = compareEdge.source.getClassName();

         if (eSourceName.equals(cSourceName) && eTargetName.equals(cTargetName)) {
           Edge edge = null;
           if (StringUtils.isNotEmpty(e.source.getDescription()) || StringUtils.isNotEmpty(e.target.getDescription())) {
             edge = e;
           } else {
             edge = compareEdge;
           }
           //lambda defines this
           Edge finalEdge = edge;
           if (!edgesNew.stream().anyMatch(ed -> ed.source.getClassName().equals(finalEdge.source.getClassName())&& ed.target.getClassName().equals(
               finalEdge.target.getClassName()))) {
             edgesNew.add(edge);
           }

         } else if (eSourceName.equals(cTargetName) && cSourceName.equals(eTargetName)) {
           edgesNew.add(new Edge(e.source, compareEdge.source, e.type, Direction.BI_DIRECTIONAL));
         }
      }
      if (!edgesNew.stream().anyMatch(ed -> ed.source.getClassName().equals(e.source.getClassName())&& ed.target.getClassName().equals(
          e.target.getClassName()))) {
        edgesNew.add(e);
      }
    }
    return edgesNew;
  }




  private DomainClass findClass(DomainClass builder, List<DomainClass> domainObjects) {
    for (DomainClass clazz : domainObjects) {
      if (String.format("%sBuilder", clazz.getClassName()).equals(builder.getClassName())) {
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
