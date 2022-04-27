package com.iluwatar.urm.presenters;

import com.iluwatar.urm.domain.DomainClass;
import com.iluwatar.urm.domain.Edge;

import java.util.List;

public interface Presenter {

  Representation describe(List<DomainClass> domainObjects, List<Edge> edges);

  String getFileEnding();

  /**
   * Factory method for {@link Presenter}.
   * @param presenterString as a String
   * @param skipMethods as boolean
   * 
   * @return chosen Presenter
   */
  static Presenter parse(String presenterString, boolean skipMethods, boolean skipConstructors, List<String> allowedAnnotations, String plantUmlLineType) {
    if (presenterString == null || presenterString.equalsIgnoreCase("plantuml")) {
      return new PlantUmlPresenter(skipMethods, skipConstructors, allowedAnnotations,plantUmlLineType);
    } else if (presenterString.equalsIgnoreCase("graphviz")) {
      return new GraphvizPresenter(skipMethods, skipConstructors);
    } else if (presenterString.equalsIgnoreCase("mermaid")) {
      return new MermaidPresenter(skipMethods, skipConstructors, allowedAnnotations);
    }
    return new PlantUmlPresenter(skipMethods, skipConstructors, allowedAnnotations, plantUmlLineType);
  }
  
}
