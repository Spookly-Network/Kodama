package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceEventRepository
        extends JpaRepository<@NonNull InstanceEvent, @NonNull UUID> {

    List<InstanceEvent> findByInstanceOrderByTimestampAsc(Instance instance);
    List<InstanceEvent> findAllByInstanceIdOrderByTimestampAsc(UUID instanceId);


}
