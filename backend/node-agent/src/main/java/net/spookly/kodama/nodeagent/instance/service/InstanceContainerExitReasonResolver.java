package net.spookly.kodama.nodeagent.instance.service;

import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;

final class InstanceContainerExitReasonResolver {

    private InstanceContainerExitReasonResolver() {
    }

    static String resolveExitReason(DockerContainerStatus status) {
        if (status == null) {
            return null;
        }
        String error = status.error();
        if (error != null && !error.isBlank()) {
            return error.trim();
        }
        if (Boolean.TRUE.equals(status.oomKilled())) {
            return "oom-killed";
        }
        if (Boolean.TRUE.equals(status.dead())) {
            return "dead";
        }
        if (Boolean.TRUE.equals(status.restarting())) {
            return "restarting";
        }
        String state = status.status();
        if (state != null && !state.isBlank()) {
            return state.trim();
        }
        return null;
    }
}
