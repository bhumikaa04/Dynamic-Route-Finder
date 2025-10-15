import java.util.List;

public class PathResult {
    public final List<Node> path;
    public final int cost;
    public final int visitedCount;
    public final long timeNs;

    public PathResult(List<Node> path, int cost, int visitedCount, long timeNs) {
        this.path = path;
        this.cost = cost;
        this.visitedCount = visitedCount;
        this.timeNs = timeNs;
    }
}