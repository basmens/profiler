package nl.basmens;

import processing.data.JSONArray;
import processing.data.JSONObject;

public class Segment extends Node<FunctionTimer> {
  public Segment(String name, long totalDuration, int hitCount) {
    super(name, totalDuration, hitCount);
  }

  protected Segment(JSONObject json) {
    super(json.getString("name"), json.getLong("total duration"), json.getInt("hit count"));

    JSONArray functionTimers = json.getJSONArray("function calls");
    for (int i = 0; i < functionTimers.size(); i++) {
      children.add(new FunctionTimer(functionTimers.getJSONObject(i)));
    }
  }

  protected FunctionTimer createChild(JSONObject json) {
    return new FunctionTimer(json);
  }

  public void draw(double x, double y, double widthMult, double height, int colorId) {
    int newColor = colorId;
    for (FunctionTimer f : children) {
      newColor = Main.colorCycle(newColor, colorId);
      f.draw(x, y + height, widthMult, height, newColor);
      x += f.totalDuration * widthMult;
    }
  }

  public Node<?> findHover(double x, double y, double widthMult, double height, int mouseX, int mouseY) {
    for (FunctionTimer f : children) {
      Node<?> hover = f.findHover(x, y + height, widthMult, height, mouseX, mouseY);
      if (hover != null) {
        return hover;
      }
      x += f.totalDuration * widthMult;
    }
    return null;
  }
}
