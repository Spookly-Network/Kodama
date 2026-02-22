package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChecksumVerifierTest {

  @TempDir Path tempDir;

  @Test
  void shouldVerifyMatchingChecksum() throws IOException {
    Path artifact = tempDir.resolve("agent.jar");
    Files.writeString(artifact, "kodama");

    ChecksumVerifier verifier = new ChecksumVerifier();
    String checksum = verifier.calculateSha256(artifact);
    verifier.verifySha256(artifact, checksum + "  agent.jar");
    assertEquals(checksum, verifier.extractChecksum(checksum + "  agent.jar"));
  }

  @Test
  void shouldFailWhenChecksumDoesNotMatch() throws IOException {
    Path artifact = tempDir.resolve("agent.jar");
    Files.writeString(artifact, "kodama");

    ChecksumVerifier verifier = new ChecksumVerifier();
    assertThrows(
        IOException.class,
        () ->
            verifier.verifySha256(
                artifact,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa  agent.jar"));
  }
}
