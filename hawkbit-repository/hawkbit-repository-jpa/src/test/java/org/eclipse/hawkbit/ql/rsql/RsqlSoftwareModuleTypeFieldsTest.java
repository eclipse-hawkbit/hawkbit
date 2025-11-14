/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleTypeFields;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.vendor.Database;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL filter software module test type
 */
class RsqlSoftwareModuleTypeFieldsTest extends AbstractJpaIntegrationTest {

    /**
     * Test filter software module test type by id
     */
    @Test
    void testFilterByParameterId() {
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "==" + osType.getId(), 1);
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "!=" + osType.getId(), 2);
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "!=" + -1, 3);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "=in=(" + osType.getId() + ",1000000)", 1);
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "=out=(" + osType.getId() + ",1000000)", 2);
    }

    /**
     * Test filter software module test type by name
     */
    @Test
    void testFilterByParameterName() {
        assertRSQLQuery(SoftwareModuleTypeFields.NAME.name() + "==" + Constants.SMT_DEFAULT_OS_NAME, 1);
        assertRSQLQuery(SoftwareModuleTypeFields.NAME.name() + "!=" + Constants.SMT_DEFAULT_OS_NAME, 2);
    }

    /**
     * Test filter software module test type by description
     */
    @Test
    void testFilterByParameterDescription() {
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "==''", 0);
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "!=''", 3);
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "==Updated*", 3);
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "!=Updated*", 0);
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "==noExist*", 0);
    }

    /**
     * Test filter software module test type by key
     */
    @Test
    void testFilterByParameterKey() {
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "==os", 1);
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "!=os", 2);
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "=in=(os)", 1);
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "=out=(os)", 2);
    }

    /**
     * Test filter software module test type by max
     */
    @Test
    void testFilterByMaxAssignment() {
        assertRSQLQuery(SoftwareModuleTypeFields.MAXASSIGNMENTS.name() + "==1", 2);
        assertRSQLQuery(SoftwareModuleTypeFields.MAXASSIGNMENTS.name() + "!=1", 1);
    }

    private void assertRSQLQuery(final String rsql, final long expectedEntity) {
        final Page<? extends SoftwareModuleType> find = softwareModuleTypeManagement.findByRsql(rsql, PageRequest.of(0, 100));
        final long countAll = find.getTotalElements();
        assertThat(find).isNotNull();
        assertThat(countAll).isEqualTo(expectedEntity);
    }
}