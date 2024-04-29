package nl.basmens;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SegmentTimeNode {
  protected final Map<String, FunctionTimeNode> functionCalls = Collections.synchronizedMap(new HashMap<>());

  public final String name;

  private long totalDuration;
  private int hitCount;

  public SegmentTimeNode(String name) {
    this.name = name;
  }

  public FunctionTimeNode getFunctionCall(String name) {
    return functionCalls.computeIfAbsent(name, FunctionTimeNode::new);
  }

  public synchronized void addDuration(long duration) {
    totalDuration += duration;
    hitCount++;
  }

  public synchronized void removeDuration(long duration) {
    totalDuration -= duration;
  }

  protected synchronized void export(StringBuilder sb) {
    sb.append('{');
    sb.append("\"name\":\"").append(name).append('\"');
    sb.append(",\"total duration\":").append(totalDuration);
    sb.append(",\"hit count\":").append(hitCount);
    sb.append(",\"function calls\":[");
    boolean first = true;
    for (FunctionTimeNode f : functionCalls.values()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      f.export(sb);
    }
    sb.append("]}");
  }
}
