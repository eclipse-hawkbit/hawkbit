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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter software module metadata")
public class RSQLSoftwareModuleMetadataFieldsTest extends AbstractJpaIntegrationTest {

    private Long softwareModuleId;

    @Before
    public void setupBeforeTest() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);

        softwareModuleId = softwareModule.getId();

        final List<MetaData> metadata = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            metadata.add(entityFactory.generateMetadata("" + i, "" + i));
        }

        softwareModuleManagement.createMetaData(softwareModule.getId(), metadata);

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

        final Page<SoftwareModuleMetadata> findEnitity = softwareModuleManagement
                .findMetaDataByRsql(new PageRequest(0, 100), softwareModuleId, rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
