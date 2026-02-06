package net.spookly.kodama.brain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceTemplateLayerRepository extends JpaRepository<@NonNull InstanceTemplateLayer, @NonNull UUID> {

    @Query("select l from InstanceTemplateLayer l where l.instance.id = :instanceId order by l.orderIndex asc")
    List<InstanceTemplateLayer> findAllByInstanceId(@Param("instanceId") UUID instanceId);

    @Query("select l from InstanceTemplateLayer l where l.instance.id in :instanceIds order by l.instance.id, l.orderIndex asc")
    List<InstanceTemplateLayer> findAllByInstanceIds(@Param("instanceIds") Collection<UUID> instanceIds);
}
