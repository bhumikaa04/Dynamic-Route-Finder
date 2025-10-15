import java.awt.Color;

public class AnimationStep {
    public String nodeName;
    public String edgeFrom;
    public String edgeTo;
    public Color color;
    public String description;
    public boolean isNode;

    // Constructor 1: For Nodes (and general messages)
    public AnimationStep(String nodeName, Color color, String description) {
        this.nodeName = nodeName;
        this.color = color;
        this.description = description;
        this.isNode = true;
    }

    // Constructor 2: For Edges
    public AnimationStep(String edgeFrom, String edgeTo, Color color, String description) {
        this.edgeFrom = edgeFrom;
        this.edgeTo = edgeTo;
        this.color = color;
        this.description = description;
        this.isNode = false;
    }
}