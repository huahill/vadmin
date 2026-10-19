package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
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

    JdbcIamInitializer(JdbcIamStore iamStore, PasswordEncoder passwordEncoder,
                         PermissionCatalog permissionCatalog, Environment environment,
                         TransactionOperations transactions) {
        this.iamStore = Objects.requireNonNull(iamStore);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.permissionCatalog = Objects.requireNonNull(permissionCatalog);
        this.environment = Objects.requireNonNull(environment);
        this.transactions = Objects.requireNonNull(transactions);
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
                if (!iamStore.hasAnyUser()) {
                    throw concurrentBootstrap;
                }
                initialize(codes);
            });
        }
    }

    private void initialize(List<String> codes) {
        iamStore.synchronizePermissions(codes);
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
