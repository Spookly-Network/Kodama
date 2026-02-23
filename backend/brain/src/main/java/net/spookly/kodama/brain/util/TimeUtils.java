package net.spookly.kodama.brain.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class TimeUtils {
  public static OffsetDateTime utcNow() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
