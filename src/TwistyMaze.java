import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Deque;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/*
  Controls for game:
  Arrow Keys (up, down, left, right) ---- move player
  "p" ----------------------------------- toggle visited paths on/off
  "r" ----------------------------------- reset game
  "b" ----------------------------------- breath first search
  "d" ----------------------------------- depth first search
  "m" ----------------------------------- toggle manual mode on/off
  manual mode on: player is required to trace back to the beginning at the end
  manual mode off: shortest path is automatically displayed when player reaches the end
*/

// class to represent posn
class NodePosn extends Posn {

  public NodePosn(int x, int y) {
    super(x, y);
  }

  // is this nodePosn same as that nodePosn
  public boolean same(Object that) {
    if (that instanceof NodePosn) {
      NodePosn posn = (NodePosn) that;
      return this.x == posn.x && this.y == posn.y;
    }
    else {
      return false;
    }
  }
}

// class representing a Node
class Node {
  // representing position of node
  NodePosn myPosn;

  // Neighboring nodes
  Node top;
  Node bottom;
  Node left;
  Node right;

  // does this node have paths to the top, bottom, left, right nodes?
  boolean pTop;
  boolean pBottom;
  boolean pLeft;
  boolean pRight;

  // is this the start node
  boolean startNode;
  // is this the end node
  boolean endNode;
  
  // is this the current node the player is on
  boolean pNode;
  // has the node been traveled on
  boolean traveled;
  // is this node part of the user's trace back
  boolean traceb;
  
  Node(int x, int y) {
    this.myPosn = new NodePosn(x, y);
    this.top = null;
    this.bottom = null;
    this.left = null;
    this.right = null;
    this.pTop = false;
    this.pBottom = false;
    this.pLeft = false;
    this.pRight = false;
    this.startNode = false;
    this.endNode = false;
    this.pNode = false;
    this.traveled = false;
    this.traceb = false;
  }
  
  Node(int x, int y, boolean isStart, boolean isEnd) {
    this.myPosn = new NodePosn(x, y);
    this.top = null;
    this.bottom = null;
    this.left = null;
    this.right = null;
    this.pTop = false;
    this.pBottom = false;
    this.pLeft = false;
    this.pRight = false;
    this.startNode = isStart;
    this.endNode = isEnd;
    this.pNode = false;
    this.traveled = false;
    this.traceb = false;
  }
  
  Node(int x, int y, Node top, Node bottom, Node left, Node right) {
    this.myPosn = new NodePosn(x, y);
    this.top = top;
    this.bottom = bottom;
    this.left = left;
    this.right = right;

    this.pTop = false;
    this.pBottom = false;
    this.pLeft = false;
    this.pRight = false;
    this.startNode = false;
    this.endNode = false;
    this.pNode = false;
    this.traveled = false;
    this.traceb = false;
  }
  
  // draws the current node
  WorldImage nodeImage(int size, boolean seeTraveled, boolean solution) {
    Color linec = new Color(15, 15, 15);
    Color cell = new Color(160, 160, 160);
    if (this.startNode) {
      cell = new Color(0, 100, 0);
    }
    if (this.endNode) {
      cell = new Color(116, 1, 113);
    }
    if (this.pNode && !this.endNode && !this.startNode) {
      cell = new Color(0, 102, 204);
    }
    if (this.traveled && seeTraveled && !this.pNode && !this.endNode && !this.startNode) {
      cell = new Color(102, 178, 255);
    }

    if (this.traceb && solution) {
      cell = new Color(0, 102, 204);
    }

    WorldImage cur = new RectangleImage(size, size, OutlineMode.SOLID, cell);

    if (!pTop) {
      cur = new OverlayImage(new LineImage(new Posn(size, 0), linec).movePinhole(0, size / 2), cur);
    }
    if (!pBottom) {
      cur = new OverlayImage(new LineImage(new Posn(size, 0), linec).movePinhole(0, -size / 2),
          cur);
    }
    if (!pLeft) {
      cur = new OverlayImage(new LineImage(new Posn(0, size), linec).movePinhole(size / 2, 0), cur);
    }
    if (!pRight) {
      cur = new OverlayImage(new LineImage(new Posn(0, size), linec).movePinhole(-size / 2, 0),
          cur);
    }
    return cur;
  }

  // updates the node
  void updateNodeHelp(Node to) {
    if (this.top == to) {
      this.pTop = true;
    }
    if (this.bottom == to) {
      this.pBottom = true;
    }
    if (this.left == to) {
      this.pLeft = true;
    }
    if (this.right == to) {
      this.pRight = true;
    }
  }

  // updates pNode boolean, is our player on this node
  public void updatePlayer(boolean b) {
    this.pNode = b;
  }
  
  // has this node been traveled
  public void updateTraveled(boolean b) {
    this.traveled = b;
  }
  
  public void updateTrace(boolean b) {
    this.traceb = b;
  }
}

// class representing an Edge
class Edge {
  // from node
  Node from;
  // to node
  Node to;
  // weight of the edge
  int weight;

  Edge(Node from, Node to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }
}

// class representing an edge comparator
class EdgeComparator implements Comparator<Edge> {
  // comparison method for two edges
  public int compare(Edge one, Edge two) {
    return one.weight - two.weight;
  }
}

class UnionFind {
  HashMap<NodePosn, NodePosn> map;

  UnionFind(HashMap<NodePosn, NodePosn> map) {
    this.map = map;
  }

  // unions the two node posns together
  void union(NodePosn one, NodePosn two) {
    map.put(this.find(one), this.find(two));
  }


  // returns the corresponding key for the given NodePosn
  NodePosn find(NodePosn that) {
    if (that.same(this.map.get(that))) {
      return that;
    }
    else {
      return find(this.map.get(that));
    }
  }
  
}

class Player {
  // current node the player is on
  Node cur;
  
  Player(Node cur) {
    this.cur = cur;
    this.cur.updatePlayer(true);
  }
  
  // updates this node based on the given direction
  void updateNode(String dir) {
    if (dir.equals("up")) {
      this.cur.updatePlayer(false);
      this.cur.updateTraveled(true);
      this.cur = this.cur.top;
      this.cur.updatePlayer(true);
    }
    if (dir.equals("down")) {
      this.cur.updatePlayer(false);
      this.cur.updateTraveled(true);
      this.cur = this.cur.bottom;
      this.cur.updatePlayer(true);
    }
    if (dir.equals("left")) {
      this.cur.updatePlayer(false);
      this.cur.updateTraveled(true);
      this.cur = this.cur.left;
      this.cur.updatePlayer(true);
    }
    if (dir.equals("right")) {
      this.cur.updatePlayer(false);
      this.cur.updateTraveled(true);
      this.cur = this.cur.right;
      this.cur.updatePlayer(true);
    }
  }
  
