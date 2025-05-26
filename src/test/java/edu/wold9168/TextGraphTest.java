package edu.wold9168;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextGraphTest {
  @Test
  public void testNoWord1() {
    TextGraph.Graph graph = new TextGraph.Graph();
    graph.addEdge("existing", "word");
    String result = TextGraph.queryBridgeWords(graph, "nonexistent", "word");
    assertEquals("No nonexistent in the graph.", result);
  }

  @Test
  public void testNoWord2() {
    TextGraph.Graph graph = new TextGraph.Graph();
    graph.addEdge("existing", "word");
    String result = TextGraph.queryBridgeWords(graph, "existing", "missing");
    assertEquals("No missing in the graph.", result);
  }

  @Test
  public void testNoBridgeWords() {
    TextGraph.Graph graph = new TextGraph.Graph();
    graph.addEdge("hello", "world");
    graph.addEdge("foo", "bar");
    String result = TextGraph.queryBridgeWords(graph, "hello", "bar");
    assertEquals("No bridge words from hello to bar", result);
  }

  @Test
  public void testSingleBridge() {
    TextGraph.Graph graph = new TextGraph.Graph();
    graph.addEdge("hello", "bridge");
    graph.addEdge("bridge", "world");
    String result = TextGraph.queryBridgeWords(graph, "hello", "world");
    assertEquals("The bridge words from hello to world are: bridge.", result);
  }

  @Test
  public void testMultipleBridges() {
    TextGraph.Graph graph = new TextGraph.Graph();
    graph.addEdge("start", "b1");
    graph.addEdge("start", "b2");
    graph.addEdge("b1", "end");
    graph.addEdge("b2", "end");

    String result = TextGraph.queryBridgeWords(graph, "start", "end");
    assertEquals("The bridge words from start to end are: b2 and b1.", result);
  }
}