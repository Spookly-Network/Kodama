package net.spookly.kodama.brain.domain.user;

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

@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {

    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    public UserRole(User user, RoleEntity role) {
        this.user = Objects.requireNonNull(user, "user");
        this.role = Objects.requireNonNull(role, "role");
    }
}
