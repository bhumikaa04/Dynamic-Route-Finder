import java.util.*;
import java.lang.Math;
import java.awt.Color; 

public class Graph {
    private final Map<Node, List<Edge>> adjacencyList;
    private final Map<String, Node> nodeNameMap; // For quick lookup by name

    private static final Color FRONTIER_NODE_COLOR = new Color(255, 193, 7); // Yellow/Orange
    private static final Color VISITED_NODE_COLOR = new Color(108, 117, 125); // Gray (Closed Set)
    private static final Color PATH_NODE_COLOR = new Color(40, 167, 69); // Green (Final Path)
    private static final Color VISITED_EDGE_COLOR = new Color(0, 123, 255); // Brighter Blue
    private static final Color PATH_EDGE_COLOR = new Color(25, 135, 84); // Darker Green
    
    public static class AlgorithmTraceResult {
        public final PathResult finalResult;
        public final List<Node> finalPath; // Easier to access final path
        public final List<AnimationStep> trace;

        public AlgorithmTraceResult(PathResult finalResult, List<Node> finalPath, List<AnimationStep> trace) {
            this.finalResult = finalResult;
            this.finalPath = finalPath;
            this.trace = trace;
        }
    }
    // -------------------------------------------------------------------


    public Graph() {
        adjacencyList = new HashMap<>();
        nodeNameMap = new HashMap<>();
    }

    public void addNode(String name, int x, int y) {
        Node newNode = new Node(name, x, y); // Assumes Node constructor (name, x, y)
        if (!nodeNameMap.containsKey(name)) {
            nodeNameMap.put(name, newNode);
            adjacencyList.putIfAbsent(newNode, new ArrayList<>());
        }
    }
    
    // Original: Used for sample/internal graph creation
    public void addNode(Node node) {
        if (!nodeNameMap.containsKey(node.getName())) {
            nodeNameMap.put(node.getName(), node);
            adjacencyList.putIfAbsent(node, new ArrayList<>());
        }
    }

    // 💡 NEW: Method to retrieve a node by name (CRITICAL for Main.java)
    public Node getNode(String name) {
        return nodeNameMap.get(name);
    }

    public Set<Node> getNodes() {
        return adjacencyList.keySet();
    }

    
    public void addEdge(Node source, Node destination, int weight) {
        adjacencyList.putIfAbsent(source, new ArrayList<>());
        adjacencyList.putIfAbsent(destination, new ArrayList<>());
        adjacencyList.get(source).add(new Edge(destination, weight));
    }

    public void addUndirectedEdge(Node a, Node b, int weight) {
        addEdge(a, b, weight);
        addEdge(b, a, weight);
    }

    public void displayGraph() {
        for (Node node : adjacencyList.keySet()) {
            System.out.print(node.getName() + " -> ");
            for (Edge edge : adjacencyList.get(node)) {
                System.out.print(edge.getDestination().getName() + "(" + edge.getWeight() + ") ");
            }
            System.out.println();
        }
    }

    // ----------------- BFS (unweighted) -----------------
    // ... (rest of existing bfs method) ...
    public List<Node> bfs(Node start, Node goal) {
        if (!adjacencyList.containsKey(start)) return Collections.emptyList();

        Queue<Node> queue = new LinkedList<>();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        visited.add(start);
        queue.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.equals(goal)) break;

