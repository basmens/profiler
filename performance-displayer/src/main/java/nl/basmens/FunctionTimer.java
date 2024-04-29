package nl.basmens;

import static nl.benmens.processing.PAppletProxy.fill;
import static nl.benmens.processing.PAppletProxy.noStroke;
import static nl.benmens.processing.PAppletProxy.rect;

import processing.data.JSONArray;
import processing.data.JSONObject;

public class FunctionTimer extends Node<Segment> {
  public FunctionTimer(String name, long totalDuration, int hitCount) {
    super(name, totalDuration, hitCount);
  }

  protected FunctionTimer(JSONObject json) {
    super(json.getString("name"), 0, json.getInt("hit count"));

    JSONArray segments = json.getJSONArray("segments");
    for (int i = 0; i < segments.size(); i++) {
      Segment segment = new Segment(segments.getJSONObject(i));
      totalDuration += segment.totalDuration;
      children.add(segment);
    }
  }

  public void draw(double x, double y, double widthMult, double height, int colorId) {
    noStroke();
    fill(Main.colors[colorId]);
    rect((float) x, (float) y, (float) (totalDuration * widthMult), (float) height);

    int newColor = colorId;
    fill(0, 60);
    for (Segment s : children) {
      newColor = Main.colorCycle(newColor, colorId);
      s.draw(x, y, widthMult, height, newColor);

      if (s != children.get(0)) {
        rect((float) (x - 3), (float) (y + height * 0.65), 6, (float) (height * 0.35));
      }
      x += s.totalDuration * widthMult;
    }
  }

  public Node<?> findHover(double x, double y, double widthMult, double height, int mouseX, int mouseY) {
    if (mouseX > x && mouseX < x + totalDuration * widthMult && mouseY > y && mouseY < y + height) {
      if (mouseY < y + height * 0.7) {
        return this;
      }

      for (Segment s : children) {
        x += s.totalDuration * widthMult;
        if (x > mouseX) {
          return s;
        }
      }
    }

    for (Segment s : children) {
      Node<?> hover = s.findHover(x, y, widthMult, height, mouseX, mouseY);
      if (hover != null) {
        return hover;
      }
      x += s.totalDuration * widthMult;
    }
    return null;
  }
}
