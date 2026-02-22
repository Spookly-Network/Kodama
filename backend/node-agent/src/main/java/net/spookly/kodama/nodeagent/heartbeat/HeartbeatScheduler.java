package net.spookly.kodama.nodeagent.heartbeat;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_BACKOFF_MILLIS = 500;

  private final NodeConfig config;
  private final NodeRegistrationState registrationState;
  private final NodeAuthTokenReader tokenReader;
  private final NodeHeartbeatClient heartbeatClient;
  private final NodeHeartbeatState heartbeatState;
  private final InstanceRegistryService registryService;
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean started = new AtomicBoolean(false);

  public HeartbeatScheduler(
      NodeConfig config,
      NodeRegistrationState registrationState,
      NodeAuthTokenReader tokenReader,
      NodeHeartbeatClient heartbeatClient,
      NodeHeartbeatState heartbeatState,
      InstanceRegistryService registryService) {
    this.config = config;
    this.registrationState = registrationState;
    this.tokenReader = tokenReader;
    this.heartbeatClient = heartbeatClient;
    this.heartbeatState = heartbeatState;
    this.registryService = registryService;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "node-heartbeat-scheduler");
              return thread;
            });
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (started.compareAndSet(false, true)) {
      scheduleNext();
    }
  }

  private void scheduleNext() {
    long delayMillis = resolveIntervalMillis();
    if (delayMillis <= 0) {
      logger.warn("Heartbeat interval is not configured. Heartbeat scheduler disabled.");
      return;
    }
    scheduler.schedule(this::runHeartbeatCycle, delayMillis, TimeUnit.MILLISECONDS);
  }

  private void runHeartbeatCycle() {
    try {
      sendHeartbeatWithRetry();
    } catch (RuntimeException ex) {
      logger.warn("Heartbeat cycle failed", ex);
    } finally {
      scheduleNext();
    }
  }

  private void sendHeartbeatWithRetry() {
    UUID nodeId = resolveNodeId();
    if (nodeId == null) {
      logger.debug("Skipping heartbeat because nodeId is not set.");
      return;
    }
    URI endpoint;
    try {
      endpoint = buildHeartbeatEndpoint(config.getBrainBaseUrl(), nodeId);
    } catch (RuntimeException ex) {
      logger.warn("Skipping heartbeat due to invalid Brain base URL", ex);
      return;
    }
    NodeHeartbeatRequest request = buildRequest();
    String authToken;
    try {
      authToken = tokenReader.readToken();
    } catch (RuntimeException ex) {
      logger.warn("Skipping heartbeat because auth token could not be read", ex);
      return;
    }
    String headerName = config.getAuth().getHeaderName();
    if (authToken != null && !authToken.isBlank() && (headerName == null || headerName.isBlank())) {
      logger.warn("Skipping heartbeat because auth header name is missing.");
      return;
    }

    long backoffMillis = INITIAL_BACKOFF_MILLIS;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        heartbeatClient.sendHeartbeat(endpoint, headerName, authToken, request);
        if (attempt > 1) {
          logger.info("Heartbeat succeeded after retry attempt {}", attempt);
        }
        return;
      } catch (RuntimeException ex) {
        if (attempt >= MAX_ATTEMPTS) {
          logger.warn("Heartbeat failed after {} attempts", MAX_ATTEMPTS, ex);
          return;
        }
        logger.warn("Heartbeat attempt {} failed, retrying in {}ms", attempt, backoffMillis, ex);
        if (!sleep(backoffMillis)) {
          return;
        }
        backoffMillis *= 2;
      }
    }
  }

  NodeHeartbeatRequest buildRequest() {
    int usedSlots = resolveUsedSlots();
    heartbeatState.setUsedSlots(usedSlots);
    NodeHeartbeatRequest request = new NodeHeartbeatRequest();
    request.setStatus(heartbeatState.getStatus());
    request.setUsedSlots(usedSlots);
    return request;
  }

  int resolveUsedSlots() {
    List<InstanceRegistryEntry> entries = registryService.listRegistries();
    long usedSlots = 0;
    for (InstanceRegistryEntry entry : entries) {
      if (entry == null || !isSlotConsumingStatus(entry.containerStatus())) {
        continue;
      }
      usedSlots += resolveSlotsRequired(entry.slotsRequired());
    }
    return clampUsedSlots(usedSlots, config.getCapacitySlots());
  }

  private boolean isSlotConsumingStatus(String status) {
    if (status == null || status.isBlank()) {
      return false;
    }
    String normalized = status.trim().toLowerCase(Locale.ROOT);
    return "starting".equals(normalized)
        || "running".equals(normalized)
        || "stopping".equals(normalized);
  }

  private int resolveSlotsRequired(Integer slotsRequired) {
    if (slotsRequired == null || slotsRequired < 1) {
      return 1;
    }
    return slotsRequired;
  }

  private int clampUsedSlots(long usedSlots, int capacitySlots) {
    long nonNegative = Math.max(0L, usedSlots);
    if (capacitySlots > 0) {
      nonNegative = Math.min(nonNegative, capacitySlots);
    }
    return (int) Math.min(nonNegative, Integer.MAX_VALUE);
  }

  private UUID resolveNodeId() {
    UUID nodeId = registrationState.getNodeId();
    if (nodeId != null) {
      return nodeId;
    }
    String configuredNodeId = config.getNodeId();
    if (configuredNodeId == null || configuredNodeId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(configuredNodeId);
    } catch (IllegalArgumentException ex) {
      logger.warn("Configured node ID is invalid: {}", configuredNodeId, ex);
      return null;
    }
  }

  private long resolveIntervalMillis() {
    int intervalSeconds = config.getHeartbeatIntervalSeconds();
    if (intervalSeconds <= 0) {
      intervalSeconds = registrationState.getHeartbeatIntervalSeconds();
    }
    if (intervalSeconds <= 0) {
      return 0;
    }
    return intervalSeconds * 1000L;
  }

  private URI buildHeartbeatEndpoint(String brainBaseUrl, UUID nodeId) {
    if (brainBaseUrl == null || brainBaseUrl.isBlank()) {
      throw new NodeHeartbeatException("Brain base URL is required for heartbeats");
    }
    String trimmed =
        brainBaseUrl.endsWith("/")
            ? brainBaseUrl.substring(0, brainBaseUrl.length() - 1)
            : brainBaseUrl;
    try {
      URI endpoint = URI.create(trimmed + "/api/nodes/" + nodeId + "/heartbeat");
      config.requireHttpsBrainUri(endpoint);
      return endpoint;
    } catch (IllegalStateException ex) {
      throw new NodeHeartbeatException(ex.getMessage());
    } catch (IllegalArgumentException ex) {
      throw new NodeHeartbeatException("Invalid Brain base URL: " + brainBaseUrl, ex);
    }
  }

  private boolean sleep(long backoffMillis) {
    try {
      Thread.sleep(backoffMillis);
      return true;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      logger.warn("Heartbeat retry interrupted");
      return false;
    }
  }

  @PreDestroy
  public void shutdown() {
    scheduler.shutdownNow();
  }
}
