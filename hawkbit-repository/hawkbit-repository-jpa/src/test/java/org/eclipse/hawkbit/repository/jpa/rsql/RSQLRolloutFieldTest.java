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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Feature("Component Tests - Repository")
@Story("RSQL filter rollout group")
class RSQLRolloutFieldTest extends AbstractJpaIntegrationTest {

    private Rollout rollout;

    @BeforeEach
    void setupBeforeTest() {
        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");
        rollout = rolloutManagement.get(rollout.getId()).get();
    }

    @Test
    @Description("Test filter rollout by distrbution set type id")
    void testFilterByDsType() {
        assertRSQLQuery(RolloutFields.DISTRIBUTIONSET.name() + ".type.id" + "==" + rollout.getDistributionSet().getType().getId() + 1, 0);
        assertRSQLQuery(RolloutFields.DISTRIBUTIONSET.name() + ".type.id" + "!=" + rollout.getDistributionSet().getType().getId() + 1, 1);
        assertRSQLQuery(RolloutFields.DISTRIBUTIONSET.name() + ".type.id" + "==" + rollout.getDistributionSet().getType().getId(), 1);
        assertRSQLQuery(RolloutFields.DISTRIBUTIONSET.name() + ".type.id" + "!=" + rollout.getDistributionSet().getType().getId(), 0);
    }

    private void assertRSQLQuery(final String rsql, final long expectedTargets) {
        final Page<Rollout> findTargetPage = rolloutManagement.findByRsql(rsql, false, PageRequest.of(0, 100));
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expectedTargets);
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId, final String targetFilterQuery) {
        return rolloutManagement.create(
                entityFactory.rollout().create().distributionSetId(
                        distributionSetManagement.get(distributionSetId).get()).name(name).targetFilterQuery(targetFilterQuery),
                amountGroups,
                false,
                new RolloutGroupConditionBuilder().withDefaults().successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());
    }
}