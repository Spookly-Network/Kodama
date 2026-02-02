package net.spookly.kodama.nodeagent.instance.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.instance.service.InstanceContainerMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class InstanceContainerMonitorScheduler implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(InstanceContainerMonitorScheduler.class);

    private final InstanceProperties instanceProperties;
    private final InstanceContainerMonitorService monitorService;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public InstanceContainerMonitorScheduler(
            InstanceProperties instanceProperties,
            InstanceContainerMonitorService monitorService
    ) {
        this.instanceProperties = instanceProperties;
        this.monitorService = monitorService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "instance-container-monitor");
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
        InstanceProperties.InstanceMonitor monitor = instanceProperties.getInstanceMonitor();
        if (monitor == null || !monitor.isEnabled()) {
            logger.info("Instance container monitor is disabled.");
            return;
        }
        int intervalSeconds = monitor.getIntervalSeconds();
        if (intervalSeconds <= 0) {
            logger.warn("Instance monitor interval is not configured. Container monitoring disabled.");
            return;
        }
        long delayMillis = intervalSeconds * 1000L;
        scheduler.schedule(this::runMonitorCycle, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void runMonitorCycle() {
        try {
            monitorService.monitorOnce();
        } catch (RuntimeException ex) {
            logger.warn("Instance container monitor cycle failed", ex);
        } finally {
            scheduleNext();
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
