package net.spookly.kodama.nodelauncher;

import java.time.Duration;

public final class CrashMonitor {

  private final Duration crashThreshold;
  private final int crashLimit;
  private int consecutiveFastCrashes;

  public CrashMonitor(Duration crashThreshold, int crashLimit) {
    if (crashThreshold == null || crashThreshold.isNegative() || crashThreshold.isZero()) {
      throw new IllegalArgumentException("crashThreshold must be a positive duration");
    }
    if (crashLimit < 1) {
      throw new IllegalArgumentException("crashLimit must be greater than 0");
    }
    this.crashThreshold = crashThreshold;
    this.crashLimit = crashLimit;
    this.consecutiveFastCrashes = 0;
  }

  public Duration crashThreshold() {
    return crashThreshold;
  }

  public boolean recordExit(Duration runDuration) {
    if (runDuration.compareTo(crashThreshold) < 0) {
      consecutiveFastCrashes += 1;
      return consecutiveFastCrashes >= crashLimit;
    }
    consecutiveFastCrashes = 0;
    return false;
  }

  public int consecutiveFastCrashes() {
    return consecutiveFastCrashes;
  }

  public void reset() {
    consecutiveFastCrashes = 0;
  }
}
