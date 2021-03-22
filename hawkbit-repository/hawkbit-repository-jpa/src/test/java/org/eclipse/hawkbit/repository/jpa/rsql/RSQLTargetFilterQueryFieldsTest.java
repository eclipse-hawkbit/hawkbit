/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.orm.jpa.vendor.Database;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter target filter query")
public class RSQLTargetFilterQueryFieldsTest extends AbstractJpaIntegrationTest {

    private TargetFilterQuery filter1;
    private TargetFilterQuery filter2;

    @BeforeEach
    public void setupBeforeTest() throws InterruptedException {
        final String filterName1 = "filter_a";
        final String filterName2 = "filter_b";
        final String filterName3 = "filter_c";

        final DistributionSet ds1 = testdataFactory.createDistributionSet("AutoAssignedDs_1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("AutoAssignedDs_2");

        filter1 = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterName1)
                .query("name==*").autoAssignDistributionSet(ds1).autoAssignActionType(ActionType.SOFT));
        filter2 = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterName2)
                .query("name==*").autoAssignDistributionSet(ds2));
        targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName3).query("name==*"));

        assertEquals(3L, targetFilterQueryManagement.count());
    }

    @Test
    @Description("Test filter target filter query by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "==" + filter1.getId(), 1);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "!=" + filter1.getId(), 2);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "!=" + -1, 3);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "==*", 3);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "==noexist*", 0);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "=in=(" + filter1.getId() + ",10000000)", 1);
        assertRSQLQuery(TargetFilterQueryFields.ID.name() + "=out=(" + filter1.getId() + ",10000000)", 2);

    }

    @Test
    @Description("Test filter target filter query by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "==" + filter1.getName(), 1);
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "==" + filter2.getName(), 1);
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "==filter_*", 3);
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "=in=(" + filter1.getName() + ",notexist)", 1);
        assertRSQLQuery(TargetFilterQueryFields.NAME.name() + "=out=(" + filter1.getName() + ",notexist)", 2);
    }

    @Test
    @Description("Test filter target filter query by auto assigned ds name")
    public void testFilterByAutoAssignedDsName() {
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name=="
                + filter1.getAutoAssignDistributionSet().getName(), 1);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name=="
                + filter2.getAutoAssignDistributionSet().getName(), 1);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name==AutoAssignedDs_*", 2);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name==noExist*", 0);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name=in=("
                + filter1.getAutoAssignDistributionSet().getName() + ",notexist)", 1);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".name=out=("
                + filter1.getAutoAssignDistributionSet().getName() + ",notexist)", 2);
    }

    @Test
    @Description("Test filter target filter query by auto assigned ds version")
    public void testFilterByAutoAssignedDsVersion() {
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".version=="
                + TestdataFactory.DEFAULT_VERSION, 2);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".version==*1*", 2);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".version==noExist*", 0);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".version=in=("
                + TestdataFactory.DEFAULT_VERSION + ",notexist)", 2);
        assertRSQLQuery(TargetFilterQueryFields.AUTOASSIGNDISTRIBUTIONSET.name() + ".version=out=("
                + TestdataFactory.DEFAULT_VERSION + ",notexist)", 1);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedFilterQueriesSize) {
        final Page<TargetFilterQuery> findTargetFilterQueryPage = targetFilterQueryManagement.findByRsql(PAGE,
                rsqlParam);
        assertThat(findTargetFilterQueryPage).isNotNull();
        assertThat(findTargetFilterQueryPage.getTotalElements()).isEqualTo(expectedFilterQueriesSize);
    }
}
