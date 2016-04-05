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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter software module metadata")
public class RSQLSoftwareModuleMetadataFieldsTest extends AbstractIntegrationTest {

    private Long softwareModuleId;

    @Before
    public void setupBeforeTest() {
        final SoftwareModule softwareModule = softwareManagement.createSoftwareModule(
                new SoftwareModule(TestDataUtil.findOrCreateSoftwareModuleType(softwareManagement, "application"),
                        "application", "1.0.0", "Desc", "vendor Limited, California"));
        softwareModuleId = softwareModule.getId();

        final List<SoftwareModuleMetadata> metadata = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            metadata.add(new SoftwareModuleMetadata("" + i, softwareModule, "" + i));
        }

        softwareManagement.createSoftwareModuleMetadata(metadata);

    }

    @Test
    @Description("Test filter software module metadata by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "==1", 1);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "!=1", 4);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "=in=(1,2)", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "=out=(1,2)", 3);
    }

    @Test
    @Description("Test fitler software module metadata status by value")
    public void testFilterByParameterValue() {
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "==1", 1);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "!=1", 4);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "=in=(1,2)", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "=out=(1,2)", 3);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Page<SoftwareModuleMetadata> findEnitity = softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(softwareModuleId,
                        RSQLUtility.parse(rsqlParam, SoftwareModuleMetadataFields.class), new PageRequest(0, 100));
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
