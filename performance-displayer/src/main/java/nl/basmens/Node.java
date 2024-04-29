package nl.basmens;

import java.util.ArrayList;
import java.util.List;

public class Node<E extends Node<?>> {
  protected final List<E> children = new ArrayList<>();
  public final String name;

  protected long totalDuration;
  protected int hitCount;

  protected Node(String name, long totalDuration, int hitCount) {
    this.name = name;
    this.totalDuration = totalDuration;
    this.hitCount = hitCount;
  }

  public E getChild(int i) {
    return children.get(i);
  }

  public int getDepth() {
    return children.stream().mapToInt(Node::getDepth).max().orElse(0) + 1;
  }

  public void deepSort() {
    children.sort((E a, E b) -> {
      long dif = b.totalDuration - a.totalDuration;
      return dif > 0 ? 1 : (dif == 0 ? 0 : -1);
    });
    children.forEach(Node::deepSort);
  }

  public void addChild(E child) {
    children.add(child);
  }
}
