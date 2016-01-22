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

import java.util.Arrays;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - RSQL filtering")
@Stories("RSQL filter distribution set")
public class RSQLDistributionSetFieldTest extends AbstractIntegrationTest {

    @Before
    public void seuptBeforeTest() {

        final DistributionSet ds = TestDataUtil.generateDistributionSet("DS", softwareManagement,
                distributionSetManagement);
        ds.setDescription("DS");
        ds.getMetadata().add(new DistributionSetMetadata("metaKey", ds, "metaValue"));
        distributionSetManagement.updateDistributionSet(ds);

        final DistributionSet ds2 = TestDataUtil.generateDistributionSets("NewDS", 3, softwareManagement,
                distributionSetManagement).get(0);

        ds2.setDescription("DS2");
        ds2.getMetadata().add(new DistributionSetMetadata("metaKey", ds2, "value"));
        distributionSetManagement.updateDistributionSet(ds2);

        final DistributionSetTag targetTag = tagManagement.createDistributionSetTag(new DistributionSetTag("Tag1"));
        tagManagement.createDistributionSetTag(new DistributionSetTag("Tag2"));
        tagManagement.createDistributionSetTag(new DistributionSetTag("Tag3"));
        tagManagement.createDistributionSetTag(new DistributionSetTag("Tag4"));

        distributionSetManagement.assignTag(Arrays.asList(ds.getId(), ds2.getId()), targetTag);
    }

    @Test
    @Description("Test filter distribution set by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(DistributionSetFields.ID.name() + "==*", 4);
    }

    @Test
    @Description("Test filter distribution set by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==*DS", 4);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=out=(DS,notexist)", 3);
    }

    @Test
    @Description("Test filter distribution set by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS*", 2);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=out=(DS,notexist)", 3);
    }

    @Test
    @Description("Test filter distribution set by version")
    public void testFilterByParameterVersion() {
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "==v1.0", 2);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "!=v1.0", 2);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "=in=(v1.0,v1.1)", 3);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "=out=(v1.0,error)", 2);
    }

    @Test
    @Description("Test filter distribution set by complete property")
    public void testFilterByAttribute() {
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==true", 4);
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=in=(true)", 4);
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=out=(true)", 0);
    }

    @Test
    @Description("Test filter distribution set by tag")
    public void testFilterByTag() {
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==T*", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=out=(Tag1,notexist)", 0);
    }

    @Test
    @Description("Test filter distribution set by type")
    public void testFilterByType() {
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==ecl_os_app_jvm", 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=in=(ecl_os_app_jvm,ecl)", 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=out=(ecl_os_app_jvm)", 0);
    }

    @Test
    @Description("")
    public void testFilterByMetadata() {
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=in=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".notExist==metaValue", 0);

    }

    private void assertRSQLQuery(final String rsqlParam, final long excpectedEntity) {
        final Page<DistributionSet> find = distributionSetManagement.findDistributionSetsAll(
                RSQLUtility.parse(rsqlParam, DistributionSetFields.class), new PageRequest(0, 100), false);
        final long countAll = find.getTotalElements();
        assertThat(find).isNotNull();
        assertThat(countAll).isEqualTo(excpectedEntity);
    }
}
