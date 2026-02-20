package net.spookly.kodama.nodeagent.instance.dto;

import java.util.UUID;

public record NodeInstanceCommandRequest(UUID instanceId, String name) {}
