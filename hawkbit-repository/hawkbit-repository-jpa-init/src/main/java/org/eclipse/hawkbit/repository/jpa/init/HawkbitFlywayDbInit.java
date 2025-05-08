/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.init;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.ErrorCode;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;

/**
 * hawkBit Flyway db init configuration. Could be configured with "hawkbit.db.*" or "spring.database.*" environment or system properties,
 * with keys:
 * <ul>
 *     <li>mode: <code>migrate</code> or <code>validate</code> (default)</li>
 *     <li>url: database url</li>
 *     <li>username: database user - shall have the necessary permissions</li>
 *     <li>password: database user's password</li>
 *     <li>sql-migration-suffixes: flyway 'sqlMigrationSuffixes' if not the default ones (&lt;upper case database (mariadb -> mysql)&gt;.sql) </li>
 * </ul>
 *
 * Where:
 * <ol>
 *     <li>Environment properties takes precedence over the system properties</li>
 *     <li>The "hawkbit.db.*" properties take precedence over the "spring.database.*" properties</li>
 * </ol>
 *
 * There are two modes:
 * <ul>
 *     <li>migrate: migrate the database, only when started with parameter with key <code>mode</code> (environment or system property)</li>
 *     <li>validate: validate the database, default, only validates db exits with error code if not in sync or configuration is bad</li>
 * </ul>
 * Note: could also be configured using default flyway env properties
 * <p/>
 * Exit codes:
 * <ul>
 *     <li>{@link HawkbitFlywayDbInit#EXIT_CODE_SUCCESS} (0): success (on validate - db schema is valid)</li>
 *     <li>{@link HawkbitFlywayDbInit#EXIT_CODE_FAILED} (1): failed (on validate this means not the db schema is invalid but there is other
 *            error - e.g. db connection failed)(</li>
 *     <li>{@link HawkbitFlywayDbInit#EXIT_CODE_VALIDATE_FAILED} (2): only on validate - validation failed, e.d. db schema doesn't match
 *            the target schema. It corresponds on {@link org.flywaydb.core.api.exception.FlywayValidateException}</li>
 * </ul>
 * Note: {@link HawkbitFlywayDbInit#EXIT_CODE_VALIDATE_FAILED} doesn't really mean it is an error, it in general is most likely that the state of the db schema - it doesn't match the target versions.
 */
@Slf4j
public class HawkbitFlywayDbInit {

    public static final int EXIT_CODE_SUCCESS = 0;
    public static final int EXIT_CODE_FAILED = 1; // default exit code when error happens
    public static final int EXIT_CODE_VALIDATE_FAILED = 2;

    public static final String MIGRATE = "migrate";
    public static final String VALIDATE = "validate";

    private static final String MODE = prop("mode", VALIDATE);

    private static final String URL = prop("url", "jdbc:h2:mem:hawkbit;MODE=LEGACY;");
    private static final String USER = prop("username", "sa");
    private static final String PASSWORD = prop("password", "");
    private static final String TABLE = prop("table", "schema_version");

    private static final String[] LOCATIONS = prop("locations", "db/migration").split(",");
    private static final String SQL_MIGRATION_SUFFIXES = prop("sql-migration-suffixes", suffix(URL)) + ".sql";

    private static final String EXCEPTION_MESSAGE = "Exception message: {}";

    public static void main(final String[] args) {
        final Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .table(TABLE)
                .locations(LOCATIONS)
                .sqlMigrationSuffixes(SQL_MIGRATION_SUFFIXES)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .envVars()
                .load();
        log.info(
                "Start ({}): {}@{}, table: {}, locations: {}, sql-migration-suffixes: {}",
                MODE, USER, URL, TABLE, LOCATIONS, SQL_MIGRATION_SUFFIXES);

        if (MODE.equals(MIGRATE)) {
            try {
                flyway.migrate();
            } catch (final FlywayException e) {
                final ErrorCode errorCode = e.getErrorCode();
                final int errorCodeOrdinal = errorCode instanceof CoreErrorCode coreErrorCode ? coreErrorCode.ordinal() : -1;
                log.error("Flyway migrate failed: error code = {}, error code ordinal = {}", errorCode, errorCodeOrdinal);
                log.debug(EXCEPTION_MESSAGE, e.getMessage());
                System.exit(EXIT_CODE_FAILED); // migration failed
            } catch (final Throwable e) {
                log.error("Flyway validate failed (undeclared exception): ", e);
                System.exit(EXIT_CODE_FAILED); // migration failed with undeclared exception
            }
        } else {
            try {
                flyway.validate();
            } catch (final FlywayValidateException e) {
                final ErrorCode errorCode = e.getErrorCode();
                final int errorCodeOrdinal = errorCode instanceof CoreErrorCode coreErrorCode ? coreErrorCode.ordinal() : -1;
                log.error("Flyway validate failed (FlywayValidateException): error code = {}, error code ordinal = {}", errorCode, errorCodeOrdinal);
                log.info(EXCEPTION_MESSAGE, e.getMessage());
                System.exit(EXIT_CODE_VALIDATE_FAILED); // validation failed, not real error but tables are not valid
            } catch (final FlywayException e) {
                final ErrorCode errorCode = e.getErrorCode();
                final int errorCodeOrdinal = errorCode instanceof CoreErrorCode coreErrorCode ? coreErrorCode.ordinal() : -1;
                log.error("Flyway validate failed: error code = {}, error code ordinal = {}", errorCode, errorCodeOrdinal);
                log.info(EXCEPTION_MESSAGE, e.getMessage());
                System.exit(EXIT_CODE_FAILED); // validation failed
            } catch (final Throwable e) {
                log.error("Flyway validate failed (undeclared exception): ", e);
                System.exit(EXIT_CODE_FAILED); // validation failed with undeclared exception
            }
        }

        System.exit(EXIT_CODE_SUCCESS);
    }

    private static String prop(final String key, final String defaultValue) {
        String value = env(key);
        if (value == null) {
            value = sys(key);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static String env(final String key) {
        final String value = env0("hawkbit.db." + key);
        return value == null ? env0("spring.datasource." + key) : value;
    }

    private static String env0(final String property) {
        String value = System.getenv(property);
        if (value == null) {
            value = System.getenv(property.toUpperCase());
        }
        if (value == null) {
            value = System.getenv(property.replace('.', '_'));
        }
        if (value == null) {
            value = System.getenv(property.replace('.', '_').toUpperCase());
        }
        return value;
    }

    private static String sys(final String key) {
        return System.getProperty("hawkbit.db." + key, System.getProperty("spring.datasource." + key));
    }

    private static String suffix(final String url) {
        final Matcher matcher = Pattern.compile("jdbc:(?<database>\\w+):(\\\\)?.*", Pattern.CASE_INSENSITIVE).matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid db url: " + url);
        }
        final String dbUpperCase = matcher.group("database").toUpperCase();
        return "MARIADB".equals(dbUpperCase) ? "MYSQL" : dbUpperCase;
    }
}