package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LauncherApplicationVersionTest {

  @Test
  void shouldCompareSemanticVersionsDeterministically() {
    assertTrue(LauncherApplication.compareVersions("1.2.0", "1.1.9") > 0);
    assertEquals(0, LauncherApplication.compareVersions("1.0", "1.0.0"));
    assertTrue(LauncherApplication.compareVersions("2.0.0-beta", "2.0.0-alpha") > 0);
    assertTrue(LauncherApplication.compareVersions("1.0.0", "1.0.1") < 0);
  }
}
