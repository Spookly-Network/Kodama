package net.spookly.kodama.brain.domain.blueprint;

import java.util.Objects;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;

@Entity
@Table(name = "blueprint_group_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlueprintGroupLink {

    @EmbeddedId
    private BlueprintGroupLinkId id = new BlueprintGroupLinkId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blueprintId")
    @JoinColumn(name = "blueprint_id", nullable = false)
    private Blueprint blueprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private InstanceGroup group;

    public BlueprintGroupLink(Blueprint blueprint, InstanceGroup group) {
        this.blueprint = Objects.requireNonNull(blueprint, "blueprint");
        this.group = Objects.requireNonNull(group, "group");
        this.id = new BlueprintGroupLinkId(blueprint.getId(), group.getId());
    }
}
