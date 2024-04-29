package nl.basmens;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTimer {
  // Faster in fewer calls
  private static final SegmentTimeNode ROOT = new SegmentTimeNode("ROOT");
  // Faster in lots of calls
  private static final ConcurrentHashMap<Thread, LinkedList<SegmentTimeNode>> THREAD_SEGMENT_STACK = new ConcurrentHashMap<>();

  // A rough estimate of System.nanoTime() duration
  protected static final long AVERAGE_SYSTEM_NANOTIME_DURATION;

  // Do profiling or disable for increased performance
  private static boolean profile = true;

  private final Thread ownerThread;
  private final LinkedList<SegmentTimeNode> segmentStack;
  private final FunctionTimeNode functionTimer;
  private final long startTime;

  private SegmentTimeNode currentSegment;
  private long lastTimeStamp;
  
  // Calculate AVERAGE_SYSTEM_NANOTIME_DURATION
  static {
    // It's very rough, but it works
    long start = System.nanoTime();
    for (int i = 0; i < 999; i++) {
      System.nanoTime();
    }
    AVERAGE_SYSTEM_NANOTIME_DURATION = (System.nanoTime() - start) / 1000;
  }

  public PerformanceTimer(Class<?> c, String timerName, String firstSegment) {
    if (!profile) {
      this.startTime = 0;
      this.ownerThread = null;
      this.segmentStack = null;
      this.functionTimer = null;
      return;
    }

    // Get time stamps
    this.startTime = System.nanoTime();
    this.lastTimeStamp = this.startTime;

    // Build full name
    timerName = c.getName() + " - " + timerName;

    // Get the Node associated with the current function call of this timer using
    // the segment that ran the function call
    ownerThread = Thread.currentThread();
    segmentStack = THREAD_SEGMENT_STACK.computeIfAbsent(ownerThread, (Thread t) -> {
      LinkedList<SegmentTimeNode> stack = new LinkedList<>();
      stack.push(ROOT);
      return stack;
    });
    functionTimer = segmentStack.peek().getFunctionCall(timerName);
    functionTimer.incrementHitCount();

    // Add first segment to the stack for nested function calls to repeat the above
    this.currentSegment = functionTimer.getSegment(firstSegment);
    segmentStack.push(this.currentSegment);

    // To dismiss the time spent in the constructor
    long functionDuration = System.nanoTime() - this.startTime + AVERAGE_SYSTEM_NANOTIME_DURATION;
    for (SegmentTimeNode s : segmentStack) {
      s.removeDuration(functionDuration);
    }
  }

  public PerformanceTimer(Class<?> c, String timerName) {
    this(c, timerName, timerName);
  }

  public static void disable() {
    profile = false;
  }

  public static void enable() {
    profile = true;
  }

  public void nextSegment(String nextSegmentName) {
    if (!profile) {
      return;
    }

    long functionCallStart = System.nanoTime();

    if (Thread.currentThread()!= ownerThread) {
      throw new RuntimeException("PerformanceTimer must be used in the same thread as the one who created it");
    }

    // Add duration to current segment
    currentSegment.addDuration(functionCallStart - lastTimeStamp);
    lastTimeStamp = functionCallStart;
    
    // Move on to next segment
    segmentStack.pop();
    if (nextSegmentName != null) {
      currentSegment = functionTimer.getSegment(nextSegmentName);
      segmentStack.push(currentSegment);
    }
    
    // Discard time of this function to run
    long functionDuration = System.nanoTime() - functionCallStart;
    for (SegmentTimeNode s : segmentStack) {
      s.removeDuration(functionDuration);
    }
  }

  public void stop() {
    if (!profile) {
      return;
    }

    long functionCallStart = System.nanoTime();

    if (Thread.currentThread() != ownerThread) {
      throw new RuntimeException("PerformanceTimer must be used in the same thread as the one who created it");
    }
    if (currentSegment == null) {
      throw new RuntimeException("PerformanceTimer cannot be stopped, it has already been stopped");
    }

    // Stop timer
    nextSegment(null);
    currentSegment = null;
    
    // Discard time of functions of the timer to run
    long functionDuration = System.nanoTime() - functionCallStart + AVERAGE_SYSTEM_NANOTIME_DURATION;
    for (SegmentTimeNode s : segmentStack) {
      s.removeDuration(functionDuration);
    }
  }

  public static void flushData(OutputStream out) throws IOException {
    StringBuilder sb = new StringBuilder("{\"ROOT\":[");
    boolean first = true;
    for (FunctionTimeNode f : ROOT.functionCalls.values()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      f.export(sb);
    }
    sb.append("]}");
    out.write(sb.toString().getBytes());
  }
}
