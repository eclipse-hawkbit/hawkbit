/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("Repository Model")
public class ModelEqualsHashcodeTest extends AbstractIntegrationTest {

    @Test
    @Description("Verfies that different objects even with identical primary key, version and tenant "
            + "return different hash codes.")
    public void differentEntitiesReturnDifferentHashCodes() {
        assertThat(new Action().hashCode()).as("action should have different hashcode than action status")
                .isNotEqualTo(new ActionStatus().hashCode());
        assertThat(new DistributionSet().hashCode())
                .as("Distribution set should have different hashcode than software module")
                .isNotEqualTo(new SoftwareModule().hashCode());
        assertThat(new DistributionSet().hashCode())
                .as("Distribution set should have different hashcode than action status")
                .isNotEqualTo(new ActionStatus().hashCode());
        assertThat(new DistributionSetType().hashCode())
                .as("Distribution set type should have different hashcode than action status")
                .isNotEqualTo(new ActionStatus().hashCode());
    }

    @Test
    @Description("Verfies that different object even with identical primary key, version and tenant "
            + "are not equal.")
    public void differentEntitiesAreNotEqual() {
        assertThat(new Action().equals(new ActionStatus())).as("action equals action status").isFalse();
        assertThat(new DistributionSet().equals(new SoftwareModule())).as("Distribution set equals software module")
                .isFalse();
        assertThat(new DistributionSet().equals(new ActionStatus())).as("Distribution set equals action status")
                .isFalse();
        assertThat(new DistributionSetType().equals(new ActionStatus()))
                .as("Distribution set type equals action status").isFalse();
    }

    @Test
    @Description("Verfies that updated entities are not equal.")
    public void changedEntitiesAreNotEqual() {
        final SoftwareModuleType type = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test", "test", "test", 1));
        assertThat(type).as("persited entity is not equal to regular object")
                .isNotEqualTo(new SoftwareModuleType("test", "test", "test", 1));

        type.setDescription("another");
        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(type);
        assertThat(type).as("Changed entity is not equal to the previous version").isNotEqualTo(updated);
    }

    @Test
    @Description("Verify that no proxy of the entity manager has an influence on the equals or hashcode result.")
    public void managedEntityIsEqualToUnamangedObjectWithSameKey() {
        final SoftwareModuleType type = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test", "test", "test", 1));

        final SoftwareModuleType mock = new SoftwareModuleType("test", "test", "test", 1);
        mock.setId(type.getId());
        mock.setOptLockRevision(type.getOptLockRevision());
        mock.setTenant(type.getTenant());

        assertThat(type).as("managed entity is equal to regular object with same content").isEqualTo(mock);
        assertThat(type.hashCode()).as("managed entity has same hash code as regular object with same content")
                .isEqualTo(mock.hashCode());
    }

    @Test
    @Description("Verfies that updated entities do not have the same hashcode.")
    public void updatedEntitiesHaveDifferentHashcodes() {
        final SoftwareModuleType type = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test", "test", "test", 1));
        assertThat(type.hashCode()).as("persited entity does not have same hashcode as regular object")
                .isNotEqualTo(new SoftwareModuleType("test", "test", "test", 1).hashCode());

        final int beforeChange = type.hashCode();
        type.setDescription("another");
        assertThat(type.hashCode())
                .as("Changed entity has no different hashcode than the previous version until updated in repository")
                .isEqualTo(beforeChange);

        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(type);
        assertThat(type.hashCode()).as("Updated entity has different hashcode than the previous version")
                .isNotEqualTo(updated.hashCode());
    }

}
