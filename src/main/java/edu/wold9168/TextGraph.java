package edu.wold9168;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextGraph {
    private static final double D = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double EPSILON = 1e-6;
    private static final String TRAVERSE_FILE = "random_traverse.ini";
    private static volatile boolean walkInterrupted = false;

    static class Graph {
        Map<String, Map<String, Integer>> adjList = new HashMap<>();
        Map<String, Double> pageRank = new HashMap<>();
        Set<String> nodes = new HashSet<>();
        boolean pagerankCalculated = false;

        void addEdge(String from, String to) {
            nodes.add(from);
            nodes.add(to);
            adjList.computeIfAbsent(from, k -> new HashMap<>())
                    .merge(to, 1, Integer::sum);
        }

        boolean containsNode(String word) {
            return nodes.contains(word);
        }

        Set<String> getBridgeWords(String w1, String w2) {
            String lowerW1 = w1; // 改用局部变量
            String lowerW2 = w2;
            Set<String> bridges = new LinkedHashSet<>();
            if (!containsNode(lowerW1) || !containsNode(lowerW2))
                return bridges;

            Map<String, Integer> neighbors = adjList.getOrDefault(lowerW1, Collections.emptyMap());
            neighbors.keySet().stream()
                    .filter(bridge -> adjList.getOrDefault(bridge, Collections.emptyMap()).containsKey(lowerW2)) // 使用effectively
                                                                                                                 // final变量
                    .forEach(bridges::add);
            return bridges;
        }

        Map<String, List<PathInfo>> shortestPaths(String start, String end) {
            start = start;
            end = end;
            Map<String, List<PathInfo>> paths = new HashMap<>();
            Map<String, Double> minDistances = new HashMap<>();
            nodes.forEach(node -> minDistances.put(node, Double.MAX_VALUE));
            minDistances.put(start, 0.0);

            PriorityQueue<PathInfo> queue = new PriorityQueue<>(Comparator.comparingDouble(a -> a.totalWeight));
            queue.add(new PathInfo(start, 0.0, new ArrayList<>(List.of(start))));

            while (!queue.isEmpty()) {
                PathInfo current = queue.poll();
                String currentWord = current.current();
                double currentWeight = current.totalWeight;

                // 跳过非最优路径
                if (currentWeight > minDistances.get(currentWord))
                    continue;

                // 记录到达终点的路径
                if (currentWord.equals(end)) {
                    paths.computeIfAbsent(end, k -> new ArrayList<>()).add(current);
                    continue;
                }

                // 遍历邻接节点
                adjList.getOrDefault(currentWord, Collections.emptyMap())
                        .forEach((neighbor, weight) -> {
                            double newWeight = currentWeight + weight;
                            // 只有找到更优路径时才继续处理
                            if (newWeight <= minDistances.get(neighbor)) {
                                if (newWeight < minDistances.get(neighbor)) {
                                    minDistances.put(neighbor, newWeight);
                                    paths.remove(neighbor);
                                }
                                List<String> newPath = new ArrayList<>(current.path);
                                newPath.add(neighbor);
                                queue.add(new PathInfo(neighbor, newWeight, newPath));
                            }
                        });
            }
            return paths;
        }

        void calculatePageRank() {
            int N = nodes.size();
            Map<String, Double> tempRank = new HashMap<>();
            nodes.forEach(node -> pageRank.put(node, 1.0 / N));

            for (int i = 0; i < MAX_ITERATIONS; i++) {
                double diff = 0.0;
                tempRank.clear();
                for (String node : nodes) {
                    double sum = nodes.stream()
                            .filter(u -> adjList.getOrDefault(u, Collections.emptyMap()).containsKey(node))
                            .mapToDouble(u -> pageRank.get(u) / adjList.get(u).size())
                            .sum();
                    double newRank = (1 - D) / N + D * sum;
                    tempRank.put(node, newRank);
                    diff += Math.abs(newRank - pageRank.get(node));
                }
                pageRank.putAll(tempRank);
                if (diff < EPSILON)
                    break;
            }
            pagerankCalculated = true;
        }

        List<String> randomWalk() {
            List<String> path = new ArrayList<>();
            if (nodes.isEmpty())
                return path;

            Random rand = new Random();
            List<String> nodeList = new ArrayList<>(nodes);
            String current = nodeList.get(rand.nextInt(nodeList.size()));
            Set<String> visitedEdges = new HashSet<>();

            System.out.println("\n=== 随机游走开始 ==="); // 新增实时输出
            path.add(current);
            System.out.println("当前节点: " + current); // 新增实时输出

            while (true) {
                Map<String, Integer> neighbors = adjList.getOrDefault(current, Collections.emptyMap());
                if (neighbors.isEmpty()) {
                    System.out.println("到达无出边的节点，终止遍历"); // 新增提示
                    break;
                }

                List<String> choices = new ArrayList<>(neighbors.keySet());
                String next = choices.get(rand.nextInt(choices.size()));
                String edge = current + "->" + next;

                if (visitedEdges.contains(edge)) {
                    System.out.println("\n发现重复边 " + edge + "，终止遍历"); // 新增提示
                    break;
                }

                visitedEdges.add(edge);
                current = next;
                path.add(current);
                System.out.println(" -> " + current); // 新增路径显示

                try {
                    // 添加带进度提示的等待
                    for (int i = 0; i < 5; i++) {
                        if (walkInterrupted)
                            break;
                        System.out.print(".");
                        TimeUnit.MILLISECONDS.sleep(200);
                    }
                    System.out.println();
                } catch (InterruptedException e) {
                    walkInterrupted = true;
                }

                if (walkInterrupted) {
                    System.out.println("\n用户中断遍历");
                    break;
                }
            }
            System.out.println("=== 随机游走结束 ===\n");
            return path;
        }
    }

    static class PathInfo {
        String currentNode;
        double totalWeight;
        List<String> path;

        PathInfo(String node, double weight, List<String> path) {
            this.currentNode = node;
            this.totalWeight = weight;
            this.path = new ArrayList<>(path);
        }

        String current() {
            return currentNode;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java TextGraph <filename>");
            return;
        }

        Graph graph = buildGraph(args[0]);
        showDirectedGraph(graph);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                switch (choice) {
                    case 1 -> showDirectedGraph(graph);
                    case 2 -> handleBridgeWords(graph, scanner);
                    case 3 -> handleNewText(graph, scanner);
                    case 4 -> handleShortestPath(graph, scanner);
                    case 5 -> handlePageRank(graph, scanner);
                    case 6 -> handleRandomWalk(graph);
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Invalid choice");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number");
            }
        }
    }

    private static Graph buildGraph(String filename) {
        Graph graph = new Graph();
        Pattern nonAlpha = Pattern.compile("[^a-zA-Z]+");

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String prevWord = null;

            while ((line = reader.readLine()) != null) {
                String[] words = nonAlpha.matcher(line).replaceAll(" ").split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (prevWord != null) {
                            graph.addEdge(prevWord, word);
                        }
                        prevWord = word;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
        return graph;
    }

    public static void showDirectedGraph(Graph graph) {
        System.out.println("\nDirected Graph:");
        graph.adjList.forEach((from, neighbors) -> {
            System.out.printf("%s -> ", from);
            neighbors.forEach((to, weight) -> System.out.printf("%s(%d) ", to, weight));
            System.out.println();
        });
    }

    public static String queryBridgeWords(Graph graph, String word1, String word2) {
        word1 = word1;
        word2 = word2;

        if (!graph.containsNode(word1))
            return "No " + word1 + " in the graph.";
        if (!graph.containsNode(word2))
            return "No " + word2 + " in the graph.";

        Set<String> bridges = graph.getBridgeWords(word1, word2);
        if (bridges.isEmpty()) {
            return String.format("No bridge words from %s to %s", word1, word2);
        }
        List<String> bridgeList = new ArrayList<>(bridges);
        if (bridgeList.size() == 1) {
            return String.format("The bridge words from %s to %s are: %s.",
                    word1, word2, bridgeList.get(0));
        }
        String last = bridgeList.remove(bridgeList.size() - 1);
        return String.format("The bridge words from %s to %s are: %s and %s.",
                word1, word2, String.join(", ", bridgeList), last);
    }

    public static String generateNewText(Graph graph, String inputText) {
        Pattern nonAlpha = Pattern.compile("[^a-zA-Z]+");
        String[] words = nonAlpha.matcher(inputText).replaceAll(" ").split("\\s+");
        List<String> result = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < words.length - 1; i++) {
            result.add(words[i]);
            Set<String> bridges = graph.getBridgeWords(words[i], words[i + 1]);
            if (!bridges.isEmpty()) {
                List<String> blist = new ArrayList<>(bridges);
                result.add(blist.get(rand.nextInt(blist.size())));
            }
        }
        result.add(words[words.length - 1]);
        return String.join(" ", result);
    }

    public static String calcShortestPath(Graph graph, String word1, String word2) {
        word1 = word1;
        word2 = word2;

        if (!graph.containsNode(word1))
            return "No " + word1 + " in the graph.";
        if (word2.isEmpty()) {
            return calculateAllShortestPaths(graph, word1);
        }
        if (!graph.containsNode(word2))
            return "No " + word2 + " in the graph.";

        Map<String, List<PathInfo>> paths = graph.shortestPaths(word1, word2);
        if (paths.isEmpty())
            return "No path from " + word1 + " to " + word2;

        StringBuilder sb = new StringBuilder();
        paths.get(word2).forEach(p -> {
            sb.append("Path: ").append(String.join("->", p.path))
                    .append(" (Total weight: ").append(p.totalWeight).append(")\n");
        });
        return sb.toString();
    }

    private static String calculateAllShortestPaths(Graph graph, String start) {
        StringBuilder sb = new StringBuilder();
        graph.nodes.stream()
                .filter(node -> !node.equals(start))
                .forEach(node -> {
                    Map<String, List<PathInfo>> paths = graph.shortestPaths(start, node);
                    if (!paths.isEmpty()) {
                        sb.append("To ").append(node).append(":\n");
                        paths.get(node).forEach(p -> sb.append("  ").append(String.join("->", p.path))
                                .append(" (").append(p.totalWeight).append(")\n"));
                    }
                });
        return sb.length() > 0 ? sb.toString() : "No paths found";
    }

    public static Double calPageRank(Graph graph, String word) {
        if (!graph.pagerankCalculated)
            graph.calculatePageRank();
        return graph.pageRank.getOrDefault(word, -1.0);
    }

    public static String randomWalk(Graph graph) {
        walkInterrupted = false;
        List<String> path = graph.randomWalk();
        try (PrintWriter pw = new PrintWriter(TRAVERSE_FILE)) {
            path.forEach(pw::println);
        } catch (FileNotFoundException e) {
            System.err.println("Error writing traverse file: " + e.getMessage());
        }
        return walkInterrupted ? "Walk interrupted" : "Walk completed";
    }

    private static void printMenu() {
        System.out.println("\n=== Text Graph Analyzer ===");
        System.out.println("1. Show Directed Graph");
        System.out.println("2. Query Bridge Words");
        System.out.println("3. Generate New Text");
        System.out.println("4. Calculate Shortest Path");
        System.out.println("5. Calculate PageRank");
        System.out.println("6. Random Walk");
        System.out.println("0. Exit");
        System.out.print("Enter choice: ");
    }

    private static void handleBridgeWords(Graph graph, Scanner scanner) {
        System.out.print("Enter two words (separated by space): ");
        String[] words = scanner.nextLine().trim().split("\\s+", 2);
        if (words.length != 2) {
            System.out.println("Please enter exactly two words");
            return;
        }
        System.out.println(queryBridgeWords(graph, words[0], words[1]));
    }

    private static void handleNewText(Graph graph, Scanner scanner) {
        System.out.print("Enter text: ");
        String input = scanner.nextLine();
        System.out.println("New text: " + generateNewText(graph, input));
    }

    private static void handleShortestPath(Graph graph, Scanner scanner) {
        System.out.print("Enter one or two words: ");
        String[] words = scanner.nextLine().trim().split("\\s+", 2);
        String result = words.length == 1 ? calcShortestPath(graph, words[0], "")
                : calcShortestPath(graph, words[0], words[1]);
        System.out.println(result);
    }

    private static void handlePageRank(Graph graph, Scanner scanner) {
        System.out.print("Enter a word: ");
        String word = scanner.nextLine().trim();
        Double pr = calPageRank(graph, word);
        if (pr < 0) {
            System.out.println("Word not found");
        } else {
            System.out.printf("PageRank of %s: %.4f%n", word, pr);
        }
    }

    private static void handleRandomWalk(Graph graph) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> walkInterrupted = true));
        System.out.println("Starting random walk... (Press Ctrl+C to stop)");
        System.out.println("Result: " + randomWalk(graph));
    }
}

// 辅助函数说明：
// 1. buildGraph - 从文件构建图结构
// 2. showDirectedGraph - 可视化显示有向图
// 3. queryBridgeWords - 桥接词查询核心逻辑
// 4. generateNewText - 新文本生成核心逻辑
// 5. calcShortestPath - 最短路径计算核心逻辑
// 6. calPageRank - PageRank计算核心逻辑
// 7. randomWalk - 随机游走核心逻辑
// 8. PathInfo - 最短路径信息封装类
// 9. handle*方法 - 各功能的用户交互处理
// 10. calculateAllShortestPaths - 单点到全图的最短路径计算