            List<Edge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (Edge e : edges) {
                Node neigh = e.getDestination();
                if (!visited.contains(neigh)) {
                    visited.add(neigh);
                    parent.put(neigh, current);
                    queue.add(neigh);
                }
            }
        }

        return reconstructPath(parent, start, goal);
    }

    /** Tracing version of BFS */
    public AlgorithmTraceResult bfsWithTrace(Node start, Node goal) {
        long startTime = System.nanoTime();
        if (!adjacencyList.containsKey(start)) return new AlgorithmTraceResult(null, Collections.emptyList(), Collections.emptyList());

        Queue<Node> queue = new LinkedList<>();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        List<AnimationStep> trace = new ArrayList<>();
        int visitedCount = 0;

        visited.add(start);
        queue.add(start);
        parent.put(start, null);
        // Uses 3-arg node constructor
        trace.add(new AnimationStep(start.getName(), FRONTIER_NODE_COLOR, "Start Node: Added to Queue"));
        visitedCount++;

        Node foundGoal = null;
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            trace.add(new AnimationStep(current.getName(), VISITED_NODE_COLOR, "Dequeue Node: " + current.getName()));
            
            if (current.equals(goal)) {
                foundGoal = current;
                break;
            }

            for (Edge e : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                Node neigh = e.getDestination();
                
                // Add edge trace (exploring) - Uses 4-arg edge constructor
                trace.add(new AnimationStep(current.getName(), neigh.getName(), VISITED_EDGE_COLOR, "Explore Edge: " + current.getName() + " -> " + neigh.getName()));

                if (!visited.contains(neigh)) {
                    visited.add(neigh);
                    parent.put(neigh, current);
                    queue.add(neigh);
                    
                    trace.add(new AnimationStep(neigh.getName(), FRONTIER_NODE_COLOR, "Enqueued Node: " + neigh.getName()));
                    visitedCount++;
                }
            }
        }
        
        long endTime = System.nanoTime();
        List<Node> finalPath = reconstructPath(parent, start, goal);
        
        // Final Path Trace
        addFinalPathTrace(trace, finalPath, parent);
        
        PathResult result = new PathResult(finalPath, finalPath.size() > 0 ? finalPath.size() - 1 : Integer.MAX_VALUE, visitedCount, endTime - startTime);
        return new AlgorithmTraceResult(result, finalPath, trace);
    }


    //---------------------DFS----------------------
    // ... (rest of existing dfs method) ...
    public List<Node> dfs(Node start, Node goal) {
        if (!adjacencyList.containsKey(start)) return Collections.emptyList();

        Stack<Node> stack = new Stack<>();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        visited.add(start);
        stack.push(start);
        parent.put(start, null);

        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current.equals(goal)) break;

            List<Edge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (int i = edges.size() - 1; i >= 0; i--) {
                Edge e = edges.get(i);
                Node neigh = e.getDestination();
                if (!visited.contains(neigh)) {
                    visited.add(neigh);
                    parent.put(neigh, current);
                    stack.push(neigh);
                }
            }
        }

        return reconstructPath(parent, start, goal);
    }
    
    /** Tracing version of DFS */
    public AlgorithmTraceResult dfsWithTrace(Node start, Node goal) {
        long startTime = System.nanoTime();
        if (!adjacencyList.containsKey(start)) return new AlgorithmTraceResult(null, Collections.emptyList(), Collections.emptyList());

        Stack<Node> stack = new Stack<>();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        List<AnimationStep> trace = new ArrayList<>();
        int visitedCount = 0;

        visited.add(start);
        stack.push(start);
        parent.put(start, null);
        trace.add(new AnimationStep(start.getName(), FRONTIER_NODE_COLOR, "Start Node: Added to Stack"));
        visitedCount++;

        Node foundGoal = null;
        while (!stack.isEmpty()) {
            Node current = stack.pop();
            
            trace.add(new AnimationStep(current.getName(), VISITED_NODE_COLOR, "Pop Node: " + current.getName()));
            
            if (current.equals(goal)) {
                foundGoal = current;
                break;
            }

            List<Edge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (int i = edges.size() - 1; i >= 0; i--) {
                Edge e = edges.get(i);
                Node neigh = e.getDestination();
                
                trace.add(new AnimationStep(current.getName(), neigh.getName(), VISITED_EDGE_COLOR, "Explore Edge: " + current.getName() + " -> " + neigh.getName()));

                if (!visited.contains(neigh)) {
                    visited.add(neigh);
                    parent.put(neigh, current);
                    stack.push(neigh);
                    
                    trace.add(new AnimationStep(neigh.getName(), FRONTIER_NODE_COLOR, "Pushed Node: " + neigh.getName()));
                    visitedCount++;
                }
            }
        }
        
        long endTime = System.nanoTime();
        List<Node> finalPath = reconstructPath(parent, start, goal);
        
        addFinalPathTrace(trace, finalPath, parent);
        
        PathResult result = new PathResult(finalPath, finalPath.size() > 0 ? finalPath.size() - 1 : Integer.MAX_VALUE, visitedCount, endTime - startTime);
        return new AlgorithmTraceResult(result, finalPath, trace);
    }


    // ----------------- Dijkstra -----------------
    // ... (rest of existing dijkstra method) ...
    public PathResult dijkstra(Node start, Node goal) {
        Map<Node, Integer> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();
        for (Node n : adjacencyList.keySet()) {
            dist.put(n, Integer.MAX_VALUE);
            prev.put(n, null);
        }

        for (List<Edge> edges : adjacencyList.values()) {
            for (Edge e : edges) {
                dist.putIfAbsent(e.getDestination(), Integer.MAX_VALUE);
                prev.putIfAbsent(e.getDestination(), null);
            }
        }

        dist.put(start, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(start);

        Set<Node> visited = new HashSet<>();
        long startTime = System.nanoTime();

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(goal)) break;

            for (Edge e : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                Node neigh = e.getDestination();
                int alt = dist.get(current) + e.getWeight();
                if (alt < dist.getOrDefault(neigh, Integer.MAX_VALUE)) {
                    dist.put(neigh, alt);
                    prev.put(neigh, current);
                    pq.add(neigh);
                }
            }
        }

        long endTime = System.nanoTime();
        List<Node> path = reconstructPath(prev, start, goal);
        int cost = dist.getOrDefault(goal, Integer.MAX_VALUE);
        return new PathResult(path, cost, visited.size(), endTime - startTime);
    }

