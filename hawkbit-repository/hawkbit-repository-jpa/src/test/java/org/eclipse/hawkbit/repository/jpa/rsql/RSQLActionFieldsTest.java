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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
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

@Feature("Component Tests - Repository")
@Story("RSQL filter actions")
class RSQLActionFieldsTest extends AbstractJpaIntegrationTest {

    private JpaTarget target;
    private JpaAction action;

    @BeforeEach
    void setupBeforeTest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("daA");
        target = (JpaTarget) targetManagement
                .create(entityFactory.target().create().controllerId("targetId123").description("targetId123"));

        action = newJpaAction(dsA, false, null);
        for (int i = 0; i < 10; i++) {
            newJpaAction(dsA, i % 2 == 0, i % 2 == 0 ? "extRef" : "extRef2");
        }
    }

    @Test
    @Description("Test filter action by id")
    void testFilterByParameterId() {
        assertRSQLQuery(ActionFields.ID.name() + "==" + action.getId(), 1);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + action.getId(), 10);
        assertRSQLQuery(ActionFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + -1, 11);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(ActionFields.ID.name() + "=in=(" + action.getId() + ",10000000)", 1);
        assertRSQLQuery(ActionFields.ID.name() + "=out=(" + action.getId() + ",10000000)", 10);
    }

    @Test
    @Description("Test action by status")
    void testFilterByParameterStatus() {
        assertRSQLQuery(ActionFields.STATUS.name() + "==pending", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "!=pending", 6);
        assertRSQLQuery(ActionFields.STATUS.name() + "=in=(pending)", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "=out=(pending)", 6);

        final String rsql = ActionFields.STATUS.name() + "==true";
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterUnsupportedFieldException because status cannot be compared with 'true'")
                .isThrownBy(() -> assertRSQLQuery(rsql, 5));
    }

    @Test
    @Description("Test action by status")
    void testFilterByParameterExtRef() {
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "==extRef", 5);
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "!=extRef", 6);
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "==extRef*", 10);
    }

    private JpaAction newJpaAction(final DistributionSet dsA, final boolean active, final String extRef) {
        final JpaAction newAction = new JpaAction();
        newAction.setActionType(ActionType.SOFT);
        newAction.setDistributionSet(dsA);
        newAction.setActive(active);
        newAction.setStatus(Status.RUNNING);
        newAction.setTarget(target);
        newAction.setWeight(45);
        newAction.setInitiatedBy(tenantAware.getCurrentUsername());
        if (extRef != null) {
            newAction.setExternalRef(extRef);
        }
        actionRepository.save(newAction);

        target.addAction(action);

        return newAction;
    }

    private void assertRSQLQuery(final String rsql, final long expectedEntities) {
        final Slice<Action> findEntity = deploymentManagement.findActionsByTarget(rsql, target.getControllerId(),
                PageRequest.of(0, 100));
        final long countAllEntities = deploymentManagement.countActionsByTarget(rsql, target.getControllerId());
        assertThat(findEntity).isNotNull();
        assertThat(findEntity.getContent()).hasSize((int)expectedEntities);
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}