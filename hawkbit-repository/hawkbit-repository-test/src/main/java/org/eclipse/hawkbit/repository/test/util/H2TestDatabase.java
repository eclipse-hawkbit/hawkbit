/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import org.junit.jupiter.api.extension.Extension;

/**
 * An {@link Extension} for creating and dropping H2 schemas if tests are set up with H2.
 */
public class H2TestDatabase extends AbstractSqlTestDatabase {

    public H2TestDatabase(final DatasourceContext context) {
        super(context);
    }

    @Override
    public H2TestDatabase createRandomSchema() {
        // do nothing, since H2 is in memory
        return this;
    }

    @Override
    protected void dropRandomSchema() {
        // do nothing, since H2 is in memory
    }

    @Override
    protected String getRandomSchemaUri() {
        return "jdbc:h2:mem:" + context.getRandomSchemaName() + ";MODE=MySQL;";
    }
}