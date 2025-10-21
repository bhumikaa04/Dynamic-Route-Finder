import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// NOTE: This class assumes Graph, Node, and Edge classes are accessible.

public class GraphSerializer {

    // --- SAVE METHOD (Adapted from previous example) ---
    public static void save(Graph graph, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\n");
            
            // 1. Write Nodes
            writer.write("  \"nodes\": [\n");
            boolean firstNode = true;
            for (Node node : graph.getNodes()) {
                if (!firstNode) writer.write(",\n");
                // Structure: {"name":"A", "x":10, "y":50}
                String nodeJson = String.format("    {\"name\":\"%s\", \"x\":%d, \"y\":%d}", 
                                                node.getName(), node.getX(), node.getY());
                writer.write(nodeJson);
                firstNode = false;
            }
            writer.write("\n  ],\n");

            // 2. Write Edges
            writer.write("  \"edges\": [\n");
            boolean firstEdge = true;
            // Iterate over the adjacency map to get all unique edges (undirected)
            Set<String> uniqueEdges = new HashSet<>();
            for (Object entryObj : graph.getAdjacencyList().entrySet()) {
                Map.Entry rawEntry = (Map.Entry) entryObj;
                Node source = (Node) rawEntry.getKey();
                List edgeList = (List) rawEntry.getValue();
                for (Object edgeObj : edgeList) {
                    Edge edge = (Edge) edgeObj;
                    String destName = edge.getDestination().getName();
                    
                    // Create a canonical edge key (e.g., "A-B" or "B-A") to ensure we only save it once
                    String edgeKey = (source.getName().compareTo(destName) < 0) 
                                     ? source.getName() + "-" + destName 
                                     : destName + "-" + source.getName();
                    
                    if (uniqueEdges.add(edgeKey)) {
                        if (!firstEdge) writer.write(",\n");
                        // Structure: {"from":"A", "to":"B", "weight":2}
                        String edgeJson = String.format("    {\"from\":\"%s\", \"to\":\"%s\", \"weight\":%d}", 
                                                        source.getName(), destName, edge.getWeight());
                        writer.write(edgeJson);
                        firstEdge = false;
                    }
                }
            }
            writer.write("\n  ]\n");
            
            writer.write("}\n");
        }
    }

    // --- LOAD METHOD (Manual JSON Parsing) ---
    public static Graph load(File file) throws IOException, IllegalStateException {
        StringBuilder content = new StringBuilder();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine());
            }
        }
        String jsonContent = content.toString().replaceAll("\\s", ""); // Remove all whitespace
        
        Graph newGraph = new Graph();
        Map<String, Node> nodeMap = new HashMap<>();

        // Regex to find the content of the "nodes" array
        Pattern nodesPattern = Pattern.compile("\"nodes\":\\[(.*?)\\]");
        Matcher nodesMatcher = nodesPattern.matcher(jsonContent);
        
        if (!nodesMatcher.find()) {
            throw new IllegalStateException("Invalid graph format: 'nodes' array not found.");
        }
        String nodesContent = nodesMatcher.group(1);
        
        // Regex to match individual node objects: {"name":"X","x":Y,"y":Z}
        Pattern nodePattern = Pattern.compile("\\{\"name\":\"(.*?)\",\"x\":(\\d+),\"y\":(\\d+)\\}");
        Matcher nodeMatcher = nodePattern.matcher(nodesContent);

        // 1. Parse and create Nodes
        while (nodeMatcher.find()) {
            String name = nodeMatcher.group(1);
            int x = Integer.parseInt(nodeMatcher.group(2));
            int y = Integer.parseInt(nodeMatcher.group(3));
            
            Node node = new Node(name, x, y);
            newGraph.addNode(node);
            nodeMap.put(name, node);
        }

        // 2. Parse Edges
        Pattern edgesPattern = Pattern.compile("\"edges\":\\[(.*?)\\]");
        Matcher edgesMatcher = edgesPattern.matcher(jsonContent);
        
        if (!edgesMatcher.find()) {
            // No edges is acceptable, but the block structure must exist
            return newGraph; 
        }
        String edgesContent = edgesMatcher.group(1);

        // Regex to match individual edge objects: {"from":"A","to":"B","weight":W}
        Pattern edgePattern = Pattern.compile("\\{\"from\":\"(.*?)\",\"to\":\"(.*?)\",\"weight\":(\\d+)\\}");
        Matcher edgeMatcher = edgePattern.matcher(edgesContent);
        
        while (edgeMatcher.find()) {
            String fromName = edgeMatcher.group(1);
            String toName = edgeMatcher.group(2);
            int weight = Integer.parseInt(edgeMatcher.group(3));
            
            Node fromNode = nodeMap.get(fromName);
            Node toNode = nodeMap.get(toName);

            if (fromNode != null && toNode != null) {
                newGraph.addUndirectedEdge(fromNode, toNode, weight);
            } else {
                // Warning if an edge references a node not defined in the node list
                System.err.println("Warning: Skipping edge (" + fromName + " -> " + toName + ") due to missing node.");
            }
        }

        return newGraph;
    }
}
