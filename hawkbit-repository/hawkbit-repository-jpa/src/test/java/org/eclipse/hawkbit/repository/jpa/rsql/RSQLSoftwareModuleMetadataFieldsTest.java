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

import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter software module metadata")
public class RSQLSoftwareModuleMetadataFieldsTest extends AbstractJpaIntegrationTest {

    private Long softwareModuleId;

    @BeforeEach
    public void setupBeforeTest() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);

        softwareModuleId = softwareModule.getId();

        final List<SoftwareModuleMetadataCreate> metadata = Lists.newArrayListWithExpectedSize(5);
        for (int i = 0; i < 5; i++) {
            metadata.add(
                    entityFactory.softwareModuleMetadata().create(softwareModule.getId()).key("" + i).value("" + i));
        }

        metadata.add(entityFactory.softwareModuleMetadata().create(softwareModule.getId()).key("targetVisible")
                .value("targetVisible").targetVisible(true));
        metadata.add(entityFactory.softwareModuleMetadata().create(softwareModule.getId()).key("emptyMd")
                .targetVisible(true));

        softwareModuleManagement.createMetaData(metadata);

    }

    @Test
    @Description("Test filter software module metadata by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "==1", 1);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "!=1", 6);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "=in=(1,2)", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.KEY.name() + "=out=(1,2)", 5);
    }

    @Test
    @Description("Test fitler software module metadata by value")
    public void testFilterByParameterValue() {
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "==''", 1);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "!=''", 6);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "==1", 1);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "!=1", 6);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "=in=(1,2)", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.VALUE.name() + "=out=(1,2)", 5);
    }

    @Test
    @Description("Test fitler software module metadata by target visible")
    public void testFilterByParameterTargetVisible() {
        assertRSQLQuery(SoftwareModuleMetadataFields.TARGETVISIBLE.name() + "==true", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.TARGETVISIBLE.name() + "==false", 5);
        assertRSQLQuery(SoftwareModuleMetadataFields.TARGETVISIBLE.name() + "!=false", 2);
        assertRSQLQuery(SoftwareModuleMetadataFields.TARGETVISIBLE.name() + "!=true", 5);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Page<SoftwareModuleMetadata> findEnitity = softwareModuleManagement
                .findMetaDataByRsql(PageRequest.of(0, 100), softwareModuleId, rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
