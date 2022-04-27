package com.iluwatar.urm.presenters;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.junit.Test;



public class PresenterTest {

  @Test
  public void parseShouldReturnCorrectPresenter() {
    Presenter presenter = Presenter.parse("graphviz", false, false, new ArrayList<>(), "");
    assertTrue(presenter.getClass().getSimpleName().equals("GraphvizPresenter"));
    presenter = Presenter.parse("plantuml", false, false, new ArrayList<>(), "");
    assertTrue(presenter.getClass().getSimpleName().equals("PlantUmlPresenter"));
    presenter = Presenter.parse("mermaid", false, false, new ArrayList<>(), "");
    assertTrue(presenter.getClass().getSimpleName().equals("MermaidPresenter"));
  }
}
