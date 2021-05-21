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
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter actions")
public class RSQLActionFieldsTest extends AbstractJpaIntegrationTest {

    private JpaTarget target;
    private JpaAction action;

    @BeforeEach
    public void setupBeforeTest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("daA");
        target = (JpaTarget) targetManagement
                .create(entityFactory.target().create().controllerId("targetId123").description("targetId123"));
        action = new JpaAction();
        action.setActionType(ActionType.SOFT);
        action.setDistributionSet(dsA);
        action.setTarget(target);
        action.setStatus(Status.RUNNING);
        action.setWeight(45);
        action.setInitiatedBy(tenantAware.getCurrentUsername());
        target.addAction(action);

        actionRepository.save(action);
        for (int i = 0; i < 10; i++) {
            final JpaAction newAction = new JpaAction();
            newAction.setActionType(ActionType.SOFT);
            newAction.setDistributionSet(dsA);
            newAction.setActive((i % 2) == 0);
            newAction.setStatus(Status.RUNNING);
            newAction.setTarget(target);
            newAction.setWeight(45);
            newAction.setInitiatedBy(tenantAware.getCurrentUsername());
            actionRepository.save(newAction);
            target.addAction(newAction);
        }

    }

    @Test
    @Description("Test filter action by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(ActionFields.ID.name() + "==" + action.getId(), 1);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + action.getId(), 10);
        assertRSQLQuery(ActionFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + -1, 11);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(ActionFields.ID.name() + "==*", 11);
        assertRSQLQuery(ActionFields.ID.name() + "==noexist*", 0);
        assertRSQLQuery(ActionFields.ID.name() + "=in=(" + action.getId() + ",10000000)", 1);
        assertRSQLQuery(ActionFields.ID.name() + "=out=(" + action.getId() + ",10000000)", 10);
    }

    @Test
    @Description("Test action by status")
    public void testFilterByParameterStatus() {
        assertRSQLQuery(ActionFields.STATUS.name() + "==pending", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "!=pending", 6);
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
                PageRequest.of(0, 100));
        final long countAllEntities = deploymentManagement.countActionsByTarget(rsqlParam, target.getControllerId());
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