  // is the given direction a valid move
  boolean validMove(String dir) {
    boolean valid = false;
    if (dir.equals("up") && this.cur.pTop) {
      valid = true;
    }
    if (dir.equals("down") && this.cur.pBottom) {
      valid = true;
    }
    if (dir.equals("left") && this.cur.pLeft) {
      valid = true;
    }
    if (dir.equals("right") && this.cur.pRight) {
      valid = true;
    }
    return valid;
  }
  
  // is the player at the end
  boolean atEnd() {
    return this.cur.endNode;
  }
  
  // is the player at the start
  boolean atStart() {
    return this.cur.startNode;
  }
}

class MazeWorld extends World {
  
  // default constants of our game
  // game height
  static final int GHEIGHT = 60;
  // game width
  static final int GWIDTH = 100;
  // window height
  static final int WHEIGHT = 600;
  // window width
  static final int WWIDTH = 1000;
  
  int boardHeight;
  int boardWidth;
  int numNodes;

  int nodeSize;
  
  UnionFind unionFind;
  
  ArrayList<Node> maze;
  ArrayList<Edge> worklist;
  ArrayList<Edge> edges;

  Random rand;

  Node end;
  
  Player player;
  boolean seeTraveled;
  // has the player reached the end and is tracing back
  boolean traceBack;
  ArrayList<Node> trace;
  
  boolean endGame;
  boolean search;
  Deque<Node> searchlist;
  boolean manual;
  int correctMoves;
  int wrongMoves;
  
  MazeWorld() {
    this.boardHeight = GHEIGHT;
    this.boardWidth = GWIDTH;
    this.numNodes = GHEIGHT * GWIDTH;
    createNodes(this.boardWidth, this.boardHeight);
    this.unionFind = new UnionFind(this.nMap(this.maze));
    this.rand = new Random();
    this.worklist = createEdges();
    this.edges = createTree();
    updateNodes(this.edges);
    if (this.boardWidth >= this.boardHeight) {
      this.nodeSize = WWIDTH / this.boardWidth;
    } else {
      this.nodeSize = WHEIGHT / this.boardHeight;
    }
    if (this.boardWidth != 0 && this.boardHeight != 0) {
      this.player = new Player(this.maze.get(0));
    }
    this.seeTraveled = true;
    this.traceBack = false;
    this.trace = new ArrayList<Node>();
    this.endGame = false;
    this.search = false;
    this.manual = true;
    this.searchlist = new ArrayDeque<Node>();
    this.correctMoves = 0;
    this.wrongMoves = 0;
  }

  MazeWorld(int width, int height) {
    this.boardHeight = height;
    this.boardWidth = width;
    this.numNodes = this.boardHeight * this.boardWidth;
    
    if (width != 0 && height != 0) {
      if (this.boardWidth > this.boardHeight) {
        this.nodeSize = WWIDTH / this.boardWidth;
      } else {
        this.nodeSize = WHEIGHT / this.boardHeight;
      }
    }
 
    createNodes(this.boardWidth, this.boardHeight);
    this.unionFind = new UnionFind(this.nMap(this.maze));
    this.rand = new Random();
    this.worklist = createEdges();
    this.edges = createTree();
    updateNodes(this.edges);
    if (this.boardWidth != 0 && this.boardHeight != 0) {
      this.player = new Player(this.maze.get(0));
    }
    this.seeTraveled = true;
    this.traceBack = false;
    this.trace = new ArrayList<Node>();
    this.endGame = false;
    this.search = false;
    this.manual = true;
    this.searchlist = new ArrayDeque<Node>();
    this.correctMoves = 0;
    this.wrongMoves = 0;
  }
 
  // lays out the nodes row by row
  void createNodes(int width, int height) {
    maze = new ArrayList<Node>();
    for (int i = 0; i < height; ++i) {
      for (int j = 0; j < width; ++j) {
        if (i == 0 && j == 0) {
          maze.add(new Node(0, 0, true, false));
        }
        else if (i == height - 1 && j == width - 1) {
          maze.add(new Node(j, i, false, true));
        }
        else {
          maze.add(new Node(j, i));
        }
      }
    }
    linkNodes(width, height);
  }

  // links the individual nodes to its neighbors
  void linkNodes(int width, int height) {
    for (int i = 0; i < this.maze.size(); ++i) {
      Node cur = maze.get(i);
      // links the left cell
      if (cur.myPosn.x == 0) {
        cur.left = null;
      }
      else {
        cur.left = maze.get(i - 1);
      }
      // links the right node
      if (cur.myPosn.x == width - 1) {
        cur.right = null;
      }
      else {
        cur.right = maze.get(i + 1);
      }
      // links the top node
      if (cur.myPosn.y == 0) {
        cur.top = null;
      }
      else {
        cur.top = maze.get(i - width);
      }
      // links the bottom node
      if (cur.myPosn.y == height - 1) {
        cur.bottom = null;
      }
      else {
        cur.bottom = maze.get(i + width);
      }
    }
  }

  // worklist
  // creates edges for walls of our maze and sorts them by weight
  ArrayList<Edge> createEdges() {
    ArrayList<Edge> list = new ArrayList<Edge>();
    Node cur;

    for (int i = 0; i < maze.size(); ++i) {
      cur = maze.get(i);
      // top to bottom
      if (cur.myPosn.y < this.boardHeight - 1) {
        list.add(new Edge(cur, cur.bottom, this.rand.nextInt(1000)));
      }

      if (cur.myPosn.x < this.boardWidth - 1) {
        list.add(new Edge(cur, cur.right, this.rand.nextInt(1000)));
      }
    }

    Collections.sort(list, new EdgeComparator());
    return list;
  }
  
  // maps each node to itself
  HashMap<NodePosn, NodePosn> nMap(ArrayList<Node> nodes) {
    HashMap<NodePosn, NodePosn> map = new HashMap<NodePosn, NodePosn>();
    if (nodes.size() != 0) {
      for (int i = 0; i < nodes.size(); ++i) {
        map.put(nodes.get(i).myPosn, nodes.get(i).myPosn);
      }
    }
    return map;
  }

