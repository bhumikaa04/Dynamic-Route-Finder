import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class GraphVisualizerSwing {
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private JFrame frame;
    private Graph graph;

    // UI components
    private GraphCanvas canvas;
    private JComboBox<String> startCombo, goalCombo;
    private JButton btnPlayPause, btnStep, btnReset;
    private JToggleButton btnAuto;
    private JSlider speedSlider;
    private JLabel lblStatus, lblSpeedValue;
    private JList<String> listFrontier, listVisited, listPath;
    private DefaultListModel<String> frontierModel = new DefaultListModel<>();
    private DefaultListModel<String> visitedModel = new DefaultListModel<>();
    private DefaultListModel<String> pathModel = new DefaultListModel<>();
    private JTextArea explanationArea;

    // Animation / state
    private List<AnimationStep> trace = Collections.emptyList();
    private int traceIndex = 0;
    private javax.swing.Timer autoTimer;
    private boolean isPlaying = false;
    
    // Colors
    private static final Color COLOR_BG = new Color(248, 249, 250);
    private static final Color COLOR_PANEL = Color.WHITE;
    private static final Color COLOR_ACCENT = new Color(23, 162, 184);
    private static final Color COLOR_START = new Color(111, 66, 193);
    private static final Color COLOR_GOAL = new Color(220, 53, 69);
    private static final Color COLOR_DEFAULT = new Color(52, 152, 219);
    private static final Color COLOR_FRONTIER = new Color(255, 193, 7);
    private static final Color COLOR_VISITED = new Color(108, 117, 125);
    private static final Color COLOR_PATH = new Color(40, 167, 69);
    private static final Color EDGE_DEFAULT = new Color(160, 160, 160);
    private static final Color EDGE_HIGHLIGHT = new Color(23, 162, 184);

    // Visual maps
    private Map<String, Point> nodePositions = new HashMap<>();
    private Map<String, Color> nodeColors = new HashMap<>();
    private Map<String, Map<String, Color>> edgeColors = new HashMap<>();

    public GraphVisualizerSwing(Graph graph) {
        this.graph = graph;
        SwingUtilities.invokeLater(this::buildAndShow);
    }

    private void buildAndShow() {
        frame = new JFrame("Graph Algorithm Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1200, 800));
        frame.getContentPane().setBackground(COLOR_BG);
        frame.setLayout(new BorderLayout());

        // Header
        JPanel header = createHeaderPanel();
        frame.add(header, BorderLayout.NORTH);

        // Main split: left canvas, right controls
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.70);
        mainSplit.setBorder(null);

        // Canvas panel
        canvas = new GraphCanvas();
        JPanel canvasHolder = new JPanel(new BorderLayout());
        canvasHolder.setBackground(COLOR_BG);
        canvasHolder.add(canvas, BorderLayout.CENTER);
        canvasHolder.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(12, 12, 12, 6),
            BorderFactory.createLineBorder(new Color(200, 200, 200))
        ));

        mainSplit.setLeftComponent(canvasHolder);

        // Right side: controls and info
        JPanel rightPanel = createRightPanel();
        mainSplit.setRightComponent(rightPanel);

        frame.add(mainSplit, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        frame.add(statusBar, BorderLayout.SOUTH);

        // Wire up actions
        setupEventHandlers();

        // Initialize data
        populateNodeCombos();
        initializeNodePositions();
        updateUIFromGraph();

        // Timer for autoplay
        autoTimer = new javax.swing.Timer(speedSlider.getValue(), e -> {
            if (traceIndex < trace.size()) stepOnce();
            else togglePlayPause();
        });
        
        speedSlider.addChangeListener(e -> {
            autoTimer.setDelay(speedSlider.getValue());
            lblSpeedValue.setText(speedSlider.getValue() + "ms");
        });
        
        frame.pack();
        mainSplit.setDividerLocation((int)(frame.getWidth()*0.70));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // --- 1. Left Panel (Title + Create Button) ---
        // Use FlowLayout to arrange title and button side-by-side
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0)); 
        leftPanel.setBackground(COLOR_BG);
        
        // Title
        JLabel title = new JLabel("Graph Algorithm Visualizer");
        title.setFont(TITLE_FONT);
        title.setForeground(new Color(33, 37, 41));
        leftPanel.add(title);
        
        // Create Graph Button
        JButton createGraphBtn = new JButton("Create New Graph");
        
        // FIX: Set a reasonable width (e.g., 160) for the text to fit.
        Dimension buttonSize = new Dimension(160, 35); 
        createGraphBtn.setPreferredSize(buttonSize); 
        
        createGraphBtn.setFont(UI_FONT);
        createGraphBtn.setBackground(new Color(108, 117, 125));
        createGraphBtn.setForeground(Color.WHITE);
        createGraphBtn.setFocusPainted(false);
        createGraphBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        createGraphBtn.addActionListener(e -> openCreateGraphDialog());

        JButton openGraphBtn = new JButton("Open Graph...");
        openGraphBtn.setPreferredSize(new Dimension(130, 35)); 
        openGraphBtn.setFont(UI_FONT);
        openGraphBtn.setBackground(new Color(40, 167, 69)); // Green for success/open
        openGraphBtn.setForeground(Color.WHITE);
        openGraphBtn.setFocusPainted(false);
        openGraphBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        openGraphBtn.addActionListener(e -> openGraphFromFile());
        leftPanel.add(openGraphBtn); 

        JButton saveGraphButton = new JButton("Save Graph");
        
        // FIX: Set a reasonable width (e.g., 160) for the text to fit. 
        createGraphBtn.setPreferredSize(buttonSize); 
        
        saveGraphButton.setFont(UI_FONT);
        saveGraphButton.setBackground(new Color(108, 117, 125));
        saveGraphButton.setForeground(Color.WHITE);
        saveGraphButton.setFocusPainted(false);
        saveGraphButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        saveGraphButton.addActionListener(e -> saveGraphToFile());
        
        leftPanel.add(createGraphBtn); 
        leftPanel.add(saveGraphButton);
        
        // Add the combined panel to the header's WEST region
        header.add(leftPanel, BorderLayout.WEST); 

        // ----------------------------------------------------------------------
        
        // Algorithm selection panel (Right side - No changes needed here)
        JPanel algoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        algoPanel.setBackground(COLOR_BG);
        algoPanel.add(new JLabel("Algorithm:"));

        JComboBox<String> algoCombo = new JComboBox<>(new String[]{
            "Breadth-First Search (BFS)", 
            "Depth-First Search (DFS)", 
            "Dijkstra's Algorithm",
            "A* Search (Manhattan)",
            "A* Search (Euclidean)"
        });
        algoCombo.setFont(UI_FONT);
        algoPanel.add(algoCombo);

        // Start/Goal selection
        algoPanel.add(new JLabel("Start:"));
        startCombo = new JComboBox<>(); // Initialization is correctly inside the method
        startCombo.setFont(UI_FONT);
        algoPanel.add(startCombo);
        
        algoPanel.add(new JLabel("Goal:"));
        goalCombo = new JComboBox<>(); // Initialization is correctly inside the method
        goalCombo.setFont(UI_FONT);
        algoPanel.add(goalCombo);

        JButton runBtn = new JButton("Run Algorithm");
        runBtn.setFont(UI_FONT);
        runBtn.setBackground(COLOR_ACCENT);
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        algoPanel.add(runBtn);

        header.add(algoPanel, BorderLayout.EAST);

        // Wire algorithm run
        runBtn.addActionListener(e -> {
            String selectedAlgo = (String) algoCombo.getSelectedItem();
            String algoKey = selectedAlgo.contains("BFS") ? "BFS" :
                            selectedAlgo.contains("DFS") ? "DFS" :
                            selectedAlgo.contains("Dijkstra") ? "Dijkstra" :
                            selectedAlgo.contains("Manhattan") ? "A* (Manhattan)" : "A* (Euclidean)";
            prepareAlgorithm(algoKey, runBtn);
        });

        return header;
    }


    private JPanel createRightPanel() {
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout(12, 12));
        right.setBorder(new EmptyBorder(12, 6, 12, 12));
        right.setBackground(COLOR_BG);

        // Playback controls
        JPanel playback = createPlaybackPanel();
        right.add(playback, BorderLayout.NORTH);

        // Visualization queues
        JPanel queuesPanel = createQueuesPanel();
        right.add(queuesPanel, BorderLayout.CENTER);

        // Explanation area
        JPanel explanationPanel = createExplanationPanel();
        right.add(explanationPanel, BorderLayout.SOUTH);

        return right;
    }

    private JPanel createPlaybackPanel() {
        JPanel playback = new JPanel(new GridBagLayout());
        playback.setBackground(COLOR_PANEL);
        playback.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Animation Controls"),
            new EmptyBorder(8, 8, 8, 8)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // First row: buttons
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        btnPlayPause = createControlButton("Play");
        playback.add(btnPlayPause, gbc);

        gbc.gridx = 1;
        btnStep = createControlButton("Step");
        playback.add(btnStep, gbc);

        gbc.gridx = 2;
        btnReset = createControlButton("Reset");
        playback.add(btnReset, gbc);

        // Second row: auto mode and speed
        gbc.gridx = 0; gbc.gridy = 1;
        playback.add(new JLabel("Auto Mode:"), gbc);

        gbc.gridx = 1;
        btnAuto = new JToggleButton("Off");
        btnAuto.setFont(UI_FONT);
        btnAuto.setFocusPainted(false);
        playback.add(btnAuto, gbc);

        gbc.gridx = 2;
        playback.add(new JLabel("Speed:"), gbc);

        // Third row: speed slider
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        speedSlider = new JSlider(100, 2000, 600);
        speedSlider.setMajorTickSpacing(500);
        speedSlider.setMinorTickSpacing(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        playback.add(speedSlider, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1;
        lblSpeedValue = new JLabel("600ms");
        lblSpeedValue.setFont(UI_FONT);
        playback.add(lblSpeedValue, gbc);

        return playback;
    }

    private JPanel createQueuesPanel() {
        JPanel queuesPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        queuesPanel.setBackground(COLOR_BG);

        // Frontier queue
        JPanel frontierPanel = createQueuePanel("Frontier Queue", frontierModel, COLOR_FRONTIER);
        listFrontier = findListInPanel(frontierPanel);

        // Visited nodes
        JPanel visitedPanel = createQueuePanel("Visited Nodes", visitedModel, COLOR_VISITED);
        listVisited = findListInPanel(visitedPanel);

        // Final path
        JPanel pathPanel = createQueuePanel("Final Path", pathModel, COLOR_PATH);
        listPath = findListInPanel(pathPanel);

        queuesPanel.add(frontierPanel);
        queuesPanel.add(visitedPanel);
        queuesPanel.add(pathPanel);

        return queuesPanel;
    }

private JPanel createQueuePanel(String title, DefaultListModel<String> model, Color color) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(COLOR_BG);
    
    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(TITLE_FONT);
    titleLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
    panel.add(titleLabel, BorderLayout.NORTH);

    JList<String> list = new JList<>(model);
    list.setFont(UI_FONT);
    list.setBackground(COLOR_PANEL);
    list.setSelectionBackground(color.brighter());
    list.setSelectionForeground(Color.BLACK);
    
    // 💡 THE CORRECTION: Set the JList to display elements horizontally.
    list.setLayoutOrientation(JList.HORIZONTAL_WRAP); 
    // Setting visible row count to 1 ensures it appears as a single, horizontal row.
    list.setVisibleRowCount(1); 
    
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setPreferredSize(new Dimension(200, 120));
    scrollPane.setBorder(BorderFactory.createLineBorder(color, 2));
    
    // 💡 IMPORTANT: Ensure horizontal scrolling is enabled (and vertical is disabled).
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    
    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
}
    @SuppressWarnings("unchecked")
    private JList<String> findListInPanel(JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JScrollPane) {
                Component view = ((JScrollPane) c).getViewport().getView();
                if (view instanceof JList) {
                    return (JList<String>) view;
                }
            }
        }
        return null;
    }

    private JPanel createExplanationPanel() {
        JPanel explanationPanel = new JPanel(new BorderLayout());
        explanationPanel.setBackground(COLOR_BG);

        JLabel title = new JLabel("Step-by-Step Explanation");
        title.setFont(TITLE_FONT);
        explanationPanel.add(title, BorderLayout.NORTH);

        explanationArea = new JTextArea(6, 20);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setEditable(false);
        explanationArea.setFont(UI_FONT);
        explanationArea.setBackground(COLOR_PANEL);
        explanationArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 8, 8, 8)
        ));

        JScrollPane scrollPane = new JScrollPane(explanationArea);
        explanationPanel.add(scrollPane, BorderLayout.CENTER);

        return explanationPanel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(8, 4));
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusBar.setBackground(COLOR_BG);
        
        lblStatus = new JLabel("Ready to visualize graph algorithms");
        lblStatus.setFont(UI_FONT);
        statusBar.add(lblStatus, BorderLayout.WEST);

        JLabel helpLabel = new JLabel("Space: Play/Pause | Right Arrow: Step | Mouse: Zoom/Pan");
        helpLabel.setFont(UI_FONT.deriveFont(11f));
        helpLabel.setForeground(Color.GRAY);
        statusBar.add(helpLabel, BorderLayout.EAST);

        return statusBar;
    }

    private JButton createControlButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT);
        button.setFocusPainted(false);
        button.setBackground(COLOR_PANEL);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT),
            new EmptyBorder(6, 12, 6, 12)
        ));
        return button;
    }

    private void setupEventHandlers() {
        btnPlayPause.addActionListener(e -> togglePlayPause());
        btnStep.addActionListener(e -> stepOnce());
        btnReset.addActionListener(e -> resetVisualization());
        btnAuto.addActionListener(e -> toggleAutoMode());

        // Keyboard shortcuts
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("SPACE"), "playpause");
        frame.getRootPane().getActionMap().put("playpause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { togglePlayPause(); }
        });
        
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("RIGHT"), "step");
        frame.getRootPane().getActionMap().put("step", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { stepOnce(); }
        });
    }

    @SuppressWarnings("unchecked")
    private void populateNodeCombos() {
        startCombo.removeAllItems();
        goalCombo.removeAllItems();
        try {
            Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacency = (Map<Node, List<Edge>>) adjField.get(graph);
            for (Node n : adjacency.keySet()) {
                startCombo.addItem(n.getName());
                goalCombo.addItem(n.getName());
            }
        } catch (Exception e) {
            // fallback
            startCombo.addItem("A");
            startCombo.addItem("B");
            goalCombo.addItem("C");
        }
        if (startCombo.getItemCount() > 0) startCombo.setSelectedIndex(0);
        if (goalCombo.getItemCount() > 1) goalCombo.setSelectedIndex(goalCombo.getItemCount()-1);
    }

    @SuppressWarnings("unchecked")
    private void initializeNodePositions() {
        nodePositions.clear();
        nodeColors.clear();
        edgeColors.clear();
        try {
            Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacency = (Map<Node, List<Edge>>) adjField.get(graph);
            int scale = 120;
            int offsetX = 150;
            int offsetY = 120;
            for (Node n : adjacency.keySet()) {
                int x = n.getX()*scale + offsetX;
                int y = n.getY()*scale + offsetY;
                nodePositions.put(n.getName(), new Point(x, y));
                nodeColors.put(n.getName(), COLOR_DEFAULT);
                edgeColors.putIfAbsent(n.getName(), new HashMap<>());
                for (Edge e : adjacency.get(n)) {
                    edgeColors.get(n.getName()).put(e.getDestination().getName(), EDGE_DEFAULT);
                }
            }
            // mark start/goal
            if (startCombo.getSelectedItem()!=null) nodeColors.put((String)startCombo.getSelectedItem(), COLOR_START);
            if (goalCombo.getSelectedItem()!=null) nodeColors.put((String)goalCombo.getSelectedItem(), COLOR_GOAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUIFromGraph() {
        canvas.repaint();
        lblStatus.setText("Graph loaded - ready for algorithm visualization");
    }

    private void prepareAlgorithm(String algoName, JButton sourceButton) {
        resetVisualization();
        String start = (String) startCombo.getSelectedItem();
        String goal = (String) goalCombo.getSelectedItem();
        if (start == null || goal == null) {
            lblStatus.setText("Please select start and goal nodes first.");
            return;
        }
        lblStatus.setText("Computing " + algoName + " trace...");
        autoTimer.stop();
        isPlaying = false;
        btnPlayPause.setText("Play");

        // compute trace in background
        SwingWorker<Graph.AlgorithmTraceResult, Void> worker = new SwingWorker<>() {
            @Override
            protected Graph.AlgorithmTraceResult doInBackground() throws Exception {
                Node s = findNodeByName(start);
                Node g = findNodeByName(goal);
                if (s==null || g==null) return null;
                switch (algoName) {
                    case "BFS": return graph.bfsWithTrace(s, g);
                    case "DFS": return graph.dfsWithTrace(s, g);
                    case "Dijkstra": return graph.dijkstraWithTrace(s, g);
                    case "A* (Manhattan)": return graph.aStarWithTrace(s, g, "manhattan");
                    case "A* (Euclidean)": return graph.aStarWithTrace(s, g, "euclidean");
                    default: return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Graph.AlgorithmTraceResult res = get();
                    if (res != null && res.trace != null && !res.trace.isEmpty()) {
                        trace = (List<AnimationStep>) (List<?>) res.trace;
                        traceIndex = 0;
                        btnPlayPause.setEnabled(true);
                        btnStep.setEnabled(true);
                        frontierModel.clear(); visitedModel.clear(); pathModel.clear(); 
                        explanationArea.setText("");
                        lblStatus.setText("Trace ready - use Play/Step to visualize");
                        displayMetrics(res.finalResult, res.finalPath);
                    } else {
                        lblStatus.setText("No path found or trace empty");
                        explanationArea.setText("No path found from " + start + " to " + goal + ".");
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Failed to compute trace: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void displayMetrics(Object resultObj, List<Node> finalPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm Results:\n");
        sb.append("Path: ").append(finalPath==null?"Not Found":finalPath).append("\n");
        if (resultObj!=null) {
            try {
                Class<?> cls = resultObj.getClass();
                Field costF = cls.getDeclaredField("cost");
                Field visitedF = cls.getDeclaredField("visitedCount");
                Field timeF = cls.getDeclaredField("timeNs");
                costF.setAccessible(true); visitedF.setAccessible(true); timeF.setAccessible(true);
                int cost = costF.getInt(resultObj);
                int visited = visitedF.getInt(resultObj);
                long timeNs = timeF.getLong(resultObj);
                sb.append("Total Cost: ").append(cost==Integer.MAX_VALUE?"Infinity":cost).append("\n");
                sb.append("Nodes Visited: ").append(visited).append("\n");
                sb.append(String.format("Computation Time: %.3f ms", timeNs/1_000_000.0));
            } catch (Exception e) {
                sb.append("Path Length: ").append(finalPath==null?0:Math.max(0, finalPath.size()-1));
            }
        }
        explanationArea.setText(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private Node findNodeByName(String name) {
        try {
            Field adjField = graph.getClass().getDeclaredField("adjacencyList");
            adjField.setAccessible(true);
            Map<Node, List<Edge>> adjacency = (Map<Node, List<Edge>>) adjField.get(graph);
            for (Node n : adjacency.keySet()) if (n.getName().equals(name)) return n;
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void togglePlayPause() {
        if (trace.isEmpty()) return;
        if (isPlaying) {
            autoTimer.stop();
            isPlaying = false;
            btnPlayPause.setText("Play");
            lblStatus.setText("Paused at step " + (traceIndex) + "/" + trace.size());
        } else {
            if (traceIndex >= trace.size()) { traceIndex = 0; resetVisualState(); }
            isPlaying = true;
            autoTimer.setDelay(speedSlider.getValue());
            autoTimer.start();
            btnPlayPause.setText("Pause");
            lblStatus.setText("Playing animation...");
        }
    }

    private void toggleAutoMode() {
        if (btnAuto.isSelected()) {
            btnAuto.setText("Auto: On");
        } else {
            btnAuto.setText("Auto: Off");
        }
    }

    private void stepOnce() {
        if (trace.isEmpty()) return;
        if (traceIndex >= trace.size()) {
            lblStatus.setText("Algorithm complete - total steps: " + trace.size());
            btnPlayPause.setText("Play");
            isPlaying = false;
            autoTimer.stop();
            return;
        }
        AnimationStep s = trace.get(traceIndex);
        applyStepVisual(s);
        explanationArea.setText(generateExplanationFor(s, traceIndex));
        traceIndex++;
        lblStatus.setText("Step " + traceIndex + " / " + trace.size());
        canvas.repaint();
    }

    private void applyStepVisual(AnimationStep step) {
        if (step.isNode) {
            nodeColors.put(step.nodeName, step.color);
            if (step.color.equals(COLOR_FRONTIER)) {
                frontierModel.addElement(step.nodeName);
                // Auto-scroll to show latest addition
                listFrontier.ensureIndexIsVisible(frontierModel.size() - 1);
            }
            if (step.color.equals(COLOR_VISITED)) {
                visitedModel.addElement(step.nodeName);
                listVisited.ensureIndexIsVisible(visitedModel.size() - 1);
            }
            if (step.color.equals(COLOR_PATH)) {
                pathModel.addElement(step.nodeName);
                listPath.ensureIndexIsVisible(pathModel.size() - 1);
            }
        } else {
            edgeColors.putIfAbsent(step.edgeFrom, new HashMap<>());
            edgeColors.get(step.edgeFrom).put(step.edgeTo, step.color);
            // undirected symmetry if present
            if (edgeColors.containsKey(step.edgeTo)) {
                edgeColors.get(step.edgeTo).put(step.edgeFrom, step.color);
            }
        }
    }

    private String generateExplanationFor(AnimationStep step, int idx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Step ").append(idx+1).append(": ").append(step.description).append("\n\n");
        if (step.isNode) {
            if (step.color.equals(COLOR_FRONTIER)) {
                sb.append("• Node added to frontier queue\n");
                sb.append("• Will be explored in upcoming steps\n");
            } else if (step.color.equals(COLOR_VISITED)) {
                sb.append("• Node marked as visited\n");
                sb.append("• All neighbors have been processed\n");
            } else if (step.color.equals(COLOR_PATH)) {
                sb.append("• Node confirmed as part of final path\n");
            }
        } else {
            if (step.color.equals(EDGE_DEFAULT)) {
                sb.append("• Examining connection between nodes\n");
            } else {
                sb.append("• Edge identified as part of exploration path\n");
            }
        }
        
        if (idx == trace.size()-1) {
            sb.append("\nAlgorithm completed - final path highlighted in green");
        }
        return sb.toString();
    }

    private void resetVisualization() {
        trace = Collections.emptyList();
        traceIndex = 0;
        frontierModel.clear(); 
        visitedModel.clear(); 
        pathModel.clear();
        btnPlayPause.setText("Play");
        btnPlayPause.setEnabled(false);
        btnStep.setEnabled(false);
        autoTimer.stop(); 
        isPlaying = false;
        initializeNodePositions();
        explanationArea.setText("Visualization reset. Select an algorithm to begin.");
        canvas.repaint();
        lblStatus.setText("Visualization reset");
    }

    private void resetVisualState() {
        // Reset per-step visuals but keep start/goal
        for (String k : new ArrayList<>(nodeColors.keySet())) {
            if (nodeColors.get(k).equals(COLOR_START) || nodeColors.get(k).equals(COLOR_GOAL)) continue;
            nodeColors.put(k, COLOR_DEFAULT);
        }
        for (Map<String, Color> m : edgeColors.values()) {
            m.replaceAll((a, b) -> EDGE_DEFAULT);
        }
        frontierModel.clear(); 
        visitedModel.clear(); 
        pathModel.clear();
        canvas.repaint();
    }

    // Enhanced Canvas with zoom/pan and better visual feedback
    private class GraphCanvas extends JPanel {
        private double zoom = 1.0;
        private double translateX = 0;
        private double translateY = 0;
        private Point lastDrag;

        GraphCanvas() {
            setPreferredSize(new Dimension(900, 640));
            setBackground(COLOR_BG);
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

            addMouseWheelListener(e -> {
                double delta = -e.getPreciseWheelRotation();
                double factor = 1 + delta * 0.12;
                zoom *= factor;
                zoom = Math.max(0.25, Math.min(3.0, zoom));
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastDrag = e.getPoint();
                }

                public void mouseClicked(MouseEvent e) {
                    Point2D p = screenToWorld(e.getPoint());
                    String hit = findNodeAt(p.getX(), p.getY());
                    if (hit != null) {
                        explanationArea.setText("Selected Node: " + hit + 
                                              "\nCoordinates: " + nodePositions.get(hit) +
                                              "\nStatus: " + getNodeStatus(hit));
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    Point cur = e.getPoint();
                    translateX += (cur.x - lastDrag.x) / zoom;
                    translateY += (cur.y - lastDrag.y) / zoom;
                    lastDrag = cur;
                    repaint();
                }
            });
        }

        private String findNodeAt(double x, double y) {
            int r = 20;
            for (Map.Entry<String, Point> en : nodePositions.entrySet()) {
                Point p = en.getValue();
                if (Math.hypot(p.x - x, p.y - y) <= r) return en.getKey();
            }
            return null;
        }

        private String getNodeStatus(String nodeName) {
            Color color = nodeColors.get(nodeName);
            if (color.equals(COLOR_START)) return "Start Node";
            if (color.equals(COLOR_GOAL)) return "Goal Node";
            if (color.equals(COLOR_FRONTIER)) return "In Frontier Queue";
            if (color.equals(COLOR_VISITED)) return "Visited";
            if (color.equals(COLOR_PATH)) return "In Final Path";
            return "Unvisited";
        }

        private Point2D screenToWorld(Point p) {
            double x = (p.x - getWidth()/2.0) / zoom - translateX + getWidth()/2.0 / zoom;
            double y = (p.y - getHeight()/2.0) / zoom - translateY + getHeight()/2.0 / zoom;
            return new Point2D.Double(x, y);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply zoom and pan centered
            AffineTransform at = new AffineTransform();
            at.translate(getWidth()/2.0, getHeight()/2.0);
            at.scale(zoom, zoom);
            at.translate(-getWidth()/2.0 + translateX, -getHeight()/2.0 + translateY);
            g2d.transform(at);

            // Draw edges
            g2d.setStroke(new BasicStroke(3f / (float)zoom));
            try {
                Field adjField = graph.getClass().getDeclaredField("adjacencyList");
                adjField.setAccessible(true);
                Map<Node, List<Edge>> adjacency = (Map<Node, List<Edge>>) adjField.get(graph);
                for (Node from : adjacency.keySet()) {
                    for (Edge edge : adjacency.get(from)) {
                        Node to = edge.getDestination();
                        Point p1 = nodePositions.get(from.getName());
                        Point p2 = nodePositions.get(to.getName());
                        if (p1==null||p2==null) continue;
                        Color ec = EDGE_DEFAULT;
                        if (edgeColors.containsKey(from.getName())) {
                            ec = edgeColors.get(from.getName()).getOrDefault(to.getName(), EDGE_DEFAULT);
                        }
                        g2d.setColor(ec);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                        // weight label
                        int midX = (p1.x + p2.x)/2;
                        int midY = (p1.y + p2.y)/2;
                        g2d.setFont(new Font("Arial", Font.BOLD, (int)(12/zoom)));
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(String.valueOf(edge.getWeight()), midX+6, midY-6);
                    }
                }

                // Draw nodes
                for (Map.Entry<String, Point> en : nodePositions.entrySet()) {
                    Point p = en.getValue();
                    String name = en.getKey();
                    Color c = nodeColors.getOrDefault(name, COLOR_DEFAULT);
                    int rad = (int)(22 / zoom);
                    
                    // Node shadow for depth
                    g2d.setColor(new Color(0, 0, 0, 30));
                    g2d.fillOval(p.x-rad+2, p.y-rad+2, rad*2, rad*2);
                    
                    // Main node
                    g2d.setColor(c);
                    g2d.fillOval(p.x-rad, p.y-rad, rad*2, rad*2);
                    
                    // Node border
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1.5f/(float)zoom));
                    g2d.drawOval(p.x-rad, p.y-rad, rad*2, rad*2);
                    
                    // Node label
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, (int)(14/zoom)));
                    FontMetrics fm = g2d.getFontMetrics();
                    int tw = fm.stringWidth(name);
                    g2d.drawString(name, p.x - tw/2, p.y + fm.getAscent()/2 - 2);
                }
            } catch (Exception e) {
                g2d.setColor(Color.RED);
                g2d.drawString("Error drawing graph: " + e.getMessage(), 10, 20);
            }
            g2d.dispose();
        }
    }


    private void openCreateGraphDialog() {
    JDialog dialog = new JDialog(frame, "Create New Graph", true);
    dialog.setSize(600, 450);
    dialog.setLocationRelativeTo(frame);
    dialog.setLayout(new BorderLayout(10, 10));

    // Top panel: node/edge counts
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    JTextField nodeCountField = new JTextField(5);
    JTextField edgeCountField = new JTextField(5);
    JButton generateFieldsBtn = new JButton("Generate Fields");

    topPanel.add(new JLabel("Nodes:"));
    topPanel.add(nodeCountField);
    topPanel.add(new JLabel("Edges:"));
    topPanel.add(edgeCountField);
    topPanel.add(generateFieldsBtn);
    dialog.add(topPanel, BorderLayout.NORTH);

    // Center panel: tabs for nodes and edges
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel nodePanel = new JPanel(new BorderLayout());
    JPanel edgePanel = new JPanel(new BorderLayout());
    tabbedPane.addTab("Nodes", nodePanel);
    tabbedPane.addTab("Edges", edgePanel);
    dialog.add(tabbedPane, BorderLayout.CENTER);

    // Bottom panel: create/cancel buttons
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton createBtn = new JButton("Create Graph");
    JButton cancelBtn = new JButton("Cancel");
    bottomPanel.add(cancelBtn);
    bottomPanel.add(createBtn);
    dialog.add(bottomPanel, BorderLayout.SOUTH);

    // Table models
    DefaultTableModel nodeTableModel = new DefaultTableModel(new Object[]{"Node Name"}, 0);
    JTable nodeTable = new JTable(nodeTableModel);
    nodePanel.add(new JScrollPane(nodeTable), BorderLayout.CENTER);

    DefaultTableModel edgeTableModel = new DefaultTableModel(new Object[]{"From", "To", "Weight"}, 0);
    JTable edgeTable = new JTable(edgeTableModel);
    edgePanel.add(new JScrollPane(edgeTable), BorderLayout.CENTER);

    // Generate fields
    generateFieldsBtn.addActionListener(e -> {
        try {
            int numNodes = Integer.parseInt(nodeCountField.getText().trim());
            int numEdges = Integer.parseInt(edgeCountField.getText().trim());

            nodeTableModel.setRowCount(0);
            edgeTableModel.setRowCount(0);

            for (int i = 0; i < numNodes; i++) {
                nodeTableModel.addRow(new Object[]{(i + 1)});
            }
            for (int i = 0; i < numEdges; i++) {
                edgeTableModel.addRow(new Object[]{"", "", "1"});
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dialog, "Please enter valid numbers!", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Create graph
    createBtn.addActionListener(e -> {
        try {
            int numNodes = nodeTableModel.getRowCount();
            int numEdges = edgeTableModel.getRowCount();

            Graph g = new Graph();
            Map<String, Node> nodeMap = new HashMap<>();

            // Layout nodes
            if (numNodes <= 8) {
                double centerX = 5.0, centerY = 5.0, radius = 3.0;
                for (int i = 0; i < numNodes; i++) {
                    String name = nodeTableModel.getValueAt(i, 0).toString().trim();
                    double angle = 2 * Math.PI * i / numNodes;
                    int x = (int) (centerX + radius * Math.cos(angle));
                    int y = (int) (centerY + radius * Math.sin(angle));
                    Node node = new Node(name, x, y);
                    g.addNode(node);
                    nodeMap.put(name, node);
                }
            } else {
                int cols = (int) Math.ceil(Math.sqrt(numNodes));
                double spacing = 2.0;
                for (int i = 0; i < numNodes; i++) {
                    String name = nodeTableModel.getValueAt(i, 0).toString().trim();
                    int row = i / cols, col = i % cols;
                    Node node = new Node(name, (int) (col * spacing), (int) (row * spacing));
                    g.addNode(node);
                    nodeMap.put(name, node);
                }
            }

            for (int i = 0; i < numEdges; i++) {
    // 1. Get raw values safely, handling potential nulls from empty cells
    Object fromObj = edgeTableModel.getValueAt(i, 0);
    Object toObj = edgeTableModel.getValueAt(i, 1);
    Object weightObj = edgeTableModel.getValueAt(i, 2);

    // 2. Crucial: Check for incomplete/empty rows
if (fromObj == null || toObj == null || weightObj == null || 
        fromObj.toString().trim().isEmpty() || 
        toObj.toString().trim().isEmpty() || 
        weightObj.toString().trim().isEmpty()) {
        
        // If the row is incomplete, treat it as an error
        JOptionPane.showMessageDialog(dialog, 
            "Row " + (i + 1) + " is incomplete. Please fill all fields or remove the row.", 
            "Input Required", JOptionPane.WARNING_MESSAGE);
        return; // Stop processing and force the user to correct the table.
    }

    String from = fromObj.toString().trim();
    String to = toObj.toString().trim();
    int weight;
    
    // 3. Robust Weight Parsing
    try {
        weight = Integer.parseInt(weightObj.toString().trim());
    } catch (NumberFormatException ex) {
        // Report an error for invalid weight and stop graph creation
        JOptionPane.showMessageDialog(dialog, 
            "Invalid weight for edge " + (i + 1) + ". Weight must be an integer.", 
            "Input Error", JOptionPane.ERROR_MESSAGE);
        return; // Stop the action listener and don't dispose the dialog
    }

    Node fromNode = nodeMap.get(from);
    Node toNode = nodeMap.get(to);

    if (fromNode != null && toNode != null) {
        g.addUndirectedEdge(fromNode, toNode, weight);
    } else {
        // Report an error for invalid node and stop graph creation
        JOptionPane.showMessageDialog(dialog, 
            "Invalid node names for edge " + (i + 1) + ". Check that nodes '" + from + "' and '" + to + "' exist.", 
            "Edge Error", JOptionPane.ERROR_MESSAGE);
        return; // Stop the action listener and don't dispose the dialog
    }
}

            // Finalize
            refreshGraph(g);
            populateNodeCombos();
            dialog.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Error creating graph: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    });

    cancelBtn.addActionListener(e -> dialog.dispose());
    dialog.setVisible(true);
}


private Graph buildGraphFromUserInput(int numNodes, int numEdges) {
    Graph g = new Graph();
    Map<String, Node> nodeMap = new HashMap<>();
    boolean useCircleLayout = numNodes <= 8;

    // Node input
    for (int i = 0; i < numNodes; i++) {
        String name = JOptionPane.showInputDialog(null, "Enter name for Node " + (i + 1) + ":");
        if (name == null || name.trim().isEmpty()) name = "N" + (i + 1);
        name = name.trim().toUpperCase();

        int x, y;
        if (useCircleLayout) {
            double angle = 2 * Math.PI * i / numNodes;
            x = (int) (5.0 + 3.0 * Math.cos(angle));
            y = (int) (5.0 + 3.0 * Math.sin(angle));
        } else {
            int cols = (int) Math.ceil(Math.sqrt(numNodes));
            double spacing = 2.0;
            int row = i / cols, col = i % cols;
            x = (int) (col * spacing);
            y = (int) (row * spacing);
        }

        Node node = new Node(name, x, y);
        g.addNode(node);
        nodeMap.put(name, node);
    }

    // Edge input
    for (int i = 0; i < numEdges; i++) {
        JPanel edgePanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField fromField = new JTextField();
        JTextField toField = new JTextField();
        JTextField weightField = new JTextField("1");

        edgePanel.add(new JLabel("From Node:"));
        edgePanel.add(fromField);
        edgePanel.add(new JLabel("To Node:"));
        edgePanel.add(toField);
        edgePanel.add(new JLabel("Weight:"));
        edgePanel.add(weightField);

        int res = JOptionPane.showConfirmDialog(null, edgePanel, "Edge " + (i + 1), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String from = fromField.getText().trim().toUpperCase();
                String to = toField.getText().trim().toUpperCase();
                int weight = Integer.parseInt(weightField.getText().trim());

                Node fromNode = nodeMap.get(from);
                Node toNode = nodeMap.get(to);

                if (fromNode != null && toNode != null) {
                    g.addUndirectedEdge(fromNode, toNode, weight);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid node names for edge " + (i + 1));
                    i--; // retry
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid weight value. Please enter a number.");
                i--; // retry
            }
        } else {
            i--; // retry if canceled
        }
    }

    return g;
}

private void refreshGraph(Graph newGraph) {
    this.graph = newGraph;
    initializeNodePositions();   // reset node positions/colors
    updateUIFromGraph();         // repaint canvas and update status
}

private void openGraphFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Graph");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));

        int userSelection = fileChooser.showOpenDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            
            try {
                // Call the core serialization logic to load the graph
                Graph loadedGraph = GraphSerializer.load(fileToOpen);
                
                if (loadedGraph != null) {
                    refreshGraph(loadedGraph);
                    populateNodeCombos(); // Update dropdowns with new nodes
                    JOptionPane.showMessageDialog(frame, "Graph loaded successfully from:\n" + fileToOpen.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "The file could not be parsed or was empty.", "Load Failed", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, 
                    "Error loading graph: " + ex.getMessage() + "\nFile may be corrupted or in an incorrect format.", 
                    "Load Failed", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    // --- Save Graph Method (for completeness) ---
    private void saveGraphToFile() {
        if (this.graph == null || this.graph.getNodes().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "The current graph is empty. Nothing to save.", "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Graph");
        fileChooser.setSelectedFile(new File("custom_graph.json"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));

        int userSelection = fileChooser.showSaveDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }
            
            try {
                GraphSerializer.save(this.graph, fileToSave);
                JOptionPane.showMessageDialog(frame, "Graph saved successfully to:\n" + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error saving graph: " + ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // ---- utility / entrypoint ----
    public static void launch(Graph graph) {
        new GraphVisualizerSwing(graph);
    }
}