package net.spookly.kodama.nodeagent.instance.dto;

public record NodePreparePortDefinition(
    String name, String protocol, Integer containerPort, HostRange hostRange) {
  public record HostRange(Integer min, Integer max, Integer step) {}
}
