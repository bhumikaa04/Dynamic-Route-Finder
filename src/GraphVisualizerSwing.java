import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GraphVisualizerSwing {
    private JFrame frame;
    private JPanel graphPanel;
    private JComboBox<String> startNodeCombo, goalNodeCombo;
    private JTextArea metricsText;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton nextStepButton;
    private JTextArea explanationText; // NEW: For detailed explanations

    private Graph graph;
    private Map<String, Point> nodePositions = new HashMap<>();
    private Map<String, Color> nodeColors = new HashMap<>();
    private List<AnimationStep> currentTrace = Collections.emptyList();
    private int currentStepIndex = 0;
    
    // Colors
    public static final Color DEFAULT_NODE_COLOR = new Color(52, 152, 219); // Blue
    public static final Color FRONTIER_NODE_COLOR = new Color(255, 193, 7); // Yellow/Orange
    public static final Color VISITED_NODE_COLOR = new Color(108, 117, 125); // Gray (Closed Set)
    public static final Color PATH_NODE_COLOR = new Color(40, 167, 69); // Green (Final Path)
    public static final Color START_NODE_COLOR = new Color(111, 66, 193); // Purple for start
    public static final Color GOAL_NODE_COLOR = new Color(220, 53, 69); // Red for goal

    public static final Color DEFAULT_EDGE_COLOR = Color.GRAY;
    public static final Color VISITED_EDGE_COLOR = new Color(0, 123, 255); // Brighter Blue
    public static final Color PATH_EDGE_COLOR = new Color(25, 135, 84); // Darker Green
    
    private Map<String, Map<String, Color>> edgeColors = new HashMap<>();

    public GraphVisualizerSwing(Graph graph) {
        this.graph = graph;
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Graph Pathfinding Visualizer - Interactive Step-by-Step");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel controlPanel = createControlPanel();
        frame.add(controlPanel, BorderLayout.WEST);

        // Center panel containing graph and explanation
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // Graph panel with legend overlay
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1000, 600));
        
        // Graph panel
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        graphPanel.setBounds(0, 0, 1000, 600);
        graphPanel.setBackground(new Color(248, 249, 250));
        layeredPane.add(graphPanel, JLayeredPane.DEFAULT_LAYER);
        
        // Add legend as overlay in top-right corner
        JPanel legendPanel = createLegendPanel();
        legendPanel.setBounds(820, 10, 170, 180); // Position in top-right
        legendPanel.setOpaque(true);
        layeredPane.add(legendPanel, JLayeredPane.PALETTE_LAYER);
        
        centerPanel.add(layeredPane, BorderLayout.CENTER);
        
        // Explanation Panel below graph
        JPanel explanationPanel = new JPanel(new BorderLayout());
        explanationPanel.setBorder(BorderFactory.createTitledBorder("Step Explanation"));
        explanationText = new JTextArea(4, 80);
        explanationText.setFont(new Font("Arial", Font.PLAIN, 12));
        explanationText.setEditable(false);
        explanationText.setBackground(new Color(255, 255, 240)); // Light yellow background
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setText("Click an algorithm button to prepare the visualization, then use 'Next Step' to walk through the algorithm execution step by step.");
        explanationPanel.add(new JScrollPane(explanationText), BorderLayout.CENTER);
        explanationPanel.setPreferredSize(new Dimension(1000, 120));
        
        centerPanel.add(explanationPanel, BorderLayout.SOUTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready to visualize graph algorithms");
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(150, 16));
        progressBar.setIndeterminate(false);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);
        frame.add(statusPanel, BorderLayout.SOUTH);

        initializeNodePositions();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(350, 720)); // Adjusted height
        panel.setBackground(new Color(233, 236, 239));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Graph Algorithm Visualizer", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(33, 37, 41));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        // Node selection
        JPanel nodePanel = new JPanel(new GridLayout(2, 2, 6, 6));
        nodePanel.setBackground(panel.getBackground());
        nodePanel.setBorder(BorderFactory.createTitledBorder("Select Nodes"));
        startNodeCombo = new JComboBox<>();
        goalNodeCombo = new JComboBox<>();
        populateNodeCombos();
        nodePanel.add(new JLabel("Start Node:"));
        nodePanel.add(startNodeCombo);
        nodePanel.add(new JLabel("Goal Node:"));
        nodePanel.add(goalNodeCombo);
        panel.add(nodePanel);

        panel.add(Box.createVerticalStrut(10));
        panel.add(createAlgorithmPanel());
        panel.add(Box.createVerticalStrut(10));
        
        // Step Control Panel
        JPanel stepPanel = new JPanel(new BorderLayout());
        stepPanel.setBorder(BorderFactory.createTitledBorder("Animation Control"));
        nextStepButton = new JButton("▶ Start Animation (0/0)");
        nextStepButton.setFont(new Font("Arial", Font.BOLD, 14));
        nextStepButton.setBackground(new Color(23, 162, 184));
        nextStepButton.setForeground(Color.WHITE);
        nextStepButton.setEnabled(false);
        nextStepButton.addActionListener(e -> stepThroughAnimation());
        stepPanel.add(nextStepButton, BorderLayout.CENTER);
        panel.add(stepPanel);
        panel.add(Box.createVerticalStrut(10));
        
        panel.add(createMetricsPanel());
        panel.add(Box.createVerticalStrut(10));
        
        // Control buttons
        JPanel controlButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton resetButton = new JButton("⟳ Reset");
        resetButton.setFont(new Font("Arial", Font.BOLD, 13));
        resetButton.setBackground(new Color(220, 53, 69));
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(e -> resetVisualization());
        
        JButton autoPlayButton = new JButton("⏩ Auto Play");
        autoPlayButton.setFont(new Font("Arial", Font.BOLD, 13));
        autoPlayButton.setBackground(new Color(40, 167, 69));
        autoPlayButton.setForeground(Color.WHITE);
        // autoPlayButton.addActionListener(e -> autoPlayAnimation());
        
        controlButtons.add(resetButton);
        controlButtons.add(autoPlayButton);
        panel.add(controlButtons);

        return panel;
    }

    private JPanel createAlgorithmPanel() {
        JPanel algoPanel = new JPanel(new GridLayout(5, 1, 6, 6));
        algoPanel.setBackground(new Color(233, 236, 239));
        algoPanel.setBorder(BorderFactory.createTitledBorder("Algorithms"));

        JButton bfsButton = createStyledButton("BFS Traversal", new Color(0, 123, 255));
        JButton dfsButton = createStyledButton("DFS Traversal", new Color(108, 117, 125));
        JButton dijkstraButton = createStyledButton("Dijkstra's Algorithm", new Color(255, 193, 7));
        JButton aStarMButton = createStyledButton("A* (Manhattan)", new Color(23, 162, 184));
        JButton aStarEButton = createStyledButton("A* (Euclidean)", new Color(23, 162, 184).darker());

        algoPanel.add(bfsButton);
        algoPanel.add(dfsButton);
        algoPanel.add(dijkstraButton);
        algoPanel.add(aStarMButton);
        algoPanel.add(aStarEButton);
        return algoPanel;
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> runAlgorithm(text));
        return button;
    }

    private JPanel createMetricsPanel() {
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Results"));
        metricsText = new JTextArea(6, 25);
        metricsText.setFont(new Font("Consolas", Font.PLAIN, 12));
        metricsText.setEditable(false);
        metricsText.setBackground(new Color(255, 255, 255));
        metricsPanel.add(new JScrollPane(metricsText), BorderLayout.CENTER);
        return metricsPanel;
    }

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        legend.setBackground(new Color(255, 255, 255, 240)); // Semi-transparent white
        
        JLabel title = new JLabel("Legend");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.add(title);
        legend.add(Box.createVerticalStrut(5));
        
        legend.add(createLegendItem("Start Node", START_NODE_COLOR));
        legend.add(createLegendItem("Goal Node", GOAL_NODE_COLOR));
        legend.add(createLegendItem("Unexplored", DEFAULT_NODE_COLOR));
        legend.add(createLegendItem("Frontier", FRONTIER_NODE_COLOR));
        legend.add(createLegendItem("Explored", VISITED_NODE_COLOR));
        legend.add(createLegendItem("Path", PATH_NODE_COLOR));
        return legend;
    }

    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        item.setBackground(new Color(255, 255, 255, 0)); // Transparent
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Color square
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(16, 16));
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        item.add(colorBox);
        item.add(textLabel);
        return item;
    }

    private void populateNodeCombos() {
        Set<String> nodeNames = getNodeNamesFromGraph();
        for (String name : nodeNames) {
            startNodeCombo.addItem(name);
            goalNodeCombo.addItem(name);
        }
        if (!nodeNames.isEmpty()) {
            startNodeCombo.setSelectedIndex(0);
            goalNodeCombo.setSelectedIndex(nodeNames.size() > 1 ? nodeNames.size() - 1 : 0);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getNodeNamesFromGraph() {
        Set<String> names = new HashSet<>();
        try {
            java.lang.reflect.Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacencyList = (Map<Node, List<Edge>>) adjField.get(graph);
            for (Node node : adjacencyList.keySet()) {
                names.add(node.getName());
            }
        } catch (Exception e) {
            names.addAll(Arrays.asList("A", "B", "C", "D", "E"));
        }
        return names;
    }

    private void initializeNodePositions() {
        try {
            java.lang.reflect.Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacencyList = (Map<Node, List<Edge>>) adjField.get(graph);
            
            // LARGER: Increased scale and offsets for bigger graph
            int scale = 120; // Increased from 80
            int offsetX = 150; // Increased from 120
            int offsetY = 120; // Increased from 80
            
            for (Node node : adjacencyList.keySet()) {
                int x = node.getX() * scale + offsetX;
                int y = node.getY() * scale + offsetY;
                nodePositions.put(node.getName(), new Point(x, y));
                
                // Set start and goal node colors
                String startNode = (String) startNodeCombo.getSelectedItem();
                String goalNode = (String) goalNodeCombo.getSelectedItem();
                if (node.getName().equals(startNode)) {
                    nodeColors.put(node.getName(), START_NODE_COLOR);
                } else if (node.getName().equals(goalNode)) {
                    nodeColors.put(node.getName(), GOAL_NODE_COLOR);
                } else {
                    nodeColors.put(node.getName(), DEFAULT_NODE_COLOR);
                }
                
                // Initialize edge colors
                edgeColors.putIfAbsent(node.getName(), new HashMap<>());
                for(Edge edge : adjacencyList.get(node)) {
                    edgeColors.get(node.getName()).put(edge.getDestination().getName(), DEFAULT_EDGE_COLOR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawGraph(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(3)); // Thicker lines for better visibility

        try {
            java.lang.reflect.Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacencyList = (Map<Node, List<Edge>>) adjField.get(graph);

            // Draw edges
            for (Node from : adjacencyList.keySet()) {
                for (Edge edge : adjacencyList.get(from)) {
                    Node to = edge.getDestination();
                    Point p1 = nodePositions.get(from.getName());
                    Point p2 = nodePositions.get(to.getName());
                    
                    if (p1 != null && p2 != null) {
                        Color edgeColor = edgeColors.getOrDefault(from.getName(), Collections.emptyMap())
                                .getOrDefault(to.getName(), DEFAULT_EDGE_COLOR);
                        g2d.setColor(edgeColor);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

                        // Draw weight label with better positioning
                        int midX = (p1.x + p2.x) / 2;
                        int midY = (p1.y + p2.y) / 2;
                        g2d.setColor(edgeColor.equals(DEFAULT_EDGE_COLOR) ? Color.RED.darker() : edgeColor.darker());
                        g2d.setFont(new Font("Arial", Font.BOLD, 14)); // Larger font
                        
                        // Offset weight label to avoid overlap with line
                        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
                        int labelX = midX + (int)(15 * Math.sin(angle));
                        int labelY = midY - (int)(15 * Math.cos(angle));
                        
                        g2d.drawString(String.valueOf(edge.getWeight()), labelX, labelY);
                    }
                }
            }

            // Draw nodes (larger for better visibility)
            for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
                Point p = entry.getValue();
                String name = entry.getKey();
                
                // Draw node circle (larger)
                g2d.setColor(nodeColors.get(name));
                g2d.fillOval(p.x - 20, p.y - 20, 40, 40); // Increased from 30 to 40
                g2d.setColor(Color.BLACK);
                g2d.drawOval(p.x - 20, p.y - 20, 40, 40);
                
                // Draw node label (larger font)
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16)); // Increased from 14
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(name);
                g2d.drawString(name, p.x - textWidth/2, p.y + 6);
            }
        } catch (Exception e) {
            g2d.drawString("Error drawing graph: " + e.getMessage(), 10, 20);
        }
    }

    private void runAlgorithm(String algorithm) {
        resetVisualization();
        
        String startNodeName = (String) startNodeCombo.getSelectedItem();
        String goalNodeName = (String) goalNodeCombo.getSelectedItem();

        if (startNodeName == null || goalNodeName == null) {
            statusLabel.setText("Please select both start and goal nodes.");
            return;
        }
        
        nextStepButton.setEnabled(false);
        statusLabel.setText("Calculating " + algorithm + " trace...");
        progressBar.setIndeterminate(true);
        
        // Update node colors for start and goal
        initializeNodePositions(); // This will recolor start and goal nodes

        SwingWorker<Graph.AlgorithmTraceResult, Void> worker = new SwingWorker<>() {
            @Override
            protected Graph.AlgorithmTraceResult doInBackground() throws Exception {
                Node start = findNodeByName(startNodeName);
                Node goal = findNodeByName(goalNodeName);
                
                if (start == null || goal == null) return null;

                switch (algorithm) {
                    case "BFS Traversal":
                        return graph.bfsWithTrace(start, goal);
                    case "DFS Traversal":
                        return graph.dfsWithTrace(start, goal);
                    case "Dijkstra's Algorithm":
                        return graph.dijkstraWithTrace(start, goal);
                    case "A* (Manhattan)":
                        return graph.aStarWithTrace(start, goal, "manhattan");
                    case "A* (Euclidean)":
                        return graph.aStarWithTrace(start, goal, "euclidean");
                    default:
                        return null;
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    Graph.AlgorithmTraceResult result = get();
                    if (result != null && !result.trace.isEmpty()) {
                        currentTrace = (List<AnimationStep>) (List<?>) result.trace;
                        currentStepIndex = 0;
                        nextStepButton.setEnabled(true);
                        nextStepButton.setText(String.format("▶ Step 1/%d", currentTrace.size()));
                        statusLabel.setText("Trace ready! Click 'Next Step' to begin.");
                        explanationText.setText("Algorithm trace prepared! Click 'Next Step' to start the step-by-step visualization.\n\n" +
                                "We'll explore how " + algorithm + " finds the path from " + startNodeName + " to " + goalNodeName + ".");
                        
                        displayMetrics(algorithm, result.finalResult, result.finalPath);
                    } else {
                        statusLabel.setText(algorithm + " failed or no path found.");
                        explanationText.setText("No path found from " + startNodeName + " to " + goalNodeName + 
                                " using " + algorithm + ". Try different nodes or check the graph connections.");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error executing algorithm: " + ex.getMessage());
                    explanationText.setText("Error: " + ex.getMessage() + "\n\nCheck that all required methods are implemented in Graph.java.");
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    @SuppressWarnings("unchecked")
    private Node findNodeByName(String name) {
        try {
            java.lang.reflect.Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacencyList = (Map<Node, List<Edge>>) adjField.get(graph);
            for (Node node : adjacencyList.keySet()) {
                if (node.getName().equals(name)) {
                    return node;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void displayMetrics(String algorithm, Object resultObj, List<Node> path) {
        StringBuilder metrics = new StringBuilder();
        metrics.append("Algorithm: ").append(algorithm).append("\n");
        metrics.append("Path: ").append(path == null || path.isEmpty() ? "Not Found" : path).append("\n");
        
        if (resultObj != null) {
            try {
                Class<?> cls = resultObj.getClass();
                java.lang.reflect.Field costField = cls.getDeclaredField("cost");
                java.lang.reflect.Field visitedField = cls.getDeclaredField("visitedCount");
                java.lang.reflect.Field timeField = cls.getDeclaredField("timeNs");
                costField.setAccessible(true);
                visitedField.setAccessible(true);
                timeField.setAccessible(true);
                int cost = costField.getInt(resultObj);
                int visited = visitedField.getInt(resultObj);
                long timeNs = timeField.getLong(resultObj);
                metrics.append("Cost: ").append(cost == Integer.MAX_VALUE ? "∞" : cost).append("\n");
                metrics.append("Nodes Visited: ").append(visited).append("\n");
                metrics.append(String.format("Time: %.3f ms\n", timeNs / 1_000_000.0));
            } catch (Exception e) {
                metrics.append("Length: ").append(path != null && path.size() > 0 ? path.size() - 1 : "0").append("\n");
            }
        } else {
            metrics.append("Length: ").append(path != null && path.size() > 0 ? path.size() - 1 : "0").append("\n");
        }
        
        metricsText.setText(metrics.toString());
    }

    // IMPROVED: Step Through Animation with Detailed Explanations
    private void stepThroughAnimation() {
        if (currentStepIndex >= currentTrace.size()) {
            nextStepButton.setText("🎉 Animation Complete!");
            nextStepButton.setEnabled(false);
            statusLabel.setText("Algorithm execution finished!");
            explanationText.setText("🎉 Algorithm completed successfully!\n\n" +
                    "The final path has been highlighted in green. You can see the optimal route from start to goal.\n" +
                    "Compare the results in the 'Algorithm Results' panel above.\n\n" +
                    "Try running other algorithms to compare their performance!");
            return;
        }

        AnimationStep step = currentTrace.get(currentStepIndex);
        
        // Apply the visual change
        if (step.isNode) {
            nodeColors.put(step.nodeName, step.color);
        } else {
            edgeColors.putIfAbsent(step.edgeFrom, new HashMap<>());
            edgeColors.get(step.edgeFrom).put(step.edgeTo, step.color);
            
            // Update reverse edge for undirected graphs
            if (edgeColors.containsKey(step.edgeTo) && edgeColors.get(step.edgeTo).containsKey(step.edgeFrom)) {
                edgeColors.get(step.edgeTo).put(step.edgeFrom, step.color);
            }
        }

        // Generate detailed explanation based on the step
        String detailedExplanation = generateStepExplanation(step, currentStepIndex);
        
        // Update UI
        statusLabel.setText(String.format("Step %d/%d: %s", 
            currentStepIndex + 1, currentTrace.size(), step.description));
        nextStepButton.setText(String.format("▶ Step %d/%d", 
            currentStepIndex + 1, currentTrace.size()));
        explanationText.setText(detailedExplanation);
        
        graphPanel.repaint();
        currentStepIndex++;
    }

    // NEW: Generate detailed explanations for each step
    private String generateStepExplanation(AnimationStep step, int stepIndex) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Step ").append(stepIndex + 1).append(": ").append(step.description).append("\n\n");
        
        if (step.isNode) {
            if (step.color.equals(FRONTIER_NODE_COLOR)) {
                explanation.append("• This node has been added to the frontier (queue/stack/priority queue)\n");
                explanation.append("• It's now waiting to be explored\n");
                explanation.append("• The algorithm will examine this node's neighbors in future steps");
            } else if (step.color.equals(VISITED_NODE_COLOR)) {
                explanation.append("• This node has been fully explored\n");
                explanation.append("• All its neighbors have been processed\n");
                explanation.append("• It won't be visited again (closed set)");
            } else if (step.color.equals(PATH_NODE_COLOR)) {
                explanation.append("• This node is part of the final solution path\n");
                explanation.append("• It contributes to the optimal route from start to goal");
            }
        } else {
            if (step.color.equals(VISITED_EDGE_COLOR)) {
                explanation.append("• This edge is being explored\n");
                explanation.append("• The algorithm is checking the connection between these nodes\n");
                explanation.append("• It may lead to discovering new nodes or shorter paths");
            } else if (step.color.equals(PATH_EDGE_COLOR)) {
                explanation.append("• This edge is part of the final solution path\n");
                explanation.append("• It represents a step in the optimal route");
            }
        }
        
        // Add algorithm-specific context
        if (stepIndex == 0) {
            explanation.append("\n\n💡 Tip: The algorithm starts from the beginning and systematically explores the graph.");
        } else if (stepIndex == currentTrace.size() - 1) {
            explanation.append("\n\n🎯 Final Step: The algorithm has completed and found the optimal path!");
        }
        
        return explanation.toString();
    }

    private void resetVisualization() {
        currentTrace = Collections.emptyList();
        currentStepIndex = 0;
        nextStepButton.setText("▶ Start Animation (0/0)");
        nextStepButton.setEnabled(false);

        // Reset all node colors
        initializeNodePositions(); // This will reset colors including start/goal
        
        // Reset all edge colors
        for (Map<String, Color> map : edgeColors.values()) {
            map.replaceAll((k, v) -> DEFAULT_EDGE_COLOR);
        }

        metricsText.setText("");
        statusLabel.setText("Visualization reset");
        explanationText.setText("Ready to visualize! Select an algorithm to see how it finds paths in the graph.");
        graphPanel.repaint();
    }

    public static void launchVisualization(Graph graph) {
        SwingUtilities.invokeLater(() -> new GraphVisualizerSwing(graph));
    }
}