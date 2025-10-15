import java.util.Objects;

public class Node {
    private String name ; 
    private final int  x ;     
    private final int  y ;     

    public Node(String name , int x , int y){
        this.name = name ; 
        this.x = x ; 
        this.y = y ; 
    }

    public String getName(){
        return name ; 
    }
    public int getX() { return x; } 
    public int getY() { return y; }
    @Override
    public String toString(){
        return name; 
    }
    @Override
    public boolean equals(Object o){
        if(this == o) return true ; 
        if(!(o instanceof Node)) return false ; 
        Node node = (Node) o ; 
        return x == node.x && y == node.y && name.equals(node.name); 
    }
    @Override
    public int hashCode(){
        return Objects.hash(name , x , y); 
    }
}