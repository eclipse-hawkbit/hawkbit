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

import org.flywaydb.core.Flyway;

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
 *     <li>validate: validate the database, default, only validates db and throws {@link org.flywaydb.core.api.exception.FlywayValidateException} if not in sync</li>
 * </ul>
 * Note: could also be configured using default flyway env properties
 */
public class HawkbitFlywayDbInit {

    public static final String MIGRATE = "migrate";

    public static final String URL = prop("url", "jdbc:h2:mem:hawkbit");
    public static final String USER = prop("username", "sa");
    public static final String PASSWORD = prop("password", "");
    public static final String MODE = prop("mode", "validate");

    public static void main(final String[] args) {
        final Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .cleanDisabled(true)
                .table("schema_version")
                .sqlMigrationSuffixes(prop("sql-migration-suffixes", suffix(URL)) + ".sql")
                .validateOnMigrate(true)
                .envVars()
                .load();

        if (MODE.equals(MIGRATE)) {
            flyway.migrate();
        } else {
            flyway.validate();
        }
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

    private static final Pattern PATTERN = Pattern.compile("jdbc:(?<database>\\w+):(\\\\)?.*", Pattern.CASE_INSENSITIVE);

    private static String suffix(final String url) {
        final Matcher matcher = PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid db url: " + url);
        }
        final String dbUpperCase = matcher.group("database").toUpperCase();
        return "MARIADB".equals(dbUpperCase) ? "MYSQL" : dbUpperCase;
    }
}