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
import static org.junit.Assert.fail;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter actions")
public class RSQLActionFieldsTest extends AbstractJpaIntegrationTest {

    private JpaTarget target;
    private JpaAction action;

    @Before
    public void setupBeforeTest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("daA");
        target = (JpaTarget) targetManagement
                .create(entityFactory.target().create().controllerId("targetId123").description("targetId123"));
        action = new JpaAction();
        action.setActionType(ActionType.SOFT);
        action.setDistributionSet(dsA);
        action.setTarget(target);
        action.setStatus(Status.RUNNING);
        target.addAction(action);

        actionRepository.save(action);
        for (int i = 0; i < 10; i++) {
            final JpaAction newAction = new JpaAction();
            newAction.setActionType(ActionType.SOFT);
            newAction.setDistributionSet(dsA);
            newAction.setActive(i % 2 == 0);
            newAction.setStatus(Status.RUNNING);
            newAction.setTarget(target);
            actionRepository.save(newAction);
            target.addAction(newAction);
        }

    }

    @Test
    @Description("Test filter action by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(ActionFields.ID.name() + "==" + action.getId(), 1);
        assertRSQLQuery(ActionFields.ID.name() + "==noExist*", 0);
        assertRSQLQuery(ActionFields.ID.name() + "=in=(" + action.getId() + ",1000000)", 1);
        assertRSQLQuery(ActionFields.ID.name() + "=out=(" + action.getId() + ",1000000)", 10);
    }

    @Test
    @Description("Test action by status")
    public void testFilterByParameterStatus() {
        assertRSQLQuery(ActionFields.STATUS.name() + "==pending", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "=in=(pending)", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "=out=(pending)", 6);

        try {
            assertRSQLQuery(ActionFields.STATUS.name() + "==true", 5);
            fail("Missing expected RSQLParameterUnsupportedFieldException because status cannot be compared with 'true'");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Slice<Action> findEnitity = deploymentManagement.findActionsByTarget(rsqlParam, target.getControllerId(),
                new PageRequest(0, 100));
        final long countAllEntities = deploymentManagement.countActionsByTarget(rsqlParam, target.getControllerId());
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
