package io.github.huahill.vadmin.springjpa.administration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.springjpa.access.JpaRoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = JpaAdministrationStoreTest.TestApplication.class)
class JpaAdministrationStoreTest {
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMINISTRATOR_ROLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:vadmin-admin;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:mysql-iam/schema.sql");
        registry.add("spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type", () -> "CHAR");
    }

    @Test
    @Transactional
    void should_page_users_and_lock_administrator_role_on_char36_ids() {
        entityManager.createNativeQuery("""
                insert into vadmin_users (id, username, password_hash, enabled, auth_version)
                values (:id, 'admin', 'hash', true, 1)
                """).setParameter("id", ADMIN_ID.toString()).executeUpdate();
        entityManager.createNativeQuery("""
                insert into vadmin_roles (id, code) values (:id, 'administrator')
                """).setParameter("id", ADMINISTRATOR_ROLE_ID.toString()).executeUpdate();

        var store = new JpaAdministrationStore(entityManager);
        var page = store.users(new PagedQuery(0, 10, "username", true, Map.of("q", "admin")));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(ADMIN_ID);
            assertThat(user.username()).isEqualTo("admin");
        });

        store.lockAdministratorRole();
        JpaRoleEntity locked = entityManager.createQuery(
                "select role from JpaRoleEntity role where role.code = 'administrator'", JpaRoleEntity.class)
                .getSingleResult();
        assertThat(entityManager.getLockMode(locked)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            FlywayAutoConfiguration.class,
            io.github.huahill.vadmin.springjpa.JpaAccessControlConfiguration.class
    })
    @EntityScan(basePackages = "io.github.huahill.vadmin.springjpa")
    static class TestApplication { }
}
