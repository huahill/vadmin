package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Synchronizes the permission catalog declared by the administration modules into the
 * IAM schema and bootstraps the initial administrator account on an empty store.
 *
 * <p>When the administrator role already exists, newly declared catalog permissions are
 * granted to it on each start so a module rename does not leave the console empty.
 * SQL stays in {@link JdbcIamStore}; this runner only owns startup policy.</p>
 */
final class JdbcIamInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcIamInitializer.class);

    private final JdbcIamStore iamStore;
    private final PasswordEncoder passwordEncoder;
    private final PermissionCatalog permissionCatalog;
    private final Environment environment;
    private final TransactionOperations transactions;
    private final ExternalGrantStore externalGrants;

    JdbcIamInitializer(JdbcIamStore iamStore, PasswordEncoder passwordEncoder,
                         PermissionCatalog permissionCatalog, Environment environment,
                         TransactionOperations transactions, ExternalGrantStore externalGrants) {
        this.iamStore = Objects.requireNonNull(iamStore);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.permissionCatalog = Objects.requireNonNull(permissionCatalog);
        this.environment = Objects.requireNonNull(environment);
        this.transactions = Objects.requireNonNull(transactions);
        this.externalGrants = externalGrants;
    }

    @Override
    public void run(String... args) {
        // Immutable sets have a process-specific iteration order. Use the same lock order
        // in every replica while the permission inserts share a transaction.
        List<String> codes = permissionCatalog.all().stream().map(code -> code.value()).sorted().toList();
        try {
            transactions.executeWithoutResult(status -> initialize(codes));
        } catch (DuplicateKeyException concurrentBootstrap) {
            // Another replica may have committed the first admin while this transaction waited.
            // Retry in a fresh transaction so the winning account is visible and never reset.
            transactions.executeWithoutResult(status -> {
                boolean ready = externalGrants == null ? iamStore.hasAnyUser() : externalGrants.hasAdministrator();
                if (!ready) {
                    throw concurrentBootstrap;
                }
                initialize(codes);
            });
        }
    }

    private void initialize(List<String> codes) {
        iamStore.synchronizePermissions(codes);
        if (externalGrants != null) {
            initializeExternal(codes);
            return;
        }
        if (iamStore.hasAnyUser()) {
            int granted = iamStore.grantMissingPermissionsToAdministrator(codes);
            if (granted > 0) {
                LOG.info("Granted {} newly declared catalog permission(s) to the administrator role.", granted);
            }
            return;
        }
        int granted = iamStore.bootstrapAdministrator(passwordEncoder.encode(bootstrapPassword()), codes);
        LOG.info("Bootstrapped the initial 'admin' account with the 'administrator' role and {} permissions.", granted);
    }

    private void initializeExternal(List<String> codes) {
        iamStore.ensureAdministratorRole(codes);
        for (String subject : bootstrapSubjects()) {
            externalGrants.ensureAdministrator(SubjectId.of(subject));
        }
        if (!externalGrants.hasAdministrator()) {
            throw new IllegalStateException(
                    "vadmin.identity.bootstrap-subjects must name the initial administrator when external identity is enabled");
        }
    }

    private List<String> bootstrapSubjects() {
        String configured = environment.getProperty("vadmin.identity.bootstrap-subjects", "");
        return Arrays.stream(configured.split(",")).map(String::strip).filter(value -> !value.isEmpty()).toList();
    }

    private String bootstrapPassword() {
        String password = environment.getProperty("vadmin.bootstrap.password");
        if (password == null || password.isBlank()) {
            password = environment.getProperty("APP_BOOTSTRAP_PASSWORD");
        }
        if (password != null && !password.isBlank()) {
            return password;
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            return "change-me";
        }
        throw new IllegalStateException(
                "APP_BOOTSTRAP_PASSWORD is required to create the initial administrator on an empty IAM schema");
    }
}
