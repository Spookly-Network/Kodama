package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrashMonitorTest {

  @Test
  void shouldTriggerRollbackAfterThreeFastCrashes() {
    CrashMonitor crashMonitor = new CrashMonitor(Duration.ofSeconds(30), 3);

    assertFalse(crashMonitor.recordExit(Duration.ofSeconds(5)));
    assertFalse(crashMonitor.recordExit(Duration.ofSeconds(10)));
    assertTrue(crashMonitor.recordExit(Duration.ofSeconds(15)));
    assertEquals(3, crashMonitor.consecutiveFastCrashes());
  }

  @Test
  void shouldResetCounterAfterStableRun() {
    CrashMonitor crashMonitor = new CrashMonitor(Duration.ofSeconds(30), 3);

    crashMonitor.recordExit(Duration.ofSeconds(5));
    crashMonitor.recordExit(Duration.ofSeconds(10));

    assertFalse(crashMonitor.recordExit(Duration.ofSeconds(45)));
    assertEquals(0, crashMonitor.consecutiveFastCrashes());
  }
}
