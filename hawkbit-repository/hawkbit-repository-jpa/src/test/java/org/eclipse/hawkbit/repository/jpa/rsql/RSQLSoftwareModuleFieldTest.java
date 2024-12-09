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
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.vendor.Database;

@Feature("Component Tests - Repository")
@Story("RSQL filter software module")
public class RSQLSoftwareModuleFieldTest extends AbstractJpaIntegrationTest {

    private SoftwareModule ah;

    @BeforeEach
    public void setupBeforeTest() {
        ah = softwareModuleManagement.create(entityFactory.softwareModule().create().type(appType).name("agent-hub")
                .version("1.0.1").description("agent-hub"));
        softwareModuleManagement.create(entityFactory.softwareModule().create().type(runtimeType).name("oracle-jre")
                .version("1.7.2").description("aa"));
        softwareModuleManagement.create(
                entityFactory.softwareModule().create().type(osType).name("poki").version("3.0.2").description("aa"));
        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(osType).name("*§$%&/&%ÄÜ*Ö@").version("1.0.0")
                        .description("wildcard testing"));
        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(osType).name("noDesc").version("noDesc"));

        final JpaSoftwareModule ah2 = (JpaSoftwareModule) softwareModuleManagement.create(entityFactory.softwareModule()
                .create().type(appType).name("agent-hub2").version("1.0.1").description("agent-hub2"));

        final SoftwareModuleMetadataCreate softwareModuleMetadata = entityFactory.softwareModuleMetadata()
                .create(ah.getId()).key("metaKey").value("metaValue");
        softwareModuleManagement.createMetaData(softwareModuleMetadata);

        final SoftwareModuleMetadataCreate softwareModuleMetadata2 = entityFactory.softwareModuleMetadata()
                .create(ah2.getId()).key("metaKey").value("value");
        softwareModuleManagement.createMetaData(softwareModuleMetadata2);
    }

    @Test
    @Description("Test filter software module by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "==" + ah.getId(), 1);
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "!=" + ah.getId(), 5);
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "!=" + -1, 6);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(SoftwareModuleFields.ID.name() + "=in=(" + ah.getId() + ",1000000)", 1);
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "=out=(" + ah.getId() + ",1000000)", 5);
    }

    @Test
    @Description("Test filter software module by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==agent-hub", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "!=agent-hub", 5);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==agent-hub*", 2);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "!=agent-hub*", 4);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "=in=(agent-hub,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "=out=(agent-hub,notexist)", 5);

        //wildcard entries
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*$*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*§*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*@*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*/*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*&*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==***", 6);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*\\**", 1);
    }

    @Test
    @Description("Test filter software module by name which contain mutated vowels ")
    public void testFilterByParameterNameWithUmlaut() {
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*Ö*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*Ä*", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==*Ü*", 1);
    }

    @Test
    @Description("Test filter software module by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "==''", 1);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "!=''", 5);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "==agent-hub", 1);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "!=agent-hub", 5);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "=in=(agent-hub,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "=out=(agent-hub,notexist)", 5);
    }

    @Test
    @Description("Test filter software module by version")
    public void testFilterByParameterVersion() {
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "==1.0.1", 2);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "!=v1.0", 6);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "=in=(1.0.1,1.0.2)", 2);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "=out=(1.0.1)", 4);
    }

    @Test
    @Description("Test filter software module by type key")
    public void testFilterByType() {
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "==" + TestdataFactory.SM_TYPE_APP, 2);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "!=" + TestdataFactory.SM_TYPE_APP, 4);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "=in=(" + TestdataFactory.SM_TYPE_APP + ")", 2);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "=out=(" + TestdataFactory.SM_TYPE_APP + ")", 4);
    }

    @Test
    @Description("Test filter software module by metadata")
    public void testFilterByMetadata() {
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey!=metaValue", 1);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey!=notexist", 2);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey=in=(metaValue,value)", 2);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".notExist==metaValue", 0);

    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntity) {
        final Page<SoftwareModule> find = softwareModuleManagement.findByRsql(PageRequest.of(0, 100), rsqlParam);
        final long countAll = find.getTotalElements();
        assertThat(find).isNotNull();
        assertThat(countAll).isEqualTo(expectedEntity);
    }
}
