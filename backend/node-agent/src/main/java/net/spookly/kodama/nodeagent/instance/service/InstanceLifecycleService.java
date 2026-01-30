package net.spookly.kodama.nodeagent.instance.service;

import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.nodeagent.instance.callback.InstanceCallbackService;
import net.spookly.kodama.nodeagent.instance.dto.NodeInstanceCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceLifecycleService.class);

    private final InstanceCallbackService callbackService;
    private final InstanceStartService startService;

    public InstanceLifecycleService(
            InstanceCallbackService callbackService,
            InstanceStartService startService
    ) {
        this.callbackService = Objects.requireNonNull(callbackService, "callbackService");
        this.startService = Objects.requireNonNull(startService, "startService");
    }

    public void start(NodeInstanceCommandRequest request) {
        UUID instanceId = requireInstanceId(request);
        logger.info("Start command received. instanceId={} name={}", instanceId, valueOrDash(request.name()));
        try {
            startService.startInstance(instanceId, request.name());
        } catch (RuntimeException ex) {
            try {
                callbackService.sendFailed(instanceId);
            } catch (RuntimeException callbackEx) {
                logger.warn("Start command failure callback failed. instanceId={}", instanceId, callbackEx);
            }
            logger.warn("Start command failed. instanceId={}", instanceId, ex);
            throw ex;
        }
        try {
            callbackService.sendRunning(instanceId);
            logger.info("Start command acknowledged. instanceId={}", instanceId);
        } catch (RuntimeException ex) {
            logger.warn("Start command callback failed. instanceId={}", instanceId, ex);
            throw ex;
        }
    }

    public void stop(NodeInstanceCommandRequest request) {
        UUID instanceId = requireInstanceId(request);
        logger.info("Stop command received. instanceId={} name={}", instanceId, valueOrDash(request.name()));
        try {
            callbackService.sendStopped(instanceId);
            logger.info("Stop command acknowledged. instanceId={}", instanceId);
        } catch (RuntimeException ex) {
            logger.warn("Stop command callback failed. instanceId={}", instanceId, ex);
            throw ex;
        }
    }

    public void destroy(NodeInstanceCommandRequest request) {
        UUID instanceId = requireInstanceId(request);
        logger.info("Destroy command received. instanceId={} name={}", instanceId, valueOrDash(request.name()));
        try {
            callbackService.sendDestroyed(instanceId);
            logger.info("Destroy command acknowledged. instanceId={}", instanceId);
        } catch (RuntimeException ex) {
            logger.warn("Destroy command callback failed. instanceId={}", instanceId, ex);
            throw ex;
        }
    }

    private UUID requireInstanceId(NodeInstanceCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("instance command request is required");
        }
        UUID instanceId = request.instanceId();
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        return instanceId;
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