public AlgorithmTraceResult dijkstraWithTrace(Node start, Node goal) {
    long startTime = System.nanoTime();
    Map<Node, Integer> dist = new HashMap<>();
    Map<Node, Node> prev = new HashMap<>();
    List<AnimationStep> trace = new ArrayList<>();
    // closedSet tracks nodes for which the shortest path is finalized (visited/closed).
    Set<Node> closedSet = new HashSet<>(); 

    // 1. Initialization (Simplified for clarity, assuming getNodes() works)
    for (Node n : getNodes()) { 
        dist.put(n, Integer.MAX_VALUE);
        prev.put(n, null);
    }

    dist.put(start, 0);
    
    PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
    pq.add(start);
    
    trace.add(new AnimationStep(start.getName(), FRONTIER_NODE_COLOR, "Start Node: Added to Priority Queue (Cost: 0)"));

    while (!pq.isEmpty()) {
        Node current = pq.poll();
        
        if (closedSet.contains(current)) continue;
        
        closedSet.add(current);
        trace.add(new AnimationStep(current.getName(), VISITED_NODE_COLOR, "Extract Min: " + current.getName() + " (Cost: " + dist.get(current) + ")"));

        if (current.equals(goal)) break;

        for (Edge e : adjacencyList.getOrDefault(current, Collections.emptyList())) {
            Node neigh = e.getDestination();
            int alt = dist.get(current) + e.getWeight();
            
            trace.add(new AnimationStep(current.getName(), neigh.getName(), VISITED_EDGE_COLOR, "Relax Edge: " + current.getName() + " -> " + neigh.getName() + " (Weight: " + e.getWeight() + ")"));

            if (alt < dist.getOrDefault(neigh, Integer.MAX_VALUE)) {
                dist.put(neigh, alt);
                prev.put(neigh, current);
                pq.add(neigh); 

                trace.add(new AnimationStep(neigh.getName(), FRONTIER_NODE_COLOR, "Update Cost: " + neigh.getName() + " (New Cost: " + alt + ")"));
            }
        }
    }

    long endTime = System.nanoTime();
    List<Node> finalPath = reconstructPath(prev, start, goal);
    int cost = dist.getOrDefault(goal, Integer.MAX_VALUE);
    
    // Assuming this method exists for visualization/tracing
    addFinalPathTrace(trace, finalPath, prev); 

    // 🛑 Final result uses closedSet.size(), not a separate, error-prone counter.
    PathResult result = new PathResult(finalPath, cost, closedSet.size(), endTime - startTime);
    return new AlgorithmTraceResult(result, finalPath, trace);
}
    

    // ----------------- A* -----------------
    // ... (rest of existing aStar method) ...
    public PathResult aStar(Node start, Node goal, String heuristic) {
        Map<Node, Double> gScore = new HashMap<>();
        Map<Node, Double> fScore = new HashMap<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        for (Node n : getNodes()) {
            gScore.put(n, Double.POSITIVE_INFINITY);
            fScore.put(n, Double.POSITIVE_INFINITY);
            cameFrom.put(n, null);
        }
        
        for (List<Edge> edges : adjacencyList.values()) {
            for (Edge e : edges) {
                gScore.putIfAbsent(e.getDestination(), Double.POSITIVE_INFINITY);
                fScore.putIfAbsent(e.getDestination(), Double.POSITIVE_INFINITY);
                cameFrom.putIfAbsent(e.getDestination(), null);
            }
        }

        gScore.put(start, 0.0);
        fScore.put(start, heuristicValue(start, goal, heuristic));

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(fScore::get));
        openSet.add(start);
        Set<Node> closedSet = new HashSet<>();
        int visitedCount = 0;
        long startTime = System.nanoTime();

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            if (closedSet.contains(current)) continue;
            
            if (current.equals(goal)) {
                long endTime = System.nanoTime();
                List<Node> path = reconstructPath(cameFrom, start, goal);
                int cost = (int) Math.round(gScore.getOrDefault(goal, Double.POSITIVE_INFINITY));
                return new PathResult(path, cost, closedSet.size(), endTime - startTime);
            }

            closedSet.add(current);
            visitedCount++;

            for (Edge e : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                Node neighbor = e.getDestination();
                if (closedSet.contains(neighbor)) continue;

                double tentativeG = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + e.getWeight();
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristicValue(neighbor, goal, heuristic));

                    openSet.remove(neighbor); 
                    openSet.add(neighbor);
                }
            }
        }

        long endTime = System.nanoTime();
        return new PathResult(Collections.emptyList(), Integer.MAX_VALUE, visitedCount, endTime - startTime);
    }
    
    /** Tracing version of A* */
    public AlgorithmTraceResult aStarWithTrace(Node start, Node goal, String heuristic) {
        long startTime = System.nanoTime();
        Map<Node, Double> gScore = new HashMap<>();
        Map<Node, Double> fScore = new HashMap<>();
        Map<Node, Node> cameFrom = new HashMap<>();
        List<AnimationStep> trace = new ArrayList<>();
        Set<Node> closedSet = new HashSet<>();
        
        for (Node n : getNodes()) {
            gScore.put(n, Double.POSITIVE_INFINITY);
            fScore.put(n, Double.POSITIVE_INFINITY);
            cameFrom.put(n, null);
        }
        
        for (List<Edge> edges : adjacencyList.values()) {
            for (Edge e : edges) {
                gScore.putIfAbsent(e.getDestination(), Double.POSITIVE_INFINITY);
                fScore.putIfAbsent(e.getDestination(), Double.POSITIVE_INFINITY);
                cameFrom.putIfAbsent(e.getDestination(), null);
            }
        }

        gScore.put(start, 0.0);
        fScore.put(start, heuristicValue(start, goal, heuristic));

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(fScore::get));
        openSet.add(start);
        
        trace.add(new AnimationStep(start.getName(), FRONTIER_NODE_COLOR, String.format("Start Node: Added to PQ (f=%.1f)", fScore.get(start))));

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (closedSet.contains(current)) continue;
            
            double currentF = fScore.getOrDefault(current, Double.POSITIVE_INFINITY);
            trace.add(new AnimationStep(current.getName(), VISITED_NODE_COLOR, String.format("Extract Min: %s (f=%.1f)", current.getName(), currentF)));
            
            if (current.equals(goal)) break;

            closedSet.add(current);

            for (Edge e : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                Node neighbor = e.getDestination();
                
                trace.add(new AnimationStep(current.getName(), neighbor.getName(), VISITED_EDGE_COLOR, "Explore Edge: " + current.getName() + " -> " + neighbor.getName() + " (w=" + e.getWeight() + ")"));

                if (closedSet.contains(neighbor)) continue;

                double tentativeG = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + e.getWeight();
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    double newF = tentativeG + heuristicValue(neighbor, goal, heuristic);
                    fScore.put(neighbor, newF);

                    openSet.remove(neighbor); 
                    openSet.add(neighbor);

                    trace.add(new AnimationStep(neighbor.getName(), FRONTIER_NODE_COLOR, String.format("Update/Add: %s (g=%.1f, f=%.1f)", neighbor.getName(), tentativeG, newF)));
                }
            }
        }

        long endTime = System.nanoTime();
        List<Node> finalPath = reconstructPath(cameFrom, start, goal);
        int cost = (int) Math.round(gScore.getOrDefault(goal, Double.POSITIVE_INFINITY));

        addFinalPathTrace(trace, finalPath, cameFrom);
        
        PathResult result = new PathResult(finalPath, cost, closedSet.size(), endTime - startTime);
        return new AlgorithmTraceResult(result, finalPath, trace);
    }
    
    // ----------------- Trace Helper Method -----------------
    private void addFinalPathTrace(List<AnimationStep> trace, List<Node> finalPath, Map<Node, ? extends Node> parentMap) {
        if (finalPath.isEmpty()) {
            // FIX 1: Use the 3-argument Node constructor (null nodeName for general message)
            trace.add(new AnimationStep(null, Color.RED, "Path not found.")); 
            return;
        }
        
        // FIX 2: Use the 3-argument Node constructor (null nodeName for general message)
        trace.add(new AnimationStep(null, Color.GREEN, "PATH FOUND. Tracing back..."));

        // Trace nodes backwards from goal to start
        for (int i = finalPath.size() - 1; i >= 0; i--) {
            Node current = finalPath.get(i);
            trace.add(new AnimationStep(current.getName(), PATH_NODE_COLOR, "Final Path Node: " + current.getName()));

            if (i > 0) {
                Node previous = finalPath.get(i - 1);
                // Highlight the edge from previous -> current
                trace.add(new AnimationStep(previous.getName(), current.getName(), PATH_EDGE_COLOR, "Final Path Edge: " + previous.getName() + " -> " + current.getName()));
            }
        }
    }


    // ----------------- Utility Methods -----------------
    private double heuristicValue(Node a, Node b, String heuristic) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        if ("manhattan".equalsIgnoreCase(heuristic)) {
            return dx + dy;
        } else { // euclidean
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

    private List<Node> reconstructPath(Map<Node, ? extends Node> parent, Node start, Node goal) {
        if (!parent.containsKey(goal) && !goal.equals(start)) {
            return Collections.emptyList();
        }
        LinkedList<Node> path = new LinkedList<>();
        Node curr = goal;
        
        if (parent.get(curr) == null && !curr.equals(start)) {
            return Collections.emptyList();
        }
        
        while (curr != null) {
            path.addFirst(curr);
            curr = parent.get(curr);
        }
        if (path.isEmpty() || !path.get(0).equals(start)) return Collections.emptyList();
        return path;
    }

    public void addDirectedEdge(Node node, Node node2, int weight) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addDirectedEdge'");
    }

    public Map getAdjacencyList() {
        return this.adjacencyList;
    }
}