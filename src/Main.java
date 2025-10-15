import java.util.List;

public class Main {
    public static void main(String[] args) {
        Graph g = new Graph();

        Node A = new Node("A", 0, 0);
        Node B = new Node("B", 1, 0);
        Node C = new Node("C", 1, 1);
        Node D = new Node("D", 2, 1);
        Node E = new Node("E", 3, 1);

        // add nodes
        g.addNode(A); g.addNode(B); g.addNode(C); g.addNode(D); g.addNode(E);

        // edges (undirected example)
        g.addUndirectedEdge(A, B, 1);
        g.addUndirectedEdge(B, C, 1);
        g.addUndirectedEdge(C, D, 1);
        g.addUndirectedEdge(D, E, 1);
        g.addUndirectedEdge(A, C, 4);
        g.addUndirectedEdge(B, D, 5);

        System.out.println("Graph:");
        g.displayGraph();

        // Run algorithms in console
        List<Node> bfsPath = g.bfs(A, E);
        System.out.println("\nBFS path A->E: " + bfsPath);

        var dres = g.dijkstra(A, E);
        System.out.println("\nDijkstra path A->E: " + dres.path + " cost=" + dres.cost + " visited=" + dres.visitedCount + " timeNs=" + dres.timeNs);

        // Launch Swing visualization
        GraphVisualizerSwing.launchVisualization(g);
    }
}