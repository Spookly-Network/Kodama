package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChecksumVerifier {

  private static final Pattern SHA256_PATTERN = Pattern.compile("(?i)\\b[0-9a-f]{64}\\b");

  public void verifySha256(Path artifact, String checksumContent) throws IOException {
    String expectedChecksum = extractChecksum(checksumContent);
    String actualChecksum = calculateSha256(artifact);
    if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
      throw new IOException(
          "SHA256 checksum mismatch for "
              + artifact
              + ": expected "
              + expectedChecksum
              + " but got "
              + actualChecksum);
    }
  }

  public String calculateSha256(Path artifact) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
    }

    try (InputStream inputStream = Files.newInputStream(artifact)) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  String extractChecksum(String checksumContent) throws IOException {
    if (checksumContent == null || checksumContent.isBlank()) {
      throw new IOException("Checksum content is empty");
    }
    Matcher matcher = SHA256_PATTERN.matcher(checksumContent);
    if (!matcher.find()) {
      throw new IOException("Checksum content does not include a valid SHA256 hash");
    }
    return matcher.group().toLowerCase();
  }
}
