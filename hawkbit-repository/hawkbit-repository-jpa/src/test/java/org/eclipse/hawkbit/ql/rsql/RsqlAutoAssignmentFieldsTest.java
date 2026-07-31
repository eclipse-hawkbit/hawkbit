/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.qfields.AutoAssignmentFields;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL filter auto assignment
 */
class RsqlAutoAssignmentFieldsTest extends AbstractJpaIntegrationTest {

    private static final String OTHER_VERSION = "2.0";

    private AutoAssignment autoAssignment1;
    private AutoAssignment autoAssignment2;

    @BeforeEach
    void setupBeforeTest() {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("AutoAssignedDs_1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("AutoAssignedDs_2");
        // a third auto assignment referencing a distribution set with a different name and version
        final DistributionSet ds3 = testdataFactory.createDistributionSet("OtherDs", OTHER_VERSION, false);

        autoAssignment1 = autoAssignmentManagement.create(AutoAssignmentManagement.Create.builder().name("aa_a")
                .targetFilterQuery("name==*").distributionSet(ds1).actionType(ActionType.SOFT).build());
        autoAssignment2 = autoAssignmentManagement.create(AutoAssignmentManagement.Create.builder().name("aa_b")
                .targetFilterQuery("name==*").distributionSet(ds2).build());
        autoAssignmentManagement.create(AutoAssignmentManagement.Create.builder().name("aa_c")
                .targetFilterQuery("name==*").distributionSet(ds3).build());

        assertEquals(3L, autoAssignmentManagement.count());
    }

    /**
     * Test filter auto assignment by auto assigned ds name
     */
    @Test
    void testFilterByAutoAssignedDsName() {
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name=="
                + autoAssignment1.getDistributionSet().getName(), 1);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name=="
                + autoAssignment2.getDistributionSet().getName(), 1);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name==AutoAssignedDs_*", 2);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name==noExist*", 0);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name=in=("
                + autoAssignment1.getDistributionSet().getName() + ",notexist)", 1);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".name=out=("
                + autoAssignment1.getDistributionSet().getName() + ",notexist)", 2);
    }

    /**
     * Test filter auto assignment by auto assigned ds version
     */
    @Test
    void testFilterByAutoAssignedDsVersion() {
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".version=="
                + TestdataFactory.DEFAULT_VERSION, 2);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".version==*1*", 2);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".version==noExist*", 0);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".version=in=("
                + TestdataFactory.DEFAULT_VERSION + ",notexist)", 2);
        assertRSQLQuery(AutoAssignmentFields.DISTRIBUTIONSET.name() + ".version=out=("
                + TestdataFactory.DEFAULT_VERSION + ",notexist)", 1);
    }

    private void assertRSQLQuery(final String rsql, final long expectedAutoAssignmentSize) {
        final Page<? extends AutoAssignment> autoAssignmentPage = autoAssignmentManagement.findAutoAssignmentByRsql(rsql, PAGE);
        assertThat(autoAssignmentPage).isNotNull();
        assertThat(autoAssignmentPage.getTotalElements()).isEqualTo(expectedAutoAssignmentSize);
    }
}