  /*
  HashMap<String, String> representatives;
  List<Edge> edgesInTree;
  List<Edge> worklist = all edges in graph, sorted by edge weights;
   
  initialize every node's representative to itself
  While(there's more than one tree)
    Pick the next cheapest edge of the graph: suppose it connects X and Y.
    If find(representatives, X) same find(representatives, Y):
      discard this edge  // they're already connected
    Else:
      Record this edge in edgesInTree
      union(representatives,
            find(representatives, X),
            find(representatives, Y))
  Return the edgesInTree
  
  
  algorithm Kruskal(G) is
    F:= ∅
    for each v ∈ G.V do
        MAKE-SET(v)
    for each (u, v) in G.E ordered by weight(u, v), increasing do
        if FIND-SET(u) ≠ FIND-SET(v) then
            F:= F ∪ {(u, v)}
            UNION(FIND-SET(u), FIND-SET(v))
    return F
  */
  
  // creates the edges
  ArrayList<Edge> createTree() {
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    int i = 0;
    Edge curEdge;
    while (edgesInTree.size() < this.numNodes && i < this.worklist.size()) {
      curEdge = this.worklist.get(i);
      NodePosn from = this.unionFind.find(curEdge.from.myPosn);
      NodePosn to = this.unionFind.find(curEdge.to.myPosn);
      if (from.same(to)) {
        // discard
      } else {
        edgesInTree.add(curEdge);
        this.unionFind.union(from, to);
      }
      i++;
    }
    return edgesInTree;
  }
  

  // updates the node paths according to the given arraylist of edges
  void updateNodes(ArrayList<Edge> edges) {
    for (int i = 0; i < edges.size(); ++i) {
      Node from = edges.get(i).from;
      Node to = edges.get(i).to;
      from.updateNodeHelp(to);
      to.updateNodeHelp(from);
    }
  }
  
