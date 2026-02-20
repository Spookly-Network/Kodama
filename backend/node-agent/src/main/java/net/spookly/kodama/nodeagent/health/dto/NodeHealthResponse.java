package net.spookly.kodama.nodeagent.health.dto;

public record NodeHealthResponse(
    String status, String nodeId, String nodeName, String nodeVersion) {}
