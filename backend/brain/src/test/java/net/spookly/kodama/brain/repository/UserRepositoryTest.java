package net.spookly.kodama.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import net.spookly.kodama.brain.domain.user.RoleEntity;
import net.spookly.kodama.brain.domain.user.User;
import net.spookly.kodama.brain.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void saveUserWithRoles() {
        RoleEntity admin = roleRepository.save(new RoleEntity(Role.ADMIN));
        RoleEntity viewer = roleRepository.save(new RoleEntity(Role.VIEWER));

        User user = new User("nina", "Nina Winters", "nina@example.com", "local", "nina-1");
        user.addRole(admin);
        user.addRole(viewer);

        User saved = userRepository.save(user);
        User persisted = userRepository.findById(saved.getId()).orElseThrow();

        assertThat(persisted.getUsername()).isEqualTo("nina");
        assertThat(persisted.getEmail()).isEqualTo("nina@example.com");
        assertThat(persisted.getUserRoles())
                .extracting(userRole -> userRole.getRole().getName())
                .containsExactlyInAnyOrder(Role.ADMIN, Role.VIEWER);
    }
}
