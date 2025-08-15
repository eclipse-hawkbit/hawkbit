/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.vendor.Database;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL filter rollout group
 */
class RsqlRolloutGroupFieldTest extends AbstractJpaIntegrationTest {

    private Long rolloutGroupId;
    private Rollout rollout;

    @BeforeEach
    void setupBeforeTest() {
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");
        rollout = rolloutManagement.find(rollout.getId()).get();

        this.rolloutGroupId = rolloutGroupManagement.findByRollout(rollout.getId(), PAGE).getContent().get(0).getId();
    }

    /**
     * Test filter rollout group by  id
     */
    @Test
    void testFilterByParameterId() {
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==" + rolloutGroupId, 1);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "!=" + rolloutGroupId, 3);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "!=" + -1, 4);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(RolloutGroupFields.ID.name() + "=in=(" + rolloutGroupId + ",10000000)", 1);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "=out=(" + rolloutGroupId + ",10000000)", 3);
    }

    /**
     * Test filter rollout group by name
     */
    @Test
    void testFilterByParameterName() {
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==group-1", 1);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "!=group-1", 3);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==*", 4);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=in=(group-1,group-2)", 2);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=out=(group-1,group-2)", 2);
    }

    /**
     * Test filter rollout group by description
     */
    @Test
    void testFilterByParameterDescription() {
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group-1", 1);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "!=group-1", 3);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group*", 4);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=in=(group-1,notexist)", 1);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=out=(group-1,notexist)", 3);
    }

    private void assertRSQLQuery(final String rsql, final long expectedTargets) {
        final Page<RolloutGroup> findTargetPage = rolloutGroupManagement.findByRolloutAndRsql(rollout.getId(), rsql, PageRequest.of(0, 100)
        );
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expectedTargets);
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId,
            final String targetFilterQuery) {
        return rolloutManagement.create(
                Create.builder()
                        .distributionSet(distributionSetManagement.find(distributionSetId).get()).name(name).targetFilterQuery(targetFilterQuery)
                        .build(),
                amountGroups, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());
    }
}