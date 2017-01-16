/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.fest.assertions.core.Condition;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * {@link DistributionSetManagement} tests.
 *
 */
@Features("Component Tests - Repository")
@Stories("DistributionSet Management")
public class DistributionSetManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests the successfull module update of unused distribution set type which is in fact allowed.")
    public void updateUnassignedDistributionSetTypeModules() {
        DistributionSetType updatableType = distributionSetManagement.createDistributionSetType(
                entityFactory.distributionSetType().create().key("updatableType").name("to be deleted"));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .isEmpty();

        // add OS
        updatableType = distributionSetManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Sets.newHashSet(osType.getId()));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .containsOnly(osType);

        // add JVM
        updatableType = distributionSetManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Sets.newHashSet(runtimeType.getId()));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .containsOnly(osType, runtimeType);

        // remove OS
        updatableType = distributionSetManagement.unassignSoftwareModuleType(updatableType.getId(), osType.getId());
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .containsOnly(runtimeType);
    }

    @Test
    @Description("Tests the successfull update of used distribution set type meta data which is in fact allowed.")
    public void updateAssignedDistributionSetTypeMetaData() {
        final DistributionSetType nonUpdatableType = distributionSetManagement.createDistributionSetType(entityFactory
                .distributionSetType().create().key("updatableType").name("to be deleted").colour("test123"));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .isEmpty();
        distributionSetManagement.createDistributionSet(entityFactory.distributionSet().create().name("newtypesoft")
                .version("1").type(nonUpdatableType.getKey()));

        distributionSetManagement.updateDistributionSetType(
                entityFactory.distributionSetType().update(nonUpdatableType.getId()).description("a new description"));

        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getDescription())
                .isEqualTo("a new description");
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getColour())
                .isEqualTo("test123");
    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module addition).")
    public void addModuleToAssignedDistributionSetTypeFails() {
        final DistributionSetType nonUpdatableType = distributionSetManagement.createDistributionSetType(
                entityFactory.distributionSetType().create().key("updatableType").name("to be deleted"));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .isEmpty();
        distributionSetManagement.createDistributionSet(entityFactory.distributionSet().create().name("newtypesoft")
                .version("1").type(nonUpdatableType.getKey()));

        try {
            distributionSetManagement.assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(),
                    Sets.newHashSet(osType.getId()));
            fail("Should not have worked as DS is in use.");
        } catch (final EntityReadOnlyException e) {

        }

    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module removal).")
    public void removeModuleToAssignedDistributionSetTypeFails() {
        DistributionSetType nonUpdatableType = distributionSetManagement.createDistributionSetType(
                entityFactory.distributionSetType().create().key("updatableType").name("to be deleted"));
        assertThat(
                distributionSetManagement.findDistributionSetTypeByKey("updatableType").get().getMandatoryModuleTypes())
                        .isEmpty();

        nonUpdatableType = distributionSetManagement.assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(),
                Sets.newHashSet(osType.getId()));
        distributionSetManagement.createDistributionSet(entityFactory.distributionSet().create().name("newtypesoft")
                .version("1").type(nonUpdatableType.getKey()));

        try {
            distributionSetManagement.unassignSoftwareModuleType(nonUpdatableType.getId(), osType.getId());
            fail("Should not have worked as DS is in use.");
        } catch (final EntityReadOnlyException e) {

        }
    }

    @Test
    @Description("Tests the successfull deletion of unused (hard delete) distribution set types.")
    public void deleteUnassignedDistributionSetType() {
        final JpaDistributionSetType hardDelete = (JpaDistributionSetType) distributionSetManagement
                .createDistributionSetType(
                        entityFactory.distributionSetType().create().key("delete").name("to be deleted"));

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetManagement.deleteDistributionSetType(hardDelete.getId());

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    @Test
    @Description("Tests the successfull deletion of used (soft delete) distribution set types.")
    public void deleteAssignedDistributionSetType() {
        final JpaDistributionSetType softDelete = (JpaDistributionSetType) distributionSetManagement
                .createDistributionSetType(
                        entityFactory.distributionSetType().create().key("softdeleted").name("to be deleted"));

        assertThat(distributionSetTypeRepository.findAll()).contains(softDelete);
        distributionSetManagement.createDistributionSet(
                entityFactory.distributionSet().create().name("softdeleted").version("1").type(softDelete.getKey()));

        distributionSetManagement.deleteDistributionSetType(softDelete.getId());
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("softdeleted").get().isDeleted())
                .isEqualTo(true);
    }

    @Test
    @Description("Ensures that it is not possible to create a DS that already exists (unique constraint is on name,version for DS).")
    public void createDuplicateDistributionSetsFailsWithException() {
        testdataFactory.createDistributionSet("a");

        try {
            testdataFactory.createDistributionSet("a");
            fail("Should not have worked as DS with same UK already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Verifies that a DS is of default type if not specified explicitly at creation time.")
    public void createDistributionSetWithImplicitType() {
        final DistributionSet set = distributionSetManagement
                .createDistributionSet(entityFactory.distributionSet().create().name("newtypesoft").version("1"));

        assertThat(set.getType()).as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    @Test
    @Description("Verifies that multiple DS are of default type if not specified explicitly at creation time.")
    public void createMultipleDistributionSetsWithImplicitType() {

        final List<DistributionSetCreate> creates = Lists.newArrayListWithExpectedSize(10);
        for (int i = 0; i < 10; i++) {
            creates.add(entityFactory.distributionSet().create().name("newtypesoft" + i).version("1" + i));
        }

        final List<DistributionSet> sets = distributionSetManagement.createDistributionSets(creates);

        assertThat(sets).as("Type should be equal to default type of tenant").are(new Condition<DistributionSet>() {
            @Override
            public boolean matches(final DistributionSet value) {
                return value.getType().equals(systemManagement.getTenantMetadata().getDefaultDsType());
            }
        });

    }

    @Test
    @Description("Checks that metadata for a distribution set can be created.")
    public void createDistributionSetMetadata() {
        final String knownKey = "dsMetaKnownKey";
        final String knownValue = "dsMetaKnownValue";

        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");

        final DistributionSetMetadata metadata = new JpaDistributionSetMetadata(knownKey, ds, knownValue);
        final JpaDistributionSetMetadata createdMetadata = (JpaDistributionSetMetadata) createDistributionSetMetadata(
                ds.getId(), metadata);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    @Test
    @Description("Ensures that updates concerning the internal software structure of a DS are not possible if the DS is already assigned.")
    public void updateDistributionSetForbiddedWithIllegalUpdate() {
        // prepare data
        final Target target = testdataFactory.createTarget();

        DistributionSet ds = testdataFactory.createDistributionSet("ds-1");

        final SoftwareModule ah2 = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os2 = testdataFactory.createSoftwareModuleOs();

        // update is allowed as it is still not assigned to a target
        ds = distributionSetManagement.assignSoftwareModules(ds.getId(), Sets.newHashSet(ah2.getId()));

        // assign target
        assignDistributionSet(ds.getId(), target.getControllerId());
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());

        // not allowed as it is assigned now
        try {
            ds = distributionSetManagement.assignSoftwareModules(ds.getId(), Sets.newHashSet(os2.getId()));
            fail("Expected EntityReadOnlyException");
        } catch (final EntityReadOnlyException e) {

        }

        // not allowed as it is assigned now
        try {
            ds = distributionSetManagement.unassignSoftwareModule(ds.getId(),
                    ds.findFirstModuleByType(appType).getId());
            fail("Expected EntityReadOnlyException");
        } catch (final EntityReadOnlyException e) {

        }
    }

    @Test
    @Description("Ensures that it is not possible to add a software module that is not defined of the DS's type.")
    public void updateDistributionSetUnsupportedModuleFails() {
        final DistributionSet set = distributionSetManagement
                .createDistributionSet(entityFactory.distributionSet().create().name("agent-hub2").version("1.0.5")
                        .type(distributionSetManagement.createDistributionSetType(entityFactory.distributionSetType()
                                .create().key("test").name("test").mandatory(Lists.newArrayList(osType.getId())))
                                .getKey()));

        final SoftwareModule module = softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().name("agent-hub2").version("1.0.5").type(appType.getKey()));

        // update data
        try {
            distributionSetManagement.assignSoftwareModules(set.getId(), Sets.newHashSet(module.getId()));
            fail("Should not have worked as module type is not in DS type.");
        } catch (final UnsupportedSoftwareModuleForThisDistributionSetException e) {

        }
    }

    @Test
    @Description("Legal updates of a DS, e.g. name or description and module addition, removal while still unassigned.")
    public void updateDistributionSet() {
        // prepare data
        final Target target = testdataFactory.createTarget();
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        // update data
        // legal update of module addition
        distributionSetManagement.assignSoftwareModules(ds.getId(), Sets.newHashSet(os.getId()));
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(osType)).isEqualTo(os);

        // legal update of module removal
        distributionSetManagement.unassignSoftwareModule(ds.getId(), ds.findFirstModuleByType(appType).getId());
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(appType)).isNull();

        // Update description
        distributionSetManagement
                .updateDistributionSet(entityFactory.distributionSet().update(ds.getId()).name("a new name")
                        .description("a new description").version("a new version").requiredMigrationStep(true));
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.getDescription()).isEqualTo("a new description");
        assertThat(ds.getName()).isEqualTo("a new name");
        assertThat(ds.getVersion()).isEqualTo("a new version");
        assertThat(ds.isRequiredMigrationStep()).isTrue();
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a distribution set can be updated.")
    public void updateDistributionSetMetadata() throws InterruptedException {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1);

        // create an DS meta data entry
        createDistributionSetMetadata(ds.getId(), new JpaDistributionSetMetadata(knownKey, ds, knownValue));

        DistributionSet changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2);

        Thread.sleep(100);

        // update the DS metadata
        final JpaDistributionSetMetadata updated = (JpaDistributionSetMetadata) distributionSetManagement
                .updateDistributionSetMetadata(ds.getId(), entityFactory.generateMetadata(knownKey, knownUpdateValue));
        // we are updating the sw meta data so also modifying the base software
        // module so opt lock
        // revision must be three
        changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(3);
        assertThat(changedLockRevisionDS.getLastModifiedAt()).isGreaterThan(0L);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getDistributionSet().getId()).isEqualTo(ds.getId());
    }

    @Test
    @Description("Tests that a DS queue is possible where the result is ordered by the target assignment, i.e. assigned first in the list.")
    public void findDistributionSetsAllOrderedByLinkTarget() {

        final List<DistributionSet> buildDistributionSets = testdataFactory.createDistributionSets("dsOrder", 10);

        final List<Target> buildTargetFixtures = testdataFactory.createTargets(5, "tOrder", "someDesc");

        final Iterator<DistributionSet> dsIterator = buildDistributionSets.iterator();
        final Iterator<Target> tIterator = buildTargetFixtures.iterator();
        final DistributionSet dsFirst = dsIterator.next();
        final DistributionSet dsSecond = dsIterator.next();
        final DistributionSet dsThree = dsIterator.next();
        final DistributionSet dsFour = dsIterator.next();
        final Target tFirst = tIterator.next();
        final Target tSecond = tIterator.next();

        // set assigned
        assignDistributionSet(dsSecond.getId(), tSecond.getControllerId());
        assignDistributionSet(dsThree.getId(), tFirst.getControllerId());
        // set installed
        testdataFactory.sendUpdateActionStatusToTargets(Collections.singleton(tSecond), Status.FINISHED,
                Lists.newArrayList("some message"));

        assignDistributionSet(dsFour.getId(), tSecond.getControllerId());

        final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                .setIsDeleted(false).setIsComplete(true).setSelectDSWithNoTag(Boolean.FALSE);

        // target first only has an assigned DS-three so check order correct
        final List<DistributionSet> tFirstPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tFirst.getControllerId()).getContent();
        assertThat(tFirstPin.get(0)).isEqualTo(dsThree);
        assertThat(tFirstPin).hasSize(10);

        // target second has installed DS-2 and assigned DS-4 so check order
        // correct
        final List<DistributionSet> tSecondPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tSecond.getControllerId()).getContent();
        assertThat(tSecondPin.get(0)).isEqualTo(dsSecond);
        assertThat(tSecondPin.get(1)).isEqualTo(dsFour);
        assertThat(tFirstPin).hasSize(10);
    }

    @Test
    @Description("searches for distribution sets based on the various filter options, e.g. name, version, desc., tags.")
    public void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = tagManagement
                .createDistributionSetTag(entityFactory.tag().create().name("DistributionSetTag-A"));
        final DistributionSetTag dsTagB = tagManagement
                .createDistributionSetTag(entityFactory.tag().create().name("DistributionSetTag-B"));
        final DistributionSetTag dsTagC = tagManagement
                .createDistributionSetTag(entityFactory.tag().create().name("DistributionSetTag-C"));
        tagManagement.createDistributionSetTag(entityFactory.tag().create().name("DistributionSetTag-D"));

        Collection<DistributionSet> ds100Group1 = testdataFactory.createDistributionSets("", 100);
        Collection<DistributionSet> ds100Group2 = testdataFactory.createDistributionSets("test2", 100);
        DistributionSet dsDeleted = testdataFactory.createDistributionSet("deleted");
        final DistributionSet dsInComplete = distributionSetManagement.createDistributionSet(entityFactory
                .distributionSet().create().name("notcomplete").version("1").type(standardDsType.getKey()));

        DistributionSetType newType = distributionSetManagement.createDistributionSetType(
                entityFactory.distributionSetType().create().key("foo").name("bar").description("test"));

        distributionSetManagement.assignMandatorySoftwareModuleTypes(newType.getId(),
                Lists.newArrayList(osType.getId()));
        newType = distributionSetManagement.assignOptionalSoftwareModuleTypes(newType.getId(),
                Lists.newArrayList(appType.getId(), runtimeType.getId()));

        final DistributionSet dsNewType = distributionSetManagement.createDistributionSet(
                entityFactory.distributionSet().create().name("newtype").version("1").type(newType.getKey()).modules(
                        dsDeleted.getModules().stream().map(SoftwareModule::getId).collect(Collectors.toList())));

        assignDistributionSet(dsDeleted, Lists.newArrayList(testdataFactory.createTargets(5)));
        distributionSetManagement.deleteDistributionSet(dsDeleted.getId());
        dsDeleted = distributionSetManagement.findDistributionSetById(dsDeleted.getId());

        ds100Group1 = toggleTagAssignment(ds100Group1, dsTagA).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group1 = toggleTagAssignment(ds100Group1, dsTagB).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group2 = toggleTagAssignment(ds100Group2, dsTagA).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());

        // check setup
        assertThat(distributionSetRepository.findAll()).hasSize(203);

        // Find all
        List<DistributionSet> expected = Lists.newArrayListWithExpectedSize(203);
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsInComplete);
        expected.add(dsNewType);

        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, getDistributionSetFilterBuilder().build()).getContent())
                        .hasSize(203).containsOnly(expected.toArray(new DistributionSet[0]));

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for completed
        expected = new ArrayList<>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(202)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        expected = new ArrayList<>();
        expected.add(dsInComplete);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // search for type
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for text
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        // search for tags
        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete
        expected = new ArrayList<>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(201)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<>();
        expected.add(dsInComplete);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type
        expected = new ArrayList<>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE)
                .setIsComplete(Boolean.TRUE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(200)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        expected = new ArrayList<>();
        expected.add(dsNewType);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // combine deleted and complete and type and text
        expected = new ArrayList<>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE).setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setIsComplete(false).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType).setSearchText("%test2")
                .setIsComplete(Boolean.TRUE).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type and text and tag
        expected = new ArrayList<>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setType(standardDsType)
                .setSearchText("%test2").setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setTagNames(Lists.newArrayList(dsTagA.getName())).setIsComplete(Boolean.FALSE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    @Test
    @Description("Simple DS load without the related data that should be loaded lazy.")
    public void findDistributionSetsWithoutLazy() {
        testdataFactory.createDistributionSets(20);

        assertThat(distributionSetManagement.findDistributionSetsByDeletedAndOrCompleted(pageReq, false, true))
                .hasSize(20);
    }

    @Test
    @Description("Deltes a DS that is no in use. Expected behaviour is a hard delete on the database.")
    public void deleteUnassignedDistributionSet() {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.deleteDistributionSet(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement
                .findDistributionSetsByDeletedAndOrCompleted(pageReq, Boolean.FALSE, Boolean.TRUE).getTotalElements())
                        .isEqualTo(1);
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllDistributionSetMetadataByDsId() {
        // create a DS
        DistributionSet ds1 = testdataFactory.createDistributionSet("testDs1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("testDs2");

        for (int index = 0; index < 10; index++) {

            ds1 = createDistributionSetMetadata(ds1.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds1, "value" + index)).getDistributionSet();
        }

        for (int index = 0; index < 20; index++) {

            ds2 = createDistributionSetMetadata(ds2.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds2, "value" + index)).getDistributionSet();
        }

        final Page<DistributionSetMetadata> metadataOfDs1 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds1.getId(), new PageRequest(0, 100));

        final Page<DistributionSetMetadata> metadataOfDs2 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds2.getId(), new PageRequest(0, 100));

        assertThat(metadataOfDs1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfDs1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfDs2.getNumberOfElements()).isEqualTo(20);
        assertThat(metadataOfDs2.getTotalElements()).isEqualTo(20);
    }

    @Test
    @Description("Deletes a DS that is in use by either target assignment or rollout. Expected behaviour is a soft delete on the database, i.e. only marked as "
            + "deleted, kept as reference but unavailable for future use..")
    public void deleteAssignedDistributionSet() {
        testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");
        final DistributionSet dsToRolloutAssigned = testdataFactory.createDistributionSet("ds-4");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());

        // create assigned rollout
        createRolloutByVariables("test", "test", 5, "name==*", dsToRolloutAssigned, "50", "5");

        // delete assigned ds
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        distributionSetManagement
                .deleteDistributionSet(Lists.newArrayList(dsToTargetAssigned.getId(), dsToRolloutAssigned.getId()));

        // not assigned so not marked as deleted
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        assertThat(distributionSetManagement
                .findDistributionSetsByDeletedAndOrCompleted(pageReq, Boolean.FALSE, Boolean.TRUE).getTotalElements())
                        .isEqualTo(2);
    }

    @Test
    @Description("Verify that the DistributionSetAssignmentResult not contains already assigned targets.")
    public void verifyDistributionSetAssignmentResultNotContainsAlreadyAssignedTargets() {
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        DistributionSetAssignmentResult assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(),
                savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).hasSize(1);

        assignmentResult = assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());
        assertThat(assignmentResult.getAssignedEntity()).hasSize(0);

        assertThat(distributionSetRepository.findAll()).hasSize(1);
    }

}
