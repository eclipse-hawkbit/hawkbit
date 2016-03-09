/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter software module test type")
public class RSQLSoftwareModuleTypeFieldsTest extends AbstractIntegrationTest {

    @Test
    @Description("Test filter software module test type by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(SoftwareModuleTypeFields.ID.name() + "==*", 3);
    }

    @Test
    @Description("Test filter software module test type by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(SoftwareModuleTypeFields.NAME.name() + "==ECL*", 3);
    }

    @Test
    @Description("Test filter software module test type by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "==Updated*", 3);
        assertRSQLQuery(SoftwareModuleTypeFields.DESCRIPTION.name() + "==noExist*", 0);
    }

    @Test
    @Description("Test filter software module test type by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "==os", 1);
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "=in=(os)", 1);
        assertRSQLQuery(SoftwareModuleTypeFields.KEY.name() + "=out=(os)", 2);
    }

    @Test
    @Description("Test filter software module test type by max")
    public void testFilterByMaxAssignment() {
        assertRSQLQuery(SoftwareModuleTypeFields.MAX.name() + "==1", 3);
    }

    private void assertRSQLQuery(final String rsqlParam, final long excpectedEntity) {
        final Page<SoftwareModuleType> find = softwareManagement.findSoftwareModuleTypesByPredicate(
                RSQLUtility.parse(rsqlParam, SoftwareModuleTypeFields.class), new PageRequest(0, 100));
        final long countAll = find.getTotalElements();
        assertThat(find).isNotNull();
        assertThat(countAll).isEqualTo(excpectedEntity);
    }
}
