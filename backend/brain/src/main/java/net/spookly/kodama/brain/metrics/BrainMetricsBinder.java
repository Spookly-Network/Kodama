package net.spookly.kodama.brain.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrainMetricsBinder implements MeterBinder {

    private static final Logger logger = LoggerFactory.getLogger(BrainMetricsBinder.class);

    private final InstanceRepository instanceRepository;
    private final NodeRepository nodeRepository;

    public BrainMetricsBinder(InstanceRepository instanceRepository, NodeRepository nodeRepository) {
        this.instanceRepository = instanceRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (InstanceState state : InstanceState.values()) {
            Gauge.builder("kodama.instances.state", () -> safeInstanceCount(state))
                    .description("Number of instances in a given state.")
                    .tag("state", state.name())
                    .register(registry);
        }

        for (NodeStatus status : NodeStatus.values()) {
            Gauge.builder("kodama.nodes.status", () -> safeNodeCount(status))
                    .description("Number of nodes in a given status.")
                    .tag("status", status.name())
                    .register(registry);
        }
    }

    private double safeInstanceCount(InstanceState state) {
        try {
            return instanceRepository.countByState(state);
        } catch (RuntimeException ex) {
            logger.warn("Failed to load instance metrics state={}", state, ex);
            return Double.NaN;
        }
    }

    private double safeNodeCount(NodeStatus status) {
        try {
            return nodeRepository.countByStatus(status);
        } catch (RuntimeException ex) {
            logger.warn("Failed to load node metrics status={}", status, ex);
            return Double.NaN;
        }
    }
}