  // draws our maze
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(WWIDTH, WHEIGHT);
    for (Node c : maze) {
      // draws each cell
      scene.placeImageXY(c.nodeImage(this.nodeSize, this.seeTraveled, this.traceBack),
          c.myPosn.x * this.nodeSize + (nodeSize / 2),
          c.myPosn.y * this.nodeSize + (this.nodeSize / 2));
    }
    scene.placeImageXY(
        new TextImage("correct moves: " + this.correctMoves, this.nodeSize / 2, Color.RED),
        this.WWIDTH - this.WWIDTH / 5, this.WHEIGHT / 10);
    scene.placeImageXY(
        new TextImage("incorrect moves: " + this.wrongMoves, this.nodeSize / 2, Color.RED),
        this.WWIDTH - this.WWIDTH / 5, this.WHEIGHT / 5);
    if (endGame) {
      scene.placeImageXY(new TextImage("Maze Solved!", this.WWIDTH / 10, Color.RED),
          this.WWIDTH / 2, this.WHEIGHT / 2 - this.WHEIGHT / 5);
      scene.placeImageXY(new TextImage("Press 'r' to restart", this.WWIDTH / 10, Color.RED),
          this.WWIDTH / 2, this.WHEIGHT / 2 + this.WHEIGHT / 5);
    }
    return scene;

  }
  
  // on key event, changes the game accordingly when player presses a button on
  // their keyboard
  public void onKeyEvent(String k) {
    if (!this.endGame) {
      // arrow key input
      if ((k.equals("up") || k.equals("down") || k.equals("left") || k.equals("right"))
          && player.validMove(k) && !this.search) {
        if (!this.trace.contains(this.player.cur) && this.traceBack) {
          this.player.cur.updateTrace(true);
          // this.trace.add(this.player.cur);
        }
        this.player.updateNode(k);
        if (!player.cur.traveled) {
          this.trackMoves(this.player.cur,
              this.shortestPath(this.maze.get(0), this.maze.get(this.numNodes - 1)));
        }
        if (!this.trace.contains(this.player.cur) && this.traceBack) {
          this.player.cur.updateTrace(true);
        }
      }
    }
   
    // p button, turns on/off visited paths
    if (k.equals("p") && this.seeTraveled) {
      this.seeTraveled = false;
    }
    else if (k.equals("p") && !this.seeTraveled) {
      this.seeTraveled = true;
    }

    // n, creates a new game
    if (k.equals("r")) {
      createNodes(this.boardWidth, this.boardHeight);
      this.unionFind = new UnionFind(this.nMap(this.maze));
      this.rand = new Random();
      this.worklist = createEdges();
      this.edges = createTree();
      updateNodes(this.edges);
      if (this.boardWidth != 0 && this.boardHeight != 0) {
        this.player = new Player(this.maze.get(0));
      }
      this.searchlist = new ArrayDeque<Node>();
      this.traceBack = false;
      this.trace = new ArrayList<Node>();
      this.endGame = false;
      this.search = false;
      this.correctMoves = 0;
      this.wrongMoves = 0;
    }
    
    if (k.equals("b")) {
      this.resetTraveled();
      this.endGame = false;
      this.searchlist = this.createPath(maze.get(0), maze.get(numNodes - 1), new Queue<Node>());
      this.search = true;
      this.manual = false;
      this.traceBack = false;
      this.shortestPath(maze.get(0), maze.get(numNodes - 1));
    }

    if (k.equals("d")) {
      this.resetTraveled();
      this.endGame = false;
      this.searchlist = this.createPath(maze.get(0), maze.get(numNodes - 1), new Stack<Node>());
      this.search = true;
      this.manual = false;
      this.traceBack = false;
      this.shortestPath(maze.get(0), maze.get(numNodes - 1));
    }
    
    if (k.equals("m") && this.manual) {
      this.manual = false;
      this.shortestPath(maze.get(0), maze.get(numNodes - 1));
    }
    else if (k.equals("m") && !this.manual) {
      this.manual = true;
    }
  }
  
  // tracks the correct moves/incorrect moves based on the path given
  void trackMoves(Node cur, ArrayList<Node> path) { 
    if (path.contains(cur)) {
      this.correctMoves++;
    } else {
      this.wrongMoves++;
    }
  }
  
  // updates the world per tick
  public void onTick() {
    if (this.player.atEnd() && this.manual) {
      this.traceBack = true;
    } else if (this.player.atEnd() && !this.manual) {
      this.traceBack = true;
      this.endGame = true;
    }

    if (this.traceBack && this.player.atStart() && !this.search) {
      this.endGame = true;
    }
    
    
    // prints the result of breath first or depth first search 
    // depending on which one the player selected
    // to change the speed of the animation, change the big bang tick speed
    if (this.search && this.searchlist.size() > 0) {
      Node next = this.searchlist.remove();
      next.updateTraveled(true);
    }
    
    // when the bfs/dfs result finishes animating, shows shortest route
    // and displays end game screen
    if (this.search && this.searchlist.size() == 0) {
      this.traceBack = true;
      this.endGame = true;
    }
  }

  // resets the traveled nodes
  void resetTraveled() {
    for (Node n : this.maze) {
      if (n.traveled) {
        n.traveled = false;
      }
    }
  }
  
  // searches for to Node to from Node from, with the given search method
  // breath first search(queue) or depth first search (stack)
  Deque<Node> createPath(Node from, Node to, ICollection<Node> searchlist) {
    Deque<Node> alreadySeen = new ArrayDeque<Node>();

    // Initialize the searchlist with the from Node
    searchlist.add(from);
    // As long as the searchlist isn't empty...
    while (searchlist.size() > 0) {
      Node next = searchlist.remove();
      if (next.equals(to)) {
        return alreadySeen;
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        /*
         * for (Edge e : next.outEdges) { searchlist.add(e.to); }
         */
        if (next.pTop) {
          searchlist.add(next.top);
        }
        if (next.pBottom) {
          searchlist.add(next.bottom);
        }
        if (next.pLeft) {
          searchlist.add(next.left);
        }
        if (next.pRight) {
          searchlist.add(next.right);
        }
        alreadySeen.add(next);
      }
    }
    // We haven't found the to node, and there are no more to try
    return alreadySeen;
  }
  
  // finds the shortest path from the source to the target
  ArrayList<Node> shortestPath(Node source, Node target) {
    ArrayList<Node> unvisited = new ArrayList<Node>();
    HashMap<Node, Integer> distances = new HashMap<Node, Integer>();
    HashMap<Node, Node> predecessors = new HashMap<Node, Node>();
    
    unvisited.add(source);
    distances.put(source, 0);
    
    while (unvisited.size() > 0) {
      Node v = unvisited.remove(0);
      
      /*
      if (v.pTop) {
        int weight = this.findEdge(v, v.top).weight;
        if (distances.get(v.top) == null || distances.get(v.top) > distances.get(v) + weight) {
          distances.put(v.top, distances.get(v) + weight);
          predecessors.put(v.top, v);
          unvisited.add(v.top);
        }
      }
      if (v.pBottom) {
        int weight = this.findEdge(v, v.bottom).weight;
        if (distances.get(v.bottom) == null
            || distances.get(v.bottom) > distances.get(v) + weight) {
          distances.put(v.bottom, distances.get(v) + weight);
          predecessors.put(v.bottom, v);
          unvisited.add(v.bottom);
        }
      }
      if (v.pLeft) {
        int weight = this.findEdge(v, v.left).weight;
        if (distances.get(v.left) == null || distances.get(v.left) > distances.get(v) + weight) {
          distances.put(v.left, distances.get(v) + weight);
          predecessors.put(v.left, v);
          unvisited.add(v.left);
        }
      }
      if (v.pRight) {
        int weight = this.findEdge(v, v.right).weight;
        if (distances.get(v.right) == null || distances.get(v.right) > distances.get(v) + weight) {
          distances.put(v.right, distances.get(v) + weight);
          predecessors.put(v.right, v);
          unvisited.add(v.right);
        }
      }
       */

      if (v.pTop) {
        int weight = this.findEdge(v, v.top).weight;
        shortestPathHelp(v, v.top, weight, unvisited, distances, predecessors);
      }

      if (v.pBottom) {
        int weight = this.findEdge(v, v.bottom).weight;
        shortestPathHelp(v, v.bottom, weight, unvisited, distances, predecessors);
      }

      if (v.pLeft) {
        int weight = this.findEdge(v, v.left).weight;
        shortestPathHelp(v, v.left, weight, unvisited, distances, predecessors);
      }

      if (v.pRight) {
        int weight = this.findEdge(v, v.right).weight;
        shortestPathHelp(v, v.right, weight, unvisited, distances, predecessors);
      }

    }
    /*
     * if (distances.get(target) == null) { return -1; } else { return
     * distances.get(target); }
     */

    ArrayList<Node> path = new ArrayList<Node>();

    Node step = target;

    if (predecessors.get(step) == null) {
      return path;
    }

    path.add(step);
    step.updateTrace(true);

    while (step != source) {
      step = predecessors.get(step);
      path.add(0, step);
      if (!this.manual) {
        step.updateTrace(true);
      }
    }

    return path;

  }

  // helper function to simplifly code in shortest path
  void shortestPathHelp(Node og, Node dir, int weight, ArrayList<Node> unvisited,
      HashMap<Node, Integer> distances, HashMap<Node, Node> predecessors) {
    if (distances.get(dir) == null || distances.get(dir) > distances.get(og) + weight) {
      distances.put(dir, distances.get(og) + weight);
      predecessors.put(dir, og);
      unvisited.add(dir);
    }
  }

  // finds the edge that nodes one and two share
  Edge findEdge(Node one, Node two) {
    for (Edge e : this.edges) {
      if (e.from.equals(one) && e.to.equals(two) || e.from.equals(two) && e.to.equals(one)) {
        return e;
      }
    }
    throw new NoSuchElementException("this edge doesnt exist");
  }

}

// An ICollection is one of
// - A Queue
// - A Stack
interface ICollection<T> {
  // Adds an item to this ICollection
  void add(T item);

  // Removes an item from this ICollection
  T remove();

  // Returns the size of this ICollection
  int size();
}

// Describes a Queue
// Used in Breadth-first Search
class Queue<T> implements ICollection<T> {
  Deque<T> items;

  Queue() {
    this.items = new ArrayDeque<T>();
  }

  // Adds an item to this Queue
  public void add(T item) {
    this.items.addLast(item);
  }

  // Removes an item from this Queue
  public T remove() {
    return this.items.removeFirst();
  }

  // Returns the size of this Queue
  public int size() {
    return this.items.size();
  }
}

// Describes a Stack
// Used in Depth-first Search
class Stack<T> implements ICollection<T> {
  Deque<T> items;

  Stack() {
    this.items = new ArrayDeque<T>();
  }

  // Adds an item to a Stack
  public void add(T item) {
    this.items.addFirst(item);
  }

  // Removes and item to a Stack
  public T remove() {
    return this.items.removeFirst();
  }

  // Returns the size of this Stack
  public int size() {
    return this.items.size();
  }
}

class ExamplesMaze {

  // three by four (3x4) maze
  MazeWorld tbf = new MazeWorld(3, 4);
  // two by two (2x2) maze
  MazeWorld tbt = new MazeWorld(2, 2);
  // two by one (2x1) maze
  MazeWorld tbo = new MazeWorld(2, 1);
  // zero by zero (0x0) maze
  MazeWorld zbz = new MazeWorld(0, 0);

