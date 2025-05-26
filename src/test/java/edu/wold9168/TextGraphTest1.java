package edu.wold9168;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TextGraphTest1 {
  private TextGraph.Graph graph;

  @Before
  public void setUp() {
    graph = new TextGraph.Graph();
  }

  @Test
  public void testFirstNodeMissing() {
    String result = TextGraph.calcShortestPath(graph, "A", "B");

    assertTrue(result.contains("No A"));
  }

  @Test
  public void testSingleNodeGraph() {
    graph.addEdge("A", "A");
    String result = TextGraph.calcShortestPath(graph, "A", "");
    assertTrue(result.contains("No paths found"));
  }

  @Test
  public void testDisconnectedNodes() {
    graph.addEdge("A", "B");
    graph.addEdge("C", "D");
    String result = TextGraph.calcShortestPath(graph, "A", "C");
    assertEquals("No path from A to C", result);
  }

  @Test
  public void testSingleShortestPath() {
    graph.addEdge("A", "B");
    graph.addEdge("B", "C");
    String result = TextGraph.calcShortestPath(graph, "A", "C");
    assertTrue(result.contains("A->B->C") && result.contains("Total weight: 2.0"));
  }

  @Test
  public void testMultipleShortestPaths() {
    graph.addEdge("A", "B");
    graph.addEdge("A", "C");
    graph.addEdge("B", "D");
    graph.addEdge("C", "D");
    String result = TextGraph.calcShortestPath(graph, "A", "D");
    assertTrue(result.contains("A->B->D") && result.contains("A->C->D"));
  }

  @Test
  public void testSelfLoopPath() {
    graph.addEdge("A", "A");
    String result = TextGraph.calcShortestPath(graph, "A", "A");
    assertTrue(result.contains("Path: A") && result.contains("Total weight: 0.0"));
  }

  @Test
  public void testAllPathsFromNode() {
    graph.addEdge("A", "B");
    graph.addEdge("A", "C");
    graph.addEdge("B", "D");
    String result = TextGraph.calcShortestPath(graph, "A", "");
    assertTrue(result.contains("To B") && result.contains("To C") && result.contains("To D"));
  }

  @Test
  public void testWeightedPaths() {
    graph.addEdge("A", "B");
    graph.addEdge("A", "C");
    graph.addEdge("B", "D");
    graph.addEdge("C", "D");
    graph.addEdge("A", "D");
    String result = TextGraph.calcShortestPath(graph, "A", "D");
    assertTrue(result.contains("A->D (Total weight: 1.0)"));
  }

  @Test
  public void testSecondNodeMissing() {
    graph.addEdge("A", "C");
    String result = TextGraph.calcShortestPath(graph, "A", "B");
    assertTrue(result.contains("No B"));
  }
}