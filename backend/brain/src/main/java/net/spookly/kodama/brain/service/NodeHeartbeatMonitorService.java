package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NodeHeartbeatMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(NodeHeartbeatMonitorService.class);

    private final NodeRepository nodeRepository;
    private final NodeProperties nodeProperties;

    public NodeHeartbeatMonitorService(NodeRepository nodeRepository, NodeProperties nodeProperties) {
        this.nodeRepository = nodeRepository;
        this.nodeProperties = nodeProperties;
    }

    @Scheduled(fixedDelayString = "#{@nodeProperties.heartbeatMonitorIntervalSeconds * 1000}")
    @Transactional
    public void monitorHeartbeats() {
        try {
            markStaleNodesOffline(OffsetDateTime.now(ZoneOffset.UTC));
        } catch (RuntimeException ex) {
            logger.error("Failed to mark stale nodes offline", ex);
            throw ex;
        }
    }

    void markStaleNodesOffline(OffsetDateTime now) {
        Objects.requireNonNull(now, "now");
        int timeoutSeconds = nodeProperties.getHeartbeatTimeoutSeconds();
        OffsetDateTime cutoff = now.minusSeconds(timeoutSeconds);
        List<Node> staleNodes =
                nodeRepository.findByStatusNotAndLastHeartbeatAtBefore(NodeStatus.OFFLINE, cutoff);
        for (Node node : staleNodes) {
            node.markOffline();
            logger.info(
                    "Marked node offline due to missed heartbeats nodeId={} nodeName={} lastHeartbeatAt={} timeoutSeconds={}",
                    node.getId(),
                    node.getName(),
                    node.getLastHeartbeatAt(),
                    timeoutSeconds
            );
        }
    }
}
