package nl.basmens;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FunctionTimeNode {
  private final Map<String, SegmentTimeNode> segments = Collections.synchronizedMap(new HashMap<>());

  public final String name;

  private int hitCount;

  public FunctionTimeNode(String name) {
    this.name = name;
  }

  public SegmentTimeNode getSegment(String name) {
    return segments.computeIfAbsent(name, SegmentTimeNode::new);
  }

  public void incrementHitCount() {
    hitCount++;
  }

  protected synchronized void export(StringBuilder sb) {
    sb.append('{');
    sb.append("\"name\":\"").append(name).append('\"');
    sb.append(",\"hit count\":").append(hitCount);
    sb.append(",\"segments\":[");
    boolean first = true;
    for (SegmentTimeNode s : segments.values()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      s.export(sb);
    }
    sb.append("]}");
  }
}
