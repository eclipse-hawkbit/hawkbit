/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.RandomGeneratedInputStream;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Software Module Management
 */
class SoftwareModuleManagementTest extends AbstractJpaIntegrationTest {

    private static final PageRequest PAGE_REQUEST_100 = PageRequest.of(0, 100);

    /**
     * Verifies that management get access reacts as specified on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.get(1234L)).isNotPresent();

        assertThat(softwareModuleManagement.findByNameAndVersionAndType(NOT_EXIST_ID, NOT_EXIST_ID, osType.getId()))
                .isNotPresent();

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> softwareModuleManagement.getMetadata(module.getId(), NOT_EXIST_ID));
    }

    /**
     * Verifies that management queries react as specfied on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.create(Collections
                        .singletonList(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID))), "SoftwareModuleType");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement
                        .create(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID)), "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetadata(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key("xxx").value("xxx")), "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetadata(Collections.singletonList(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key("xxx").value("xxx"))), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(Collections.singletonList(NOT_EXIST_IDL)), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetadata(NOT_EXIST_IDL, "xxx"), "SoftwareModule");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetadata(
                        entityFactory.softwareModuleMetadata().update(NOT_EXIST_IDL, "xxx").value("xxx")), "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetadata(
                        entityFactory.softwareModuleMetadata().update(module.getId(), NOT_EXIST_ID).value("xxx")), "SoftwareModuleMetadata");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findByAssignedTo(NOT_EXIST_IDL, PAGE), "DistributionSet");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findByNameAndVersionAndType("xxx", "xxx", NOT_EXIST_IDL),
                "SoftwareModuleType");

        verifyThrownExceptionBy(() -> softwareModuleManagement.getMetadata(NOT_EXIST_IDL, NOT_EXIST_ID), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.getMetadata(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.findByType(NOT_EXIST_IDL, PAGE), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.update(entityFactory.softwareModule().update(NOT_EXIST_IDL)), "SoftwareModule");
    }

    /**
     * Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.
     */
    @Test    void updateNothingResultsInUnchangedRepository() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    /**
     * Calling update for changed fields results in change in the repository.
     */
    @Test    void updateSoftwareModuleFieldsToNewValue() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()).description("changed").vendor("changed"));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getVendor()).as("Updated vendor is").isEqualTo("changed");
    }

    /**
     * Create Software Module call fails when called for existing entity.
     */
    @Test    void createModuleCallFailsForExistingModule() {
        testdataFactory.createSoftwareModuleOs();
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Should not have worked as module already exists.")
                .isThrownBy(() -> testdataFactory.createSoftwareModuleOs());
    }

    /**
     * searched for software modules based on the various filter options, e.g. name,desc,type, version.
     */
    @Test    void findSoftwareModuleByFilters() {
        final SoftwareModule ah = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.1"));
        final SoftwareModule jvm = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(runtimeType).name("oracle-jre").version("1.7.2"));
        final SoftwareModule os = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(osType).name("poky").version("3.0.2"));

        final SoftwareModule ah2 = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.2"));
        JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                .create(entityFactory.distributionSet().create().name("ds-1").version("1.0.1").type(standardDsType)
                        .modules(Arrays.asList(os.getId(), jvm.getId(), ah2.getId())));

        final JpaTarget target = (JpaTarget) testdataFactory.createTarget();
        ds = (JpaDistributionSet) assignSet(target, ds).getDistributionSet();
        implicitLock(os);
        implicitLock(jvm);

        // standard searches
        assertThat(softwareModuleManagement.findByTextAndType("poky", osType.getId(), PAGE).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType("poky", osType.getId(), PAGE).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareModuleManagement.findByTextAndType("oracle", runtimeType.getId(), PAGE).getContent())
                .hasSize(1);
        assertThat(
                softwareModuleManagement.findByTextAndType("oracle", runtimeType.getId(), PAGE).getContent().get(0))
                .isEqualTo(jvm);
        assertThat(softwareModuleManagement.findByTextAndType(":1.0.1", appType.getId(), PAGE).getContent()).hasSize(1)
                .first().isEqualTo(ah);
        assertThat(softwareModuleManagement.findByTextAndType(":1.0", appType.getId(), PAGE).getContent()).hasSize(2);

        distributionSetManagement.unlock(ds.getId()); // otherwise delete will be rejected as a part of a locked DS
        softwareModuleManagement.delete(ah2.getId());

        assertThat(softwareModuleManagement.findByTextAndType(":1.0", appType.getId(), PAGE).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(":1.0", appType.getId(), PAGE).getContent().get(0))
                .isEqualTo(ah);
    }

    /**
     * Searches for software modules based on a list of IDs.
     */
    @Test    void findSoftwareModulesById() {

        final List<Long> modules = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId(), 624355263L);

        assertThat(softwareModuleManagement.get(modules)).hasSize(2);
    }

    /**
     * Searches for software modules by type.
     */
    @Test    void findSoftwareModulesByType() {
        // found in test
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs("one");
        final SoftwareModule two = testdataFactory.createSoftwareModuleOs("two");
        // ignored
        softwareModuleManagement.delete(testdataFactory.createSoftwareModuleOs("deleted").getId());
        testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.findByType(osType.getId(), PAGE).getContent())
                .as("Expected to find the following number of modules:").hasSize(2).as("with the following elements")
                .contains(two, one);
    }

    /**
     * Counts all software modules in the repsitory that are not marked as deleted.
     */
    @Test    void countSoftwareModulesAll() {
        // found in test
        testdataFactory.createSoftwareModuleOs("one");
        testdataFactory.createSoftwareModuleOs("two");
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleOs("deleted");
        // ignored
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Expected to find the following number of modules:")
                .isEqualTo(2);
    }

    /**
     * Deletes an artifact, which is not assigned to a Distribution Set
     */
    @Test    void hardDeleteOfNotAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with Artifacts
        final SoftwareModule unassignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);
        final Iterator<Artifact> artifactsIt = unassignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();

        // [STEP2]: Delete unassigned SoftwareModule
        softwareModuleManagement.delete(unassignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModule is deleted
        assertThat(softwareModuleRepository.findAll()).isEmpty();
        assertThat(softwareModuleManagement.get(unassignedModule.getId())).isNotPresent();

        // verify: binary data of artifact is deleted
        assertArtifactNull(artifact1, artifact2);

        // verify: metadata of artifact is deleted
        assertThat(artifactRepository.findById(artifact1.getId())).isNotPresent();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotPresent();
    }

    /**
     * Deletes an artifact, which is assigned to a DistributionSet
     */
    @Test    void softDeleteOfAssignedArtifact() {
        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        testdataFactory.createDistributionSet(Set.of(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The module should be flagged as deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleManagement.findByRsql("name==*", PAGE)).isEmpty();
        assertThat(softwareModuleManagement.count()).isZero();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    /**
     * Delete an artifact, which has been assigned to a rolled out DistributionSet in the past
     */
    @Test    void softDeleteOfHistoricalAssignedArtifact() {

        // Init target
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        final DistributionSet disSet = testdataFactory.createDistributionSet(Set.of(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        assignDistributionSet(disSet, Collections.singletonList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.delete(disSet.getId());

        // [STEP5]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The found module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    /**
     * Delete an software module with an artifact, which is also used by another software module.
     */
    @Test    void deleteSoftwareModulesWithSharedArtifact() {

        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = randomBytes(artifactSize);

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        // [STEP2]: Create newArtifactX and add it to SoftwareModuleX
        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleX.getId(), "artifactx",
                false, artifactSize));
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleY.getId(), "artifactx",
                false, artifactSize));
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareModuleManagement.get(moduleX.getId())).isNotPresent();
        assertThat(softwareModuleManagement.get(moduleY.getId())).isPresent();

        // verify: binary data of artifact is not deleted
        assertArtifactNotNull(artifactY);

        // verify: meta data of artifactX is deleted
        assertThat(artifactRepository.findById(artifactX.getId())).isNotPresent();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isPresent();

    }

    /**
     * Delete two assigned softwaremodules which share an artifact.
     */
    @Test    void deleteMultipleSoftwareModulesWhichShareAnArtifact() {
        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = randomBytes(artifactSize);
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleX.getId(), "artifactx",
                false, artifactSize));
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleY.getId(), "artifactx",
                false, artifactSize));
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        final DistributionSet disSetX = testdataFactory.createDistributionSet(Set.of(moduleX), "X");
        assignDistributionSet(disSetX, Collections.singletonList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        final DistributionSet disSetY = testdataFactory.createDistributionSet(Set.of(moduleY), "Y");
        assignDistributionSet(disSetY, Collections.singletonList(target));

        // [STEP5]: Delete SoftwareModuleX
        distributionSetManagement.unlock(disSetX.getId()); // otherwise delete will be rejected as a part of a locked DS
        distributionSetManagement.unlock(disSetY.getId()); // otherwise delete will be rejected as a part of a locked DS
        softwareModuleManagement.delete(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareModuleManagement.delete(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();

        // verify: SoftwareModuleX and SoftwareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue(moduleX.isDeleted(), "The module should be flagged deleted");
        assertTrue(moduleY.isDeleted(), "The module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtifactNull(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isNotNull();
    }

    /**
     * Verifies that all undeleted software modules are found in the repository.
     */
    @Test    void countSoftwareModuleTypesAll() {
        testdataFactory.createSoftwareModuleOs();

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        testdataFactory.createDistributionSet(Collections.singletonList(deleted));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Number of undeleted modules").isEqualTo(1);
        assertThat(softwareModuleRepository.count()).as("Number of all modules").isEqualTo(2);
    }

    /**
     * Verifies that software modules are returned that are assigned to given DS.
     */
    @Test    void findSoftwareModuleByAssignedTo() {
        // test modules
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createSoftwareModuleOs("notassigned");

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        final DistributionSet set = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("set").version("1").modules(Arrays.asList(one.getId(), deleted.getId())));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.findByAssignedTo(set.getId(), PAGE).getContent())
                .as("Found this number of modules").hasSize(2);
    }

    /**
     * Checks that metadata for a software module can be created.
     */
    @Test    void createSoftwareModuleMetadata() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final String knownKey2 = "myKnownKey2";
        final String knownValue2 = "myKnownValue2";

        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModule.getOptLockRevision()).isEqualTo(1);

        final SoftwareModuleMetadataCreate swMetadata1 = entityFactory.softwareModuleMetadata()
                .create(softwareModule.getId())
                .key(knownKey1)
                .value(knownValue1)
                .targetVisible(true);

        final SoftwareModuleMetadataCreate swMetadata2 = entityFactory.softwareModuleMetadata()
                .create(softwareModule.getId())
                .key(knownKey2)
                .value(knownValue2);

        softwareModuleManagement.createMetadata(Arrays.asList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(softwareModule.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        assertThat(softwareModuleManagement.getMetadata(softwareModule.getId(), knownKey1)).satisfies(metadata -> {
            assertThat(metadata.getKey()).isEqualTo(knownKey1);
            assertThat(metadata.getValue()).isEqualTo(knownValue1);
            assertThat(metadata.isTargetVisible()).isTrue();
        });

        assertThat(softwareModuleManagement.getMetadata(softwareModule.getId(), knownKey2)).satisfies(metadata -> {
            assertThat(metadata.getKey()).isEqualTo(knownKey2);
            assertThat(metadata.getValue()).isEqualTo(knownValue2);
            assertThat(metadata.isTargetVisible()).isFalse();
        });
    }

    /**
     * Verifies the enforcement of the metadata quota per software module.
     */
    @Test    void createSoftwareModuleMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp("m1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        for (int i = 0; i < maxMetaData; ++i) {
            softwareModuleManagement.updateMetadata(
                    entityFactory.softwareModuleMetadata().create(module.getId()).key("k" + i).value("v" + i));
        }

        // quota exceeded
        final SoftwareModuleMetadataCreate metadata = entityFactory.softwareModuleMetadata().create(module.getId())
                .key("k" + maxMetaData).value("v" + maxMetaData);
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.updateMetadata(metadata));

        // add multiple meta data entries at once
        final SoftwareModule module2 = testdataFactory.createSoftwareModuleApp("m2");
        final List<SoftwareModuleMetadataCreate> create = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            create.add(entityFactory.softwareModuleMetadata().create(module2.getId()).key("k" + i).value("v" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetadata(create));

        // add some meta data entries
        final SoftwareModule module3 = testdataFactory.createSoftwareModuleApp("m3");
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            softwareModuleManagement.updateMetadata(
                    entityFactory.softwareModuleMetadata().create(module3.getId()).key("k" + i).value("v" + i));
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<SoftwareModuleMetadataCreate> create2 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            create2.add(entityFactory.softwareModuleMetadata().create(module3.getId()).key("kk" + i).value("vv" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetadata(create2));

    }

    /**
     * Checks that metadata for a software module cannot be created for an existing key.
     */
    @Test    void createSoftwareModuleMetadataFailsIfKeyExists() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        final SoftwareModuleMetadataCreate metadata = entityFactory.softwareModuleMetadata()
                .create(ah.getId()).key(knownKey1).value(knownValue1).targetVisible(true);
        softwareModuleManagement.updateMetadata(metadata);

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.updateMetadata(metadata))
                .withMessageContaining("Metadata").withMessageContaining(knownKey1);

        final String knownKey2 = "myKnownKey2";

        softwareModuleManagement.updateMetadata(entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey2)
                .value(knownValue1).targetVisible(false));

        final SoftwareModuleMetadataCreate metadata2 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey2).value(knownValue1).targetVisible(true);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.updateMetadata(metadata2))
                .withMessageContaining("Metadata").withMessageContaining(knownKey2);
    }

    /**
     * Checks that metadata for a software module can be updated.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void updateSoftwareModuleMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a base software module
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        // create an software module meta data entry
        final SoftwareModuleMetadata softwareModuleMetadata = softwareModuleManagement.updateMetadata(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey).value(knownValue));
        assertThat(softwareModuleMetadata.isTargetVisible()).isFalse();
        assertThat(softwareModuleMetadata.getValue()).isEqualTo(knownValue);

        // base software module should have now the opt lock revision one
        // because we are modifying the base software module
        SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        // update the software module metadata
        final SoftwareModuleMetadata updated = softwareModuleManagement.updateMetadata(entityFactory
                .softwareModuleMetadata().update(ah.getId(), knownKey).value(knownUpdateValue).targetVisible(true));

        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be two
        changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(3);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.isTargetVisible()).isTrue();
        assertThat(((JpaSoftwareModuleMetadata) updated).getId().getKey()).isEqualTo(knownKey);
        assertThat(((JpaSoftwareModuleMetadata) updated).getSoftwareModule().getId()).isEqualTo(ah.getId());
    }

    /**
     * Verifies that existing metadata can be deleted.
     */
    @Test    void deleteSoftwareModuleMetadata() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final SoftwareModule swModule = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.updateMetadata(
                entityFactory.softwareModuleMetadata().create(swModule.getId()).key(knownKey1).value(knownValue1));

        assertThat(softwareModuleManagement.getMetadata(swModule.getId()))
                .as("Contains the created metadata element").allSatisfy(metadata -> {
                    assertThat(((JpaSoftwareModuleMetadata) metadata).getSoftwareModule().getId()).isEqualTo(swModule.getId());
                    assertThat(metadata.getKey()).isEqualTo(knownKey1);
                    assertThat(metadata.getValue()).isEqualTo(knownValue1);
                });

        softwareModuleManagement.deleteMetadata(swModule.getId(), knownKey1);
        assertThat(softwareModuleManagement.getMetadata(swModule.getId())).as("Metadata elements are").isEmpty();
    }

    /**
     * Verifies that non existing metadata find results in exception.
     */
    @Test    void findSoftwareModuleMetadataFailsIfEntryDoesNotExist() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.updateMetadata(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1).value(knownValue1));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> softwareModuleManagement.getMetadata(ah.getId(), "doesnotexist"));
    }

    /**
     * Queries and loads the metadata related to a given software module.
     */
    @Test    void findAllSoftwareModuleMetadataBySwId() {
        final SoftwareModule sw1 = testdataFactory.createSoftwareModuleApp();
        final int metadataCountSw1 = 8;

        final SoftwareModule sw2 = testdataFactory.createSoftwareModuleOs();
        final int metadataCountSw2 = 10;

        for (int index = 0; index < metadataCountSw1; index++) {
            softwareModuleManagement.updateMetadata(entityFactory.softwareModuleMetadata().create(sw1.getId())
                    .key("key" + index).value("value" + index).targetVisible(true));
        }

        for (int index = 0; index < metadataCountSw2; index++) {
            softwareModuleManagement.updateMetadata(entityFactory.softwareModuleMetadata().create(sw2.getId())
                    .key("key" + index).value("value" + index).targetVisible(false));
        }

        final List<SoftwareModuleMetadata> metadataSw1 = softwareModuleManagement.getMetadata(sw1.getId());
        final List<SoftwareModuleMetadata> metadataSw2 = softwareModuleManagement.getMetadata(sw2.getId());
        assertThat(metadataSw1).hasSize(metadataCountSw1);
        assertThat(metadataSw2).hasSize(metadataCountSw2);

        final Page<SoftwareModuleMetadata> metadataSw1V = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(
                sw1.getId(), PAGE_REQUEST_100);
        final Page<SoftwareModuleMetadata> metadataSw2V = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(
                sw2.getId(), PAGE_REQUEST_100);
        assertThat(metadataSw1V.getNumberOfElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw1V.getTotalElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw2V.getNumberOfElements()).isZero();
        assertThat(metadataSw2V.getTotalElements()).isZero();
    }

    /**
     * Locks a SM.
     */
    @Test    void lockSoftwareModule() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true))
                .isFalse();
        softwareModuleManagement.lock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();
    }

    /**
     * Unlocks a SM.
     */
    @Test    void unlockSoftwareModule() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        softwareModuleManagement.lock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();
        softwareModuleManagement.unlock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true))
                .isFalse();
    }

    /**
     * Artifacts of a locked SM can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test    void lockSoftwareModuleApplied() {
        final Long softwareModuleId = testdataFactory.createSoftwareModule("sm-1").getId();
        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(new byte[] { 1 }), softwareModuleId, "artifact1", false, 1));
        final int artifactCount = softwareModuleManagement.get(softwareModuleId).get().getArtifacts().size();
        assertThat(artifactCount).isNotZero();
        softwareModuleManagement.lock(softwareModuleId);
        assertThat(softwareModuleManagement.get(softwareModuleId).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();

        // try add
        final ArtifactUpload artifactUpload = new ArtifactUpload(new ByteArrayInputStream(new byte[] { 2 }), softwareModuleId, "artifact2",
                false, 1);
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.create(artifactUpload));
        assertThat(softwareModuleManagement.get(softwareModuleId).get().getArtifacts())
                .as("Artifacts shall not be added to a locked SM.")
                .hasSize(artifactCount);

        // try remove
        final long artifactId = softwareModuleManagement.get(softwareModuleId).get().getArtifacts().stream().findFirst().get().getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.delete(artifactId));
        assertThat(softwareModuleManagement.get(softwareModuleId).get().getArtifacts())
                .as("Artifact shall not be removed from a locked SM.")
                .hasSize(artifactCount);
        assertThat(artifactManagement.get(artifactId))
                .as("Artifact shall not be removed if belongs to a locked SM.")
                .isPresent();
    }

    /**
     * Artifacts of a locked SM can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test    void lockedContainingDistributionSetApplied() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        final List<SoftwareModule> modules = distributionSet.getModules().stream().toList();
        assertThat(modules).hasSizeGreaterThan(1);

        // try delete while DS is not locked
        softwareModuleManagement.delete(modules.get(0).getId());

        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false))
                .isTrue();

        // try delete SM of a locked DS
        final Long moduleId = modules.get(1).getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to delete a software module of a locked DS should throw an exception")
                .isThrownBy(() -> softwareModuleManagement.delete(moduleId));
    }

    private Action assignSet(final JpaTarget target, final JpaDistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        implicitLock(ds);
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);
        final Optional<DistributionSet> assignedDistributionSet = deploymentManagement
                .getAssignedDistributionSet(target.getControllerId());
        assertThat(assignedDistributionSet).contains(ds);
        final Action action = actionRepository
                .findAll(
                        (root, query, cb) ->
                                cb.and(
                                        cb.equal(root.get(JpaAction_.target).get(JpaTarget_.id), target.getId()),
                                        cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.id), ds.getId())),
                        PAGE).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    private SoftwareModule createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
            final String version, final int numberArtifacts) {

        final long countSoftwareModule = softwareModuleRepository.count();

        // create SoftwareModule
        SoftwareModule softwareModule = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name).version(version).description("description of artifact " + name));

        final int artifactSize = 5 * 1024;
        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.create(new ArtifactUpload(new RandomGeneratedInputStream(artifactSize),
                    softwareModule.getId(), "file" + (i + 1), false, artifactSize));
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareModuleManagement.get(softwareModule.getId()).get();
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtifactNotNull(artifacts.toArray(new Artifact[artifacts.size()]));
        }

        artifacts.forEach(artifact -> assertThat(artifactRepository.findById(artifact.getId())).isNotNull());
        return softwareModule;
    }

    private void assertArtifactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), result.getSha1Hash()))
                    .isNotNull();
        }
    }

    private void assertArtifactNull(final Artifact... results) {
        for (final Artifact result : results) {
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), result.getSha1Hash()))
                    .isNull();
        }
    }
}
