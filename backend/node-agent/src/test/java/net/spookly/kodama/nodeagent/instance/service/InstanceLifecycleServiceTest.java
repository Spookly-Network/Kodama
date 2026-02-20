package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import net.spookly.kodama.nodeagent.instance.callback.InstanceCallbackService;
import net.spookly.kodama.nodeagent.instance.dto.NodeInstanceCommandRequest;
import org.junit.jupiter.api.Test;

class InstanceLifecycleServiceTest {

  private final InstanceCallbackService callbackService = mock(InstanceCallbackService.class);
  private final InstanceStartService startService = mock(InstanceStartService.class);
  private final InstanceStopService stopService = mock(InstanceStopService.class);
  private final InstanceDestroyService destroyService = mock(InstanceDestroyService.class);
  private final InstanceLifecycleService service =
      new InstanceLifecycleService(callbackService, startService, stopService, destroyService);

  @Test
  void startTriggersRunningCallback() {
    UUID instanceId = UUID.randomUUID();

    service.start(new NodeInstanceCommandRequest(instanceId, "demo"));

    verify(startService).startInstance(instanceId, "demo");
    verify(callbackService).sendRunning(instanceId);
  }

  @Test
  void stopTriggersStoppedCallback() {
    UUID instanceId = UUID.randomUUID();

    service.stop(new NodeInstanceCommandRequest(instanceId, "demo"));

    verify(stopService).stopInstance(instanceId);
    verify(callbackService).sendStopped(instanceId);
  }

  @Test
  void destroyTriggersDestroyedCallback() {
    UUID instanceId = UUID.randomUUID();

    service.destroy(new NodeInstanceCommandRequest(instanceId, "demo"));

    verify(destroyService).destroyInstance(instanceId);
    verify(callbackService).sendDestroyed(instanceId);
  }

  @Test
  void startRejectsMissingInstanceId() {
    NodeInstanceCommandRequest request = new NodeInstanceCommandRequest(null, "demo");

    assertThatThrownBy(() -> service.start(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("instanceId is required");
  }

  @Test
  void startFailureSendsFailedCallback() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("boom")).when(startService).startInstance(instanceId, "demo");

    assertThatThrownBy(() -> service.start(new NodeInstanceCommandRequest(instanceId, "demo")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("boom");

    verify(callbackService).sendFailed(instanceId);
  }

  @Test
  void stopFailureSendsFailedCallback() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("boom")).when(stopService).stopInstance(instanceId);

    assertThatThrownBy(() -> service.stop(new NodeInstanceCommandRequest(instanceId, "demo")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("boom");

    verify(callbackService).sendFailed(instanceId);
  }

  @Test
  void destroyFailureSendsFailedCallback() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("boom")).when(destroyService).destroyInstance(instanceId);

    assertThatThrownBy(() -> service.destroy(new NodeInstanceCommandRequest(instanceId, "demo")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("boom");

    verify(callbackService).sendFailed(instanceId);
  }

  @Test
  void startCallbackFailureDoesNotSendFailedCallback() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("callback boom")).when(callbackService).sendRunning(instanceId);

    assertThatNoException()
        .isThrownBy(() -> service.start(new NodeInstanceCommandRequest(instanceId, "demo")));

    verify(startService).startInstance(instanceId, "demo");
    verify(callbackService).sendRunning(instanceId);
    verify(callbackService, never()).sendFailed(instanceId);
  }

  @Test
  void stopCallbackFailureDoesNotThrow() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("callback boom")).when(callbackService).sendStopped(instanceId);

    assertThatNoException()
        .isThrownBy(() -> service.stop(new NodeInstanceCommandRequest(instanceId, "demo")));

    verify(stopService).stopInstance(instanceId);
    verify(callbackService).sendStopped(instanceId);
    verify(callbackService, never()).sendFailed(instanceId);
  }

  @Test
  void destroyCallbackFailureDoesNotThrow() {
    UUID instanceId = UUID.randomUUID();
    doThrow(new RuntimeException("callback boom")).when(callbackService).sendDestroyed(instanceId);

    assertThatNoException()
        .isThrownBy(() -> service.destroy(new NodeInstanceCommandRequest(instanceId, "demo")));

    verify(destroyService).destroyInstance(instanceId);
    verify(callbackService).sendDestroyed(instanceId);
    verify(callbackService, never()).sendFailed(instanceId);
  }
}
