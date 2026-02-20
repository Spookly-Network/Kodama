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
  private final InstanceStopService stopService;
  private final InstanceDestroyService destroyService;

  public InstanceLifecycleService(
      InstanceCallbackService callbackService,
      InstanceStartService startService,
      InstanceStopService stopService,
      InstanceDestroyService destroyService) {
    this.callbackService = Objects.requireNonNull(callbackService, "callbackService");
    this.startService = Objects.requireNonNull(startService, "startService");
    this.stopService = Objects.requireNonNull(stopService, "stopService");
    this.destroyService = Objects.requireNonNull(destroyService, "destroyService");
  }

  public void start(NodeInstanceCommandRequest request) {
    UUID instanceId = requireInstanceId(request);
    executeLifecycleCommand(
        "Start",
        instanceId,
        request.name(),
        () -> startService.startInstance(instanceId, request.name()),
        () -> callbackService.sendRunning(instanceId));
  }

  public void stop(NodeInstanceCommandRequest request) {
    UUID instanceId = requireInstanceId(request);
    executeLifecycleCommand(
        "Stop",
        instanceId,
        request.name(),
        () -> stopService.stopInstance(instanceId),
        () -> callbackService.sendStopped(instanceId));
  }

  public void destroy(NodeInstanceCommandRequest request) {
    UUID instanceId = requireInstanceId(request);
    executeLifecycleCommand(
        "Destroy",
        instanceId,
        request.name(),
        () -> destroyService.destroyInstance(instanceId),
        () -> callbackService.sendDestroyed(instanceId));
  }

  private void executeLifecycleCommand(
      String commandName,
      UUID instanceId,
      String requestName,
      Runnable commandAction,
      Runnable callbackAction) {
    logger.info(
        "{} command received. instanceId={} name={}",
        commandName,
        instanceId,
        valueOrDash(requestName));
    try {
      commandAction.run();
    } catch (RuntimeException ex) {
      sendFailedCallback(commandName, instanceId);
      logger.warn("{} command failed. instanceId={}", commandName, instanceId, ex);
      throw ex;
    }
    try {
      callbackAction.run();
      logger.info("{} command acknowledged. instanceId={}", commandName, instanceId);
    } catch (RuntimeException ex) {
      logger.warn("{} command callback failed. instanceId={}", commandName, instanceId, ex);
    }
  }

  private void sendFailedCallback(String commandName, UUID instanceId) {
    try {
      callbackService.sendFailed(instanceId);
    } catch (RuntimeException callbackEx) {
      logger.warn(
          "{} command failure callback failed. instanceId={}", commandName, instanceId, callbackEx);
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