  NodePosn nodeposn1;
  NodePosn nodeposn2;
  NodePosn nodeposn3;
  NodePosn nodeposn4;
  NodePosn nodeposn5;

  Node node1;
  Node node2;
  Node node3;
  Node node4;

  Edge edge1;
  Edge edge2;
  Edge edge3;

  EdgeComparator edgec;

  ArrayList<Edge> edgesInTree1;

  HashMap<NodePosn, NodePosn> hash1;
  HashMap<NodePosn, NodePosn> hash2;
  HashMap<NodePosn, NodePosn> hashE;

  UnionFind map1;

  ArrayList<Node> maze1;
  ArrayList<Node> mazeE;

  Queue<Node> queue1;
  Queue<Node> queue2;

  Stack<Node> stack1;
  Stack<Node> stack2;

  void initData() {

    nodeposn1 = new NodePosn(0, 0);
    nodeposn2 = new NodePosn(1, 0);
    nodeposn3 = new NodePosn(0, 1);
    nodeposn4 = new NodePosn(1, 1);
    nodeposn5 = new NodePosn(0, 2);

    node1 = new Node(0, 0, true, false);
    node2 = new Node(1, 0, false, false);
    node3 = new Node(0, 1, false, false);
    node4 = new Node(1, 1, false, true);

    edge1 = new Edge(node1, node2, 5);
    edge2 = new Edge(node1, node3, 21);
    edge3 = new Edge(node2, node4, 2);

    edgesInTree1 = new ArrayList<Edge>();
    edgesInTree1.add(edge1);
    edgesInTree1.add(edge2);
    edgesInTree1.add(edge3);

    edgec = new EdgeComparator();

    hash1 = new HashMap<NodePosn, NodePosn>();
    hash2 = new HashMap<NodePosn, NodePosn>();
    hashE = new HashMap<NodePosn, NodePosn>();

    // maps every node to itself
    hash1.put(nodeposn1, nodeposn1);
    hash1.put(nodeposn2, nodeposn2);
    hash1.put(nodeposn3, nodeposn3);
    hash1.put(nodeposn4, nodeposn4);

    hash2.put(nodeposn1, nodeposn2);
    hash2.put(nodeposn1, nodeposn3);
    hash2.put(nodeposn2, nodeposn4);

    map1 = new UnionFind(hash1);

    mazeE = new ArrayList<Node>();
    maze1 = new ArrayList<Node>();
    maze1.add(node1);
    maze1.add(node2);
    maze1.add(node3);
    maze1.add(node4);
  }

  // altered Data
  void alteredData() {
    node1.right = node2;
    node1.bottom = node3;
    node2.left = node1;
    node2.bottom = node4;
    node3.right = node4;
    node3.top = node2;
    node4.left = node3;
    node4.top = node2;

    // simulates a linked node
    hash1.put(nodeposn5, nodeposn4);

    edge1.weight = 1;
    edge2.weight = 2;
    edge3.weight = 3;

  }

  void testsame(Tester t) {
    initData();
    alteredData();

    t.checkExpect(nodeposn1.same(nodeposn2), false);
    t.checkExpect(nodeposn1.same(nodeposn3), false);
    t.checkExpect(nodeposn1.same(nodeposn1), true);
    t.checkExpect(nodeposn2.same(nodeposn2), true);
    
    // when given type isnt a nodeposn
    t.checkExpect(nodeposn2.same(node1), false);
  }

  void testUpdateNodeHelp(Tester t) {
    initData();
    alteredData();

    t.checkExpect(node1.pTop, false);

    node1.updateNodeHelp(node2);

    t.checkExpect(node1.pTop, false);
    t.checkExpect(node1.pRight, true);

    t.checkExpect(node2.pLeft, false);
    node2.updateNodeHelp(node1);
    t.checkExpect(node2.pLeft, true);

  }

  void testUpdatePlayer(Tester t) {
    initData();
    t.checkExpect(this.node2.pNode, false);

    this.node2.updatePlayer(true);
    t.checkExpect(this.node2.pNode, true);

    // if boolean given is the same, no change
    this.node2.updatePlayer(true);
    t.checkExpect(this.node2.pNode, this.node2.pNode);

    this.node2.updatePlayer(false);
    t.checkExpect(this.node2.pNode, false);
  }

  void testUpdateTraveled(Tester t) {
    initData();
    t.checkExpect(this.node2.traveled, false);

    this.node2.updateTraveled(true);
    t.checkExpect(this.node2.traveled, true);

    // if boolean given is the same, no change
    this.node2.updateTraveled(true);
    t.checkExpect(this.node2.traveled, this.node2.traveled);

    this.node2.updateTraveled(false);
    t.checkExpect(this.node2.traveled, false);
  }

  void testUpdateTrace(Tester t) {
    initData();
    t.checkExpect(this.node2.traceb, false);

    this.node2.updateTrace(true);
    t.checkExpect(this.node2.traceb, true);

    // if boolean given is the same, no change
    this.node2.updateTrace(true);
    t.checkExpect(this.node2.traceb, this.node2.traceb);

    this.node2.updateTrace(false);
    t.checkExpect(this.node2.traceb, false);
  }

  void testCompare(Tester t) {
    initData();

    t.checkExpect(edgec.compare(edge1, edge2), -16);
    t.checkExpect(edgec.compare(edge1, edge3), 3);
    t.checkExpect(edgec.compare(edge3, edge1), -3);
  }

  void testFind(Tester t) {
    initData();
    alteredData();

    t.checkExpect(map1.find(nodeposn1), nodeposn1);
    t.checkExpect(map1.find(nodeposn4), nodeposn5);
    t.checkExpect(map1.find(nodeposn5), nodeposn4);
  }

  void testUnion(Tester t) {
    initData();

    t.checkExpect(map1.find(nodeposn1), nodeposn1);
    t.checkExpect(map1.find(nodeposn2), nodeposn2);
    map1.union(nodeposn1, nodeposn2);
    t.checkExpect(map1.find(nodeposn1), nodeposn2);
    t.checkExpect(map1.find(nodeposn2), nodeposn1);
  }

  void testCreateNodes(Tester t) {
    initData();

    t.checkExpect(tbf.maze.size(), 12);

    this.tbf.createNodes(10, 10);
    t.checkExpect(tbf.maze.size(), 100);
  }

