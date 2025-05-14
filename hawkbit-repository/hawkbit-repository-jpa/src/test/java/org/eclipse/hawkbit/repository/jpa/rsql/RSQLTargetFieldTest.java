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

import java.util.Arrays;
import java.util.Map;

import jakarta.persistence.EntityManager;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;

@Feature("Component Tests - Repository")
@Story("RSQL filter target")
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
class RSQLTargetFieldTest extends AbstractJpaIntegrationTest {

    private static final String OR = ",";
    private static final String AND = ";";

    @Autowired
    protected VirtualPropertyReplacer virtualPropertyReplacer;
    @Autowired
    protected EntityManager entityManager;

    private Target target;
    private Target target2;
    private TargetType targetType1;
    private TargetType targetType2;

    @BeforeEach
    void setupBeforeTest() {
        targetType1 = targetTypeManagement
                .create(entityFactory.targetType().create()
                        .name("Type1").description("Desc. Type1")
                        .key("Type1.key"));
        targetType2 = targetTypeManagement
                .create(entityFactory.targetType().create()
                        .name("Type2").description("Desc. Type2")
                        .key("Type2.key"));

        final DistributionSet ds = testdataFactory.createDistributionSet("AssignedDs");

        final TargetTag targetTag = targetTagManagement.create(entityFactory.tag().create().name("Tag1"));
        final TargetTag targetTag2 = targetTagManagement.create(entityFactory.tag().create().name("Tag2"));
        final TargetTag targetTag3 = targetTagManagement.create(entityFactory.tag().create().name("Tag3"));
        targetTagManagement.create(entityFactory.tag().create().name("Tag4"));

        target = targetManagement.create(
                entityFactory.target().create()
                        .controllerId("targetId123")
                        .name("targetName123")
                        .description("targetDesc123"));
        target = controllerManagement.updateControllerAttributes(target.getControllerId(), Map.of("revision", "1.1"), null);
        target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), LOCALHOST);
        createTargetMetadata(target.getControllerId(), entityFactory.generateTargetMetadata("metaKey", "metaValue"));
        assignDistributionSet(ds.getId(), target.getControllerId());
        targetManagement.assignType(target.getControllerId(), targetType1.getId());

        target2 = targetManagement.create(
                entityFactory.target().create()
                        .controllerId("targetId1234")
                        .description("targetId1234"));
        target2 = controllerManagement.updateControllerAttributes(target2.getControllerId(), Map.of("revision", "1.2"), null);
        targetManagement.assignType(target2.getControllerId(), targetType2.getId());
        createTargetMetadata(target2.getControllerId(), entityFactory.generateTargetMetadata("metaKey", "value"));
        target2 = controllerManagement.findOrRegisterTargetIfItDoesNotExist(target2.getControllerId(), LOCALHOST);
        targetManagement.assignTag(Arrays.asList(target.getControllerId(), target2.getControllerId()), targetTag.getId());

        final Target target3 = testdataFactory.createTarget("targetId1235");

        final Target target4 = testdataFactory.createTarget("targetId1236");
        testdataFactory.createTarget("targetId1237");
        targetManagement.assignTag(Arrays.asList(target3.getControllerId(), target4.getControllerId()), targetTag2.getId());

        targetManagement.assignTag(
                Arrays.asList(target.getControllerId(), target3.getControllerId(), target4.getControllerId()),
                targetTag3.getId());
    }

    @Test
    @Description("Test filter target by (controller) id")
    void testFilterByParameterId() {
        assertRSQLQuery(TargetFields.ID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.ID.name() + "!=targetId123", 4);
        assertRSQLQuery(TargetFields.ID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.ID.name() + "=out=(targetId123,notexist)", 4);
    }

    @Test
    @Description("Test filter target by name")
    void testFilterByParameterName() {
        assertRSQLQuery(TargetFields.NAME.name() + "==targetName123", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "==target*", 5);
        assertRSQLQuery(TargetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.NAME.name() + "!=targetName123", 4);
        assertRSQLQuery(TargetFields.NAME.name() + "=in=(targetName123,notexist)", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "=out=(targetName123,notexist)", 4);
    }

    @Test
    @Description("Test filter target by description")
    void testFilterByParameterDescription() {
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==''", 3);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "!=''", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==targetDesc123", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "!=targetDesc123", 4);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==target*", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=in=(targetDesc123,notexist)", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=out=(targetDesc123,notexist)", 4);
    }

    @Test
    @Description("Test filter target by controller id")
    void testFilterByParameterControllerId() {
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==target*", 5);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "!=targetId123", 4);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=out=(targetId123,notexist)", 4);
    }

    @Test
    @Description("Test filter target by status")
    void testFilterByParameterUpdateStatus() {
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==pending", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "!=pending", 4);
        final String rsqlNoExistStar = TargetFields.UPDATESTATUS.name() + "==noExist*";
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("update status unknown")
                .isThrownBy(() -> assertRSQLQuery(rsqlNoExistStar, 0));
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=in=(pending,error)", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=out=(pending,error)", 4);
    }

    @Test
    @Description("Test filter target by attribute")
    void testFilterByAttribute() {
        controllerManagement.updateControllerAttributes(
                testdataFactory.createTarget().getControllerId(),
                Map.of(
                        "test.dot", "value.dot",
                        "test.null", "null"),
                null);

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot>=value.dos", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot==value.dot", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null==null", 1); // "null" check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a==null", 0); // "null" check

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot=is=value.dot", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=is=null", 5); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a=is=null", 1 + 5); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot=eq=value.dot", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=eq=null", 5); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a=eq=null", 1 + 5); // null check

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot!=value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null!=null", 0); // "null" check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null!=null2", 1); // value check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a!=null", 0); // "null" check

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot=not=value.dot", 5);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=not=null", 1); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=not=null2", 1 + 5); // value check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a=not=null", 0); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot=ne=value.dot", 5);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=ne=null", 1); // null check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.null=ne=null2", 1 + 5); // value check
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.n/a=ne=null", 0); // null check

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision!=1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1*", 2);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==noExist*", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=in=(1.1,notexist)", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=out=(1.1)", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.dot*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + "..==value.dot", 0);
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + ".==value.dot");
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + "*==value.dot");
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + "==value.dot");
    }

    @Test
    @Description("Test filter target by assigned ds name")
    void testFilterByAssignedDsName() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==AssignedDs", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==A*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==noExist*", 0);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=in=(AssignedDs,notexist)", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=out=(AssignedDs,notexist)", 4);
    }

    @Test
    @Description("Test filter target by assigned ds version")
    void testFilterByAssignedDsVersion() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==*1*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==noExist*", 0);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version=in=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version=out=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 4);
    }

    @Test
    @Description("Test filter target by tag name")
    void testFilterByTag() {
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag1", 3);
        assertRSQLQuery(TargetFields.TAG.name() + "==T*", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "!=T*", 1);
        assertRSQLQuery(TargetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.TAG.name() + "!=notexist", 5);
        assertRSQLQuery(TargetFields.TAG.name() + "==''", 1);
        assertRSQLQuery(TargetFields.TAG.name() + "!=''", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(null)", 0);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(Tag1,notexist)", 3);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(null)", 5);
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1" + OR + TargetFields.TAG.name() + "==Tag2", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag2" + AND + TargetFields.TAG.name() + "==Tag3", 1);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag2" + OR + TargetFields.TAG.name() + "!=Tag3", 3);
    }

    @Test
    @Description("Test filter target by lastTargetQuery")
    void testFilterByLastTargetQuery() {
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "==" + target.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "!=" + target.getLastTargetQuery(), 4);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target2.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target2.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=le=${NOW_TS}", 2);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=${OVERDUE_TS}", 2);
    }

    @Test
    @Description("Test filter target by metadata")
    void testFilterByMetadata() {
        createTargetMetadata(testdataFactory.createTarget().getControllerId(), entityFactory.generateTargetMetadata("key.dot", "value.dot"));

        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==null", 0); // "null" check
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist==metaValue", 0);

        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=is=metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=is=null", 4); // null check (1 of the initial five has)
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=is=*v*", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=is=noExist*", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist=is=null", 1 + 5); // null check
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=eq=metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=eq=null", 4); // null check (1 of the initial five has)
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=eq=*v*", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=eq=noExist*", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist=eq=null", 1 + 5); // null check

        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey!=metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey!=null", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist!=metaValue", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey!=notExist", 2);
//
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=not=metaValue", 1 + 4);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=not=null", 2); // null check (2 of the initial five)
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist=not=metaValue", 1 + 5);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=not=notExist", 1 + 5);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=ne=metaValue", 1 + 4);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=ne=null", 2); // null check (2 of the initial five)
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist=ne=metaValue", 1 + 5);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=ne=notExist", 1 + 5);

        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=in=(metaValue,notexist)", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.dot==value.dot", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.dot*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + "..==value.dot", 0);
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + ".==value.dot");
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + "*==value.dot");
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + "==value.dot");
    }

    @Test
    @Description("Test filter based on more complex RSQL queries")
    void testFilterByComplexQueries() {
        assertRSQLQuery(TargetFields.NAME.name() + "!=targetName123" + AND + TargetFields.METADATA.name() + ".metaKey!=value", 0);
        assertRSQLQuery(
                "(" + TargetFields.TAG.name() + "!=TAG1" + OR + TargetFields.TAG.name() + "!=TAG2)" +
                        AND + TargetFields.CONTROLLERID.name() + "!=targetId1235", 4);
    }

    @Test
    @Description("Testing allowed RSQL keys based on TargetFields definition")
    void rsqlValidTargetFields() {
        RSQLUtility.validateRsqlFor(
                "ID == '0123' and NAME == abcd and DESCRIPTION == absd and CREATEDAT =lt= 0123 and LASTMODIFIEDAT =gt= 0123" +
                        " and CONTROLLERID == 0123 and UPDATESTATUS == PENDING and IPADDRESS == 0123 and LASTCONTROLLERREQUESTAT == 0123" +
                        " and tag == beta",
                TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);
        RSQLUtility.validateRsqlFor(
                "ASSIGNEDDS.name == abcd and ASSIGNEDDS.version == 0123 and INSTALLEDDS.name == abcd and INSTALLEDDS.version == 0123",
                TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);
        RSQLUtility.validateRsqlFor(
                "ATTRIBUTE.subkey1 == test and ATTRIBUTE.subkey2 == test and METADATA.metakey1 == abcd and METADATA.metavalue2 == asdfg",
                TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);
        RSQLUtility.validateRsqlFor(
                "CREATEDAT =lt= ${NOW_TS} and LASTMODIFIEDAT =ge= ${OVERDUE_TS}",
                TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);
        RSQLUtility.validateRsqlFor(
                "ATTRIBUTE.test.dot == test and ATTRIBUTE.subkey2 == test and METADATA.test.dot == abcd and METADATA.metavalue2 == asdfg",
                TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);

        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> RSQLUtility.validateRsqlFor(
                        "wrongfield == abcd",
                        TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager));
    }

    @Test
    @Description("Test filter by target type key")
    void shouldFilterTargetsByTypeKey() {
        assertRSQLQuery("targettype." + TargetTypeFields.KEY.name() + "==" + targetType1.getKey(), 1);
        assertRSQLQuery("targettype." + TargetTypeFields.KEY.name() + "==*1.key", 1);
        assertRSQLQuery("targettype." + TargetTypeFields.KEY.name() + "!=" + targetType2.getKey(), 4);
        assertRSQLQuery("targettype." + TargetTypeFields.KEY.name() + "==noExist*", 0);
    }

    @Test
    @Description("Test filter by target type name")
    void shouldFilterTargetsByTypeName() {
        assertRSQLQuery("targettype." + TargetTypeFields.NAME.name() + "==" + targetType1.getName(), 1);
        assertRSQLQuery("targettype." + TargetTypeFields.NAME.name() + "==*1", 1);
        assertRSQLQuery("targettype." + TargetTypeFields.NAME.name() + "!=" + targetType2.getName(), 4);
        assertRSQLQuery("targettype." + TargetTypeFields.NAME.name() + "==noExist*", 0);
    }

    @Test
    @Description("Test filter by target type ID and description")
    void shouldFilterTargetsByTypeIdAndDescription() {
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> assertRSQLQuery("targettype.ID==1", 0));
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> assertRSQLQuery("targettype.description==Description", 0));
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedTargets) {
        final Slice<Target> findTargetPage = targetManagement.findByRsql(PAGE, rsqlParam);
        assertThat(findTargetPage).isNotNull();
        assertThat(findTargetPage.getNumberOfElements()).isEqualTo(expectedTargets);
        assertThat(targetManagement.countByRsql(rsqlParam)).isEqualTo(expectedTargets);
    }

    private void assertRSQLQueryThrowsException(final String rsqlParam) {
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> RSQLUtility.validateRsqlFor(
                        rsqlParam, TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager));
    }
}