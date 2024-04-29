package nl.basmens;

import java.io.File;
import java.util.Locale;

import nl.benmens.processing.PApplet;
import nl.benmens.processing.PAppletProxy;
import processing.data.JSONArray;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

public class Main extends PApplet {
  private final Segment root = new Segment("root", 0, 0);
  private int[] maxDepths = new int[0];
  private int tabCount;
  private int currentTab;
  private double zoom = 1;
  private double scroll;

  private boolean[] keys = new boolean[128];

  public static int[] colors = {
      toColor(250, 210, 80), toColor(245, 120, 240), toColor(65, 240, 200), toColor(155, 245, 110),
      toColor(245, 245, 115), toColor(245, 135, 175), toColor(45, 205, 240), toColor(120, 250, 140)
      // Yellow, Pink, Aqua, Green
  };

  public static int colorCycle(int cycle, int avoid) {
    return (++cycle == avoid ? ++cycle : cycle) % colors.length;
  }

  // ===================================================================================================================
  // Native processing functions for lifecycle
  // ===================================================================================================================
  @Override
  public void settings() {
    PAppletProxy.setSharedApplet(this);
    size(1920, 1080, P2D);
  }

  @Override
  public void setup() {
    // A workaround for noSmooth() not being compatible with P2D
    ((PGraphicsOpenGL) g).textureSampling(3);
    surface.setLocation(0, 0);
    registerMethod("mouseEvent", this);

    selectInput("Select performance json", "fileSelected");
  }

  @Override
  public void draw() {
    background(0);

    if (keys['a']) {
      scroll = Math.max(Math.min(scroll - 0.03, zoom - 1), 0);
    }
    if (keys['d']) {
      scroll = Math.max(Math.min(scroll + 0.03, zoom - 1), 0);
    }

    if (tabCount == 0) {
      return;
    }

    FunctionTimer ft = root.getChild(currentTab);
    double h = (height - 200D) / maxDepths[currentTab];
    ft.draw(100 - scroll * (width - 200D), 100, (width - 200D) / ft.totalDuration * zoom, h, 0);

    Node<?> hover = ft.findHover(100 - scroll * (width - 200D), 100, (width - 200D) / ft.totalDuration * zoom, h,
        mouseX, mouseY);
    if (hover != null) {
      textSize(18);
      textAlign(LEFT, TOP);
      String nameText = "name: " + hover.name;
      String nameTotalDuration = "total duration: " + formatDuration(hover.totalDuration);
      String nameHitCount = "hit count: " + formatNumber(hover.hitCount);
      String nameAverageDuration = "average duration: " + formatDuration(hover.totalDuration / hover.hitCount);
      int w = (int) max(max(textWidth(nameText), textWidth(nameTotalDuration), textWidth(nameHitCount)),
          textWidth(nameAverageDuration)) + 30;

      pushMatrix();
      translate(min(mouseX + 10, width - w), min(mouseY + 10, height - 150));
      stroke(240);
      strokeWeight(4);
      fill(20);
      rect(0, 0, w, 150);

      fill(240);
      text(nameText, 15, 10);
      text(nameTotalDuration, 15, 45);
      text(nameHitCount, 15, 80);
      text(nameAverageDuration, 15, 115);
      popMatrix();
    }
  }

  // ===================================================================================================================
  // Util
  // ===================================================================================================================
  public String formatDuration(long duration) {
    if (duration < 1_000L) {
      // ns
      return duration + "ns";
    } else if (duration < 1_000_000L) {
      // ns
      return String.format(Locale.ENGLISH, "%d %03dns", duration / 1000L, duration % 1000L);
    } else if (duration < 100_000_000L) {
      // ms decimal
      return String.format(Locale.ENGLISH, "%d.%d", duration / 1_000_000L, duration % 1_000_000L).substring(0, 5)
          + "ms";
    } else if (duration < 1_000_000_000L) {
      // ms no decimal
      return (duration / 1_000_000L) + "ms";
    } else if (duration < 100_000_000_000L) {
      // s decimal
      return String.format(Locale.ENGLISH, "%d.%d", duration / 1_000_000_000L, duration % 1_000_000_000L).substring(0,
          5) + "s";
    } else {
      // s decimal
      return formatNumber(duration / 1_000_000_000L) + "s";
    }
  }

  public String formatNumber(long number) {
    StringBuilder result = new StringBuilder();
    for (; number >= 1000L; number /= 1000L) {
      result.insert(0, String.format(Locale.ENGLISH, " %03d", number % 1000L));
    }
    result.insert(0, number);
    return result.toString();
  }

  public static final double map(double value, double start1, double stop1, double start2, double stop2) {
    return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
  }

  public static int toColor(int r, int g, int b) {
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  // ===================================================================================================================
  // Events
  // ===================================================================================================================
  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.WHEEL) {
      double pow = Math.pow(0.91, event.getCount());
      zoom = Math.max(Math.min(zoom * pow, 1000), 1);
      scroll *= pow;
      scroll += map(mouseX, 100, width - 100D, 0, pow - 1);
      scroll = Math.max(Math.min(scroll, zoom - 1), 0);
    }
  }

  @Override
  public void keyPressed() {
    switch (key) {
      case 'w':
        currentTab = (currentTab + 1) % tabCount;
        break;
      case 's':
        currentTab = (currentTab + tabCount - 1) % tabCount;
        break;
      default:
        break;
    }

    if (key == CODED) {
      if (keyCode < 128) {
        keys[keyCode] = true;
      }
    } else {
      if (key < 128) {
        keys[Character.toLowerCase(key)] = true;
      }
    }
  }

  @Override
  public void keyReleased() {
    if (keyPressed) {
      if (key == CODED) {
        if (keyCode < 128) {
          keys[keyCode] = false;
        }
      } else {
        if (key < 128) {
          keys[Character.toLowerCase(key)] = false;
        }
      }
    } else {
      keys = new boolean[128];
    }
  }

  public void fileSelected(File selectedFile) {
    if (selectedFile == null) {
      println("No file selected");
      exit();
      return;
    }
    
    JSONArray rootArray = loadJSONObject(selectedFile).getJSONArray("ROOT");
    tabCount = rootArray.size();
    maxDepths = new int[tabCount];
    for (int i = 0; i < rootArray.size(); i++) {
      FunctionTimer ft = new FunctionTimer(rootArray.getJSONObject(i));
      root.addChild(ft);
    }
    root.deepSort();
    for (int i = 0; i < rootArray.size(); i++) {
      maxDepths[i] = root.getChild(i).getDepth() / 2;
    }
  }

  // ===================================================================================================================
  // Main function
  // ===================================================================================================================
  public static void main(String[] passedArgs) {
    if (passedArgs != null) {
      PApplet.main(new Object() {
      }.getClass().getEnclosingClass(), passedArgs);
    } else {
      PApplet.main(new Object() {
      }.getClass().getEnclosingClass());
    }
  }
}