  void testNodeImage(Tester t) {
    initData();
    alteredData();

    Color linec = new Color(15, 15, 15);
    Color cell = new Color(160, 160, 160);
    Color startc = new Color(0, 100, 0);
    Color endc = new Color(116, 1, 113);

    // Image of Node 1: start cell with no paths
    WorldImage ImageN1 = new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(5, 0),
            new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, -5),
                new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
                    new RectangleImage(10, 10, OutlineMode.SOLID, startc)))));

    // start cell with paths to node2(right) and node3(bottom)
    WorldImage ImageN1v2 = new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(5, 0),
        new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
            new RectangleImage(10, 10, OutlineMode.SOLID, startc)));

    // Image of Node 2: normal cell with no paths
    WorldImage ImageN2 = new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(5, 0),
            new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, -5),
                new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
                    new RectangleImage(10, 10, OutlineMode.SOLID, cell)))));

    // normal cell with paths to node1(left) and node4(bottom)
    WorldImage ImageN2v2 = new OverlayImage(
        new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
            new RectangleImage(10, 10, OutlineMode.SOLID, cell)));

    // Image of Node 4, end cell with no paths
    WorldImage ImageN4 = new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(5, 0),
            new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, -5),
                new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
                    new RectangleImage(10, 10, OutlineMode.SOLID, endc)))));

    // end cell with path to node2(top)
    WorldImage ImageN4v2 = new OverlayImage(
        new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(0, 10), linec).movePinhole(5, 0),
            new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, -5),
                new RectangleImage(10, 10, OutlineMode.SOLID, endc))));

    // the start cell with no paths
    t.checkExpect(node1.nodeImage(10, true, false), ImageN1);
    node1.updateNodeHelp(node2);
    node1.updateNodeHelp(node3);
    // start cell with paths to node2(right) and node3(bottom)
    t.checkExpect(node1.nodeImage(10, true, false), ImageN1v2);

    // normal cell with no path
    t.checkExpect(node2.nodeImage(10, true, false), ImageN2);
    node2.updateNodeHelp(node1);
    node2.updateNodeHelp(node4);
    // normal cell with paths to node1(left) and node4(bottom)
    t.checkExpect(node2.nodeImage(10, true, false), ImageN2v2);

    // end cell with no paths
    t.checkExpect(node4.nodeImage(10, true, false), ImageN4);
    node4.updateNodeHelp(node2);
    // end cell with path to node2(top)
    t.checkExpect(node4.nodeImage(10, true, false), ImageN4v2);

    // added tests for new graphics (part 2)

    WorldImage ImageN2v3 = new OverlayImage(
        new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
            new RectangleImage(10, 10, OutlineMode.SOLID, new Color(102, 178, 255))));

    WorldImage ImageN2v4 = new OverlayImage(
        new LineImage(new Posn(0, 10), linec).movePinhole(-5, 0),
        new OverlayImage(new LineImage(new Posn(10, 0), linec).movePinhole(0, 5),
            new RectangleImage(10, 10, OutlineMode.SOLID, new Color(0, 102, 204))));

    // player is currently on this node
    this.node2.pNode = true;
    t.checkExpect(this.node2.nodeImage(10, true, false), ImageN2v4);
    // player has moved off of this node
    this.node2.pNode = false;
    // the current node has been traveled on
    this.node2.traveled = true;
    t.checkExpect(node2.nodeImage(10, true, false), ImageN2v3);
    // player has disabled the viewing of traveled nodes
    t.checkExpect(node2.nodeImage(10, false, false), ImageN2v2);
    // player has finished the game and is tracing their way back to the beginning
    this.node2.traceb = true;
    t.checkExpect(node2.nodeImage(10, true, true), ImageN2v4);
  }

  void testLinkCells(Tester t) {
    initData();
    tbt.maze = maze1;
    t.checkExpect(node1.right, null);
    t.checkExpect(node1.bottom, null);
    t.checkExpect(node1.left, null);
    t.checkExpect(node1.top, null);
    t.checkExpect(node2.left, null);
    t.checkExpect(node2.bottom, null);
    t.checkExpect(node2.right, null);
    t.checkExpect(node2.top, null);
    t.checkExpect(node3.right, null);
    t.checkExpect(node3.top, null);
    t.checkExpect(node3.left, null);
    t.checkExpect(node3.bottom, null);
    t.checkExpect(node4.left, null);
    t.checkExpect(node4.top, null);
    t.checkExpect(node4.right, null);
    t.checkExpect(node4.bottom, null);

    alteredData();
    tbt.linkNodes(2, 2);
    t.checkExpect(node1.right, node2);
    t.checkExpect(node1.bottom, node3);
    t.checkExpect(node1.left, null);
    t.checkExpect(node1.top, null);
    t.checkExpect(node2.left, node1);
    t.checkExpect(node2.bottom, node4);
    t.checkExpect(node2.right, null);
    t.checkExpect(node2.top, null);
    t.checkExpect(node3.right, node4);
    t.checkExpect(node3.top, node1);
    t.checkExpect(node3.left, null);
    t.checkExpect(node3.bottom, null);
    t.checkExpect(node4.left, node3);
    t.checkExpect(node4.top, node2);
    t.checkExpect(node4.right, null);
    t.checkExpect(node4.bottom, null);
  }

  // method to test if list of edges is sorted in ascending order by weight
  boolean isAscending(ArrayList<Edge> edges) {
    boolean ascending = true;
    for (int i = 1; i < edges.size(); ++i) {
      ascending = ascending && (edges.get(i).weight >= edges.get(i - 1).weight);
    }
    return ascending;
  }

  void testIsAscending(Tester t) {
    initData();
    t.checkExpect(isAscending(edgesInTree1), false);

    alteredData();
    t.checkExpect(isAscending(edgesInTree1), true);
  }

  void testCreateEdges(Tester t) {
    initData();

    tbt.maze = maze1;
    tbt.updateNodes(edgesInTree1);
    tbt.createEdges();

    t.checkExpect(tbt.worklist.size(), 4);
    t.checkExpect(isAscending(tbt.worklist), true);
  }

  void testNMap(Tester t) {
    initData();

    t.checkExpect(tbt.nMap(maze1), hash1);
    t.checkExpect(tbt.nMap(mazeE), hashE);
  }

  void testMakeScene(Tester t) {
    Color linec = new Color(15, 15, 15);
    Color startc = new Color(0, 100, 0);
    Color endc = new Color(116, 1, 113);
    WorldScene scene = new WorldScene(1000, 600);

    t.checkExpect(zbz.makeScene(), scene);

    WorldImage ImageN1 = new OverlayImage(
        new LineImage(new Posn(0, 500), linec).movePinhole(250, 0),
        new OverlayImage(new LineImage(new Posn(500, 0), linec).movePinhole(0, -250),
            new OverlayImage(new LineImage(new Posn(500, 0), linec).movePinhole(0, 250),
                new RectangleImage(500, 500, OutlineMode.SOLID, startc))));

    WorldImage ImageN2 = new OverlayImage(
        new LineImage(new Posn(0, 500), linec).movePinhole(-250, 0),
        new OverlayImage(new LineImage(new Posn(500, 0), linec).movePinhole(0, -250),
            new OverlayImage(new LineImage(new Posn(500, 0), linec).movePinhole(0, 250),
                new RectangleImage(500, 500, OutlineMode.SOLID, endc))));

    scene.placeImageXY(ImageN1, 250, 250);
    scene.placeImageXY(ImageN2, 1 * 500 + 250, 250);

    scene.placeImageXY(new TextImage("correct moves: 0", 250, Color.RED), 800, 60);
    scene.placeImageXY(new TextImage("incorrect moves: 0", 250, Color.RED), 800, 120);
    t.checkExpect(tbo.makeScene(), scene);

    tbo.endGame = true;

    scene.placeImageXY(new TextImage("Maze Solved!", 1000 / 10, Color.RED), 1000 / 2,
        600 / 2 - 600 / 5);
    scene.placeImageXY(new TextImage("Press 'r' to restart", 1000 / 10, Color.RED), 1000 / 2,
        600 / 2 + 600 / 5);

    t.checkExpect(tbo.makeScene(), scene);
  }

  void testUpdateNodes(Tester t) {
    initData();

    tbt.maze = maze1;
    t.checkExpect(node1.pRight, false);
    t.checkExpect(node1.pBottom, false);
    t.checkExpect(node1.pLeft, false);
    t.checkExpect(node1.pTop, false);
    t.checkExpect(node2.pLeft, false);
    t.checkExpect(node2.pBottom, false);
    t.checkExpect(node2.pRight, false);
    t.checkExpect(node2.pTop, false);
    t.checkExpect(node3.pRight, false);
    t.checkExpect(node3.pTop, false);
    t.checkExpect(node3.pLeft, false);
    t.checkExpect(node3.pBottom, false);
    t.checkExpect(node4.pLeft, false);
    t.checkExpect(node4.pTop, false);
    t.checkExpect(node4.pRight, false);
    t.checkExpect(node4.pBottom, false);

    alteredData();
    tbt.updateNodes(edgesInTree1);

    t.checkExpect(node1.pRight, true);
    t.checkExpect(node1.pBottom, true);
    t.checkExpect(node1.pLeft, false);
    t.checkExpect(node1.pTop, false);
    t.checkExpect(node2.pLeft, true);
    t.checkExpect(node2.pBottom, true);
    t.checkExpect(node2.pRight, false);
    t.checkExpect(node2.pTop, false);
    t.checkExpect(node3.pRight, false);
    t.checkExpect(node3.pTop, false);
    t.checkExpect(node3.pLeft, false);
    t.checkExpect(node3.pBottom, false);
    t.checkExpect(node4.pLeft, false);
    t.checkExpect(node4.pTop, true);
    t.checkExpect(node4.pRight, false);
    t.checkExpect(node4.pBottom, false);
  }

  void initWorklist() {
    queue1 = new Queue<Node>();
    queue2 = new Queue<Node>();

    stack1 = new Stack<Node>();
    stack2 = new Stack<Node>();
  }

  void testQueueAdd(Tester t) {
    initData();
    initWorklist();
    this.queue1.add(this.node1);
    t.checkExpect(this.queue1.items.getLast(), this.node1);

    this.queue1.add(this.node2);
    t.checkExpect(this.queue1.items.getLast(), this.node2);

    this.queue1.add(this.node3);
    t.checkExpect(this.queue1.items.getLast(), this.node3);

    t.checkExpect(this.queue1.items.getFirst(), this.node1);
  }

  void testQueueRemove(Tester t) {
    initData();
    initWorklist();
    this.queue1.add(this.node1);
    this.queue1.add(this.node2);
    this.queue1.add(this.node3);

    t.checkExpect(this.queue1.remove(), node1);
    t.checkExpect(this.queue1.remove(), node2);
    t.checkExpect(this.queue1.remove(), node3);
    t.checkExpect(this.queue1.items.size(), 0);
  }

  void testQueueSize(Tester t) {
    initData();
    initWorklist();
    this.queue1.add(this.node1);
    t.checkExpect(this.queue1.size(), 1);

    this.queue1.add(this.node2);
    t.checkExpect(this.queue1.size(), 2);

    this.queue1.add(this.node3);
    t.checkExpect(this.queue1.size(), 3);

    this.queue1.remove();
    t.checkExpect(this.queue1.size(), 2);
  }

  void testStackAdd(Tester t) {
    initData();
    initWorklist();
    this.stack1.add(this.node1);
    t.checkExpect(this.stack1.items.getFirst(), this.node1);

    this.stack1.add(this.node2);
    t.checkExpect(this.stack1.items.getFirst(), this.node2);

    this.stack1.add(this.node3);
    t.checkExpect(this.stack1.items.getFirst(), this.node3);

    t.checkExpect(this.stack1.items.getLast(), this.node1);
  }

  void testStackRemove(Tester t) {
    initData();
    initWorklist();
    this.stack1.add(this.node1);
    this.stack1.add(this.node2);
    this.stack1.add(this.node3);

    t.checkExpect(this.stack1.remove(), node3);
    t.checkExpect(this.stack1.remove(), node2);
    t.checkExpect(this.stack1.remove(), node1);
    t.checkExpect(this.stack1.items.size(), 0);
  }

  void testStackSize(Tester t) {
    initData();
    initWorklist();
    this.stack1.add(this.node1);
    t.checkExpect(this.stack1.size(), 1);

    this.stack1.add(this.node2);
    t.checkExpect(this.stack1.size(), 2);

    this.stack1.add(this.node3);
    t.checkExpect(this.stack1.size(), 3);

    this.stack1.remove();
    t.checkExpect(this.stack1.size(), 2);
  }

  void testResetTraveled(Tester t) {
    initData();
    node1.traveled = true;
    node2.traveled = true;
    node3.traveled = true;
    node4.traveled = false;

    t.checkExpect(node1.traveled, true);
    t.checkExpect(node2.traveled, true);
    t.checkExpect(node3.traveled, true);
    t.checkExpect(node4.traveled, false);
    tbt.maze = maze1;
    tbt.resetTraveled();
    t.checkExpect(node1.traveled, false);
    t.checkExpect(node2.traveled, false);
    t.checkExpect(node3.traveled, false);
    t.checkExpect(node4.traveled, false);
  }

  void testFindEdge(Tester t) {
    initData();
    tbt.maze = this.maze1;
    tbt.edges = this.edgesInTree1;
    t.checkExpect(tbt.findEdge(node1, node2), this.edge1);
    t.checkExpect(tbt.findEdge(node1, node3), this.edge2);

    t.checkException(new NoSuchElementException("this edge doesnt exist"), tbt, "findEdge", node1,
        node4);

  }

  void testOnKey(Tester t) {
    initData();
    alteredData();

    tbt.maze = this.maze1;
    tbt.edges = this.edgesInTree1;
    // player movements
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(0, 0));
    tbt.onKeyEvent("right");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(1, 0));
    tbt.onKeyEvent("down");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(1, 1));
    // tried to move left but there is a wall, no change in position
    tbt.onKeyEvent("left");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(1, 1));
    tbt.onKeyEvent("up");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(1, 0));
    tbt.onKeyEvent("left");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(0, 0));

    // turning visable paths on and off
    t.checkExpect(tbt.seeTraveled, true);
    tbt.onKeyEvent("p");
    t.checkExpect(tbt.seeTraveled, false);
    tbt.onKeyEvent("p");
    t.checkExpect(tbt.seeTraveled, true);
    
    MazeWorld test = new MazeWorld(5, 5);
    t.checkExpect(test.search, false);
    t.checkExpect(test.searchlist.size(), 0);
    
    test.onKeyEvent("m");
    t.checkExpect(test.manual, false);
    test.onKeyEvent("m");
    t.checkExpect(test.manual, true);
    
    // breath first search
    test.onKeyEvent("b");
    t.checkExpect(test.searchlist,
        test.createPath(test.maze.get(0), test.maze.get(test.maze.size() - 1), new Queue<Node>()));
    t.checkExpect(test.search, true);
    t.checkExpect(test.manual, false);
    t.checkExpect(test.traceBack, false);
    t.checkExpect(test.endGame, false);

    MazeWorld test2 = new MazeWorld(5, 5);
    t.checkExpect(test2.search, false);
    t.checkExpect(test2.searchlist.size(), 0);
    // depth first search
    test2.onKeyEvent("d");
    t.checkExpect(test2.search, true);
    t.checkExpect(test2.searchlist, test2.createPath(test2.maze.get(0),
        test2.maze.get(test2.maze.size() - 1), new Stack<Node>()));
    t.checkExpect(test2.manual, false);
    t.checkExpect(test2.traceBack, false);
    t.checkExpect(test2.endGame, false);
    
    // r to restart
    tbt.onKeyEvent("right");
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(1, 0));
    tbt.onKeyEvent("r");
    t.checkExpect(tbt.maze.size(), 4);
    t.checkExpect(tbt.player.cur.myPosn, new NodePosn(0, 0));
    t.checkExpect(tbt.searchlist, new ArrayDeque<Node>());
    t.checkExpect(tbt.search, false);
    t.checkExpect(tbt.traceBack, false);
    t.checkExpect(tbt.trace, new ArrayList<Node>());
    t.checkExpect(tbt.endGame, false);
  }

  void testCreatePath(Tester t) {
    MazeWorld test1 = new MazeWorld(5, 5);
    Deque<Node> testBFS = test1.createPath(test1.maze.get(0), test1.maze.get(test1.maze.size() - 1),
        new Queue<Node>());
    t.checkExpect(testBFS.getFirst(), test1.maze.get(0));

    Deque<Node> testDFS = test1.createPath(test1.maze.get(0), test1.maze.get(test1.maze.size() - 1),
        new Stack<Node>());
    t.checkExpect(testDFS.getFirst(), test1.maze.get(0));

  }

  void testShortestPath(Tester t) {
    MazeWorld test = new MazeWorld(5, 5);

    ArrayList<Node> testShort = test.shortestPath(test.maze.get(0), test.maze.get(24));
    t.checkExpect(testShort.get(0), test.maze.get(0));
    t.checkExpect(testShort.get(testShort.size() - 1), test.maze.get(24));

    MazeWorld test2 = new MazeWorld(3, 3);

    ArrayList<Node> testShort2 = test2.shortestPath(test2.maze.get(0), test2.maze.get(8));
    t.checkExpect(testShort2.get(0), test2.maze.get(0));
    t.checkExpect(testShort2.get(testShort2.size() - 1), test2.maze.get(8));
  }

  void testOnTick(Tester t) {
    initData();
    MazeWorld test = new MazeWorld(3, 3);
    test.player.cur = test.maze.get(8);
    test.manual = false;

    test.onTick();
    t.checkExpect(test.traceBack, true);
    t.checkExpect(test.endGame, true);

    test.onKeyEvent("r");
    test.player.cur = test.maze.get(8);
    test.manual = true;
    test.onTick();
    t.checkExpect(test.endGame, false);
    t.checkExpect(test.traceBack, true);

    Deque<Node> searchlist1 = new ArrayDeque<Node>();
    searchlist1.add(node1);
    searchlist1.add(node2);
    searchlist1.add(node3);
    searchlist1.add(node4);
    test.searchlist = searchlist1;
    test.search = true;

    t.checkExpect(test.searchlist.size(), 4);
    test.onTick();
    t.checkExpect(test.searchlist.size(), 3);

    test.onKeyEvent("r");
    // player has completed the maze and traced their way back to the beginning
    test.player.cur = test.maze.get(0);
    test.search = false;
    test.traceBack = true;
    test.onTick();
    t.checkExpect(test.endGame, true);
  }

  void testTrackMoves(Tester t) {
    initData();
    alteredData();
    ArrayList<Node> path = new ArrayList<Node>();
    path.add(node1);
    path.add(node2);
    path.add(node4);
    
    this.tbt.trackMoves(node2, path);
    t.checkExpect(tbt.correctMoves, 1);
    t.checkExpect(tbt.wrongMoves, 0);
    
    this.tbt.trackMoves(node3, path);
    t.checkExpect(tbt.correctMoves, 1);
    t.checkExpect(tbt.wrongMoves, 1);
    
    this.tbt.trackMoves(node4, path);
    t.checkExpect(tbt.correctMoves, 2);
    t.checkExpect(tbt.wrongMoves, 1);
  }
  
  void testBigBang(Tester t) {
    MazeWorld mw = new MazeWorld(20, 12);
    mw.bigBang(1000, 600, 1 / 10000.0);
  }
}