/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter rollout group")
public class RSQLRolloutGroupFields extends AbstractJpaIntegrationTest {

    private Long rolloutGroupId;
    private Rollout rollout;

    @Before
    public void seuptBeforeTest() {
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");
        rollout = rolloutManagement.get(rollout.getId()).get();

        this.rolloutGroupId = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent()
                .get(0).getId();
    }

    @Test
    @Description("Test filter rollout group by  id")
    public void testFilterByParameterId() {
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==" + rolloutGroupId, 1);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "=in=(" + rolloutGroupId + ")", 1);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "=out=(" + rolloutGroupId + ")", 3);
    }

    @Test
    @Description("Test filter rollout group by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==group-1", 1);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==*", 4);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=in=(group-1,group-2)", 2);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=out=(group-1,group-2)", 2);
    }

    @Test
    @Description("Test filter rollout group by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group-1", 1);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group*", 4);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=in=(group-1,notexist)", 1);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=out=(group-1,notexist)", 3);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expcetedTargets) {
        final Page<RolloutGroup> findTargetPage = rolloutGroupManagement.findByRolloutAndRsql(new PageRequest(0, 100),
                rollout.getId(), rsqlParam);
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expcetedTargets);
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId,
            final String targetFilterQuery) {
        return rolloutManagement.create(
                entityFactory.rollout().create()
                        .set(distributionSetManagement.get(distributionSetId).get()).name(name)
                        .targetFilterQuery(targetFilterQuery),
                amountGroups, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());
    }
}
