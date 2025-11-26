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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import org.eclipse.hawkbit.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement.Create;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement.Update;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.IncompleteSoftwareModuleException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.RandomGeneratedInputStream;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Software Module Management
 */
class SoftwareModuleManagementTest
        extends AbstractRepositoryManagementWithMetadataTest<SoftwareModule, Create, Update, MetadataValue, MetadataValueCreate> {

    @SuppressWarnings("unchecked")
    @Override
    protected <O> O forType(final Class<O> type) {
        if (type == MetadataValueCreate.class) {
            return (O) new MetadataValueCreate(forType(String.class), forType(Boolean.class));
        }
        return super.forType(type);
    }

    @Override
    protected Object builderParameterValue(final Method builderSetter) {
        // encrypted true is not supported
        if (builderSetter.getDeclaringClass() == Create.CreateBuilder.class && "encrypted".equals(builderSetter.getName())) {
            return Boolean.FALSE;
        }

        return super.builderParameterValue(builderSetter);
    }

    /**
     * Deletes an artifact, which is not assigned to a Distribution Set
     */
    @Test
    void hardDeleteOfNotAssignedArtifact() {
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
        assertThat(softwareModuleManagement.find(unassignedModule.getId())).isNotPresent();

        // verify: binary data of artifact is deleted
        assertArtifactDoesntExist(artifact1, artifact2);

        // verify: metadata of artifact is deleted
        assertThat(artifactRepository.findById(artifact1.getId())).isNotPresent();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotPresent();
    }

    /**
     * Deletes an artifact, which is assigned to a DistributionSet
     */
    @Test
    void softDeleteOfAssignedArtifact() {
        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        testdataFactory.createDistributionSet(Set.of(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.find(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The module should be flagged as deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleManagement.findByRsql("name==*", PAGE)).isEmpty();
        assertThat(softwareModuleManagement.count()).isZero();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactDoesntExist(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    /**
     * Delete an artifact, which has been assigned to a rolled out DistributionSet in the past
     */
    @Test
    void softDeleteOfHistoricalAssignedArtifact() {
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
        assignedModule = softwareModuleManagement.find(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The found module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactDoesntExist(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    /**
     * Delete a software module with an artifact, which is also used by another software module.
     */
    @Test
    void deleteSoftwareModulesWithSharedArtifact() {
        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = randomBytes(artifactSize);

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        // [STEP2]: Create newArtifactX and add it to SoftwareModuleX
        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(source), null, artifactSize, null,
                moduleX.getId(), "artifactx", false));
        moduleX = softwareModuleManagement.find(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(source), null, artifactSize, null,
                moduleY.getId(), "artifactx", false));
        moduleY = softwareModuleManagement.find(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareModuleManagement.find(moduleX.getId())).isNotPresent();
        assertThat(softwareModuleManagement.find(moduleY.getId())).isPresent();

        // verify: binary data of artifact is not deleted
        assertArtifactNotNull(artifactY);

        // verify: meta data of artifactX is deleted
        assertThat(artifactRepository.findById(artifactX.getId())).isNotPresent();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isPresent();
    }

    /**
     * Delete two assigned software modules which share an artifact.
     */
    @Test
    void deleteMultipleSoftwareModulesWhichShareAnArtifact() {
        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = randomBytes(artifactSize);
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(source), null, artifactSize, null,
                moduleX.getId(), "artifactx", false));
        moduleX = softwareModuleManagement.find(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(source), null, artifactSize, null,
                moduleY.getId(), "artifactx", false));
        moduleY = softwareModuleManagement.find(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        final DistributionSet disSetX = assignDistributionSet(testdataFactory.createDistributionSet(Set.of(moduleX), "X"), List.of(target))
                .getDistributionSet();

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        final DistributionSet disSetY = assignDistributionSet(testdataFactory.createDistributionSet(Set.of(moduleY), "Y"), List.of(target))
                .getDistributionSet();

        // [STEP5]: Delete SoftwareModuleX
        distributionSetManagement.unlock(disSetX); // otherwise delete will be rejected as a part of a locked DS
        distributionSetManagement.unlock(disSetY); // otherwise delete will be rejected as a part of a locked DS
        softwareModuleManagement.delete(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareModuleManagement.delete(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareModuleManagement.find(moduleX.getId()).get();
        moduleY = softwareModuleManagement.find(moduleY.getId()).get();

        // verify: SoftwareModuleX and SoftwareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue(moduleX.isDeleted(), "The module should be flagged deleted");
        assertTrue(moduleY.isDeleted(), "The module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtifactDoesntExist(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isNotNull();
    }

    /**
     * Verifies that all soft deleted software modules are found in the repository.
     */
    @Test
    void countSoftwareModules() {
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
    @Test
    void findSoftwareModuleByAssignedTo() {
        // test modules
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createSoftwareModuleOs("notassigned");

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        final DistributionSet set = distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(defaultDsType())
                .name("set").version("1").modules(Set.of(one, deleted)).build());
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.findByAssignedTo(set.getId(), PAGE).getContent())
                .as("Found this number of modules").hasSize(2);
    }

    @Test
    void lockSoftwareModule() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        assertThat(softwareModuleManagement.find(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true)).isFalse();
        softwareModuleManagement.lock(softwareModule);
        assertThat(softwareModuleManagement.find(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false)).isTrue();
    }

    @Test
    void unlockSoftwareModule() {
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        softwareModule = softwareModuleManagement.lock(softwareModule);
        assertThat(softwareModuleManagement.find(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false)).isTrue();
        softwareModule = softwareModuleManagement.unlock(softwareModule);
        assertThat(softwareModuleManagement.find(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true)).isFalse();
    }

    /**
     * Artifacts of a locked SM can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test
    void lockSoftwareModuleApplied() {
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        final Long softwareModuleId = softwareModule.getId();
        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(new byte[] { 1 }), null, 1, null, softwareModuleId, "artifact1", false));
        // update software module reference since it is modified, old reference is stale
        final int artifactCount = (softwareModule = softwareModuleManagement.find(softwareModuleId).orElseThrow()).getArtifacts().size();
        assertThat(artifactCount).isNotZero();
        softwareModuleManagement.lock(softwareModule);
        assertThat(softwareModuleManagement.find(softwareModuleId).map(SoftwareModule::isLocked).orElse(false)).isTrue();

        // try add
        final ArtifactUpload artifactUpload = new ArtifactUpload(
                new ByteArrayInputStream(new byte[] { 2 }), null, 1, null, softwareModuleId, "artifact2", false);
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.create(artifactUpload));
        assertThat(softwareModuleManagement.find(softwareModuleId).get().getArtifacts())
                .as("Artifacts shall not be added to a locked SM.")
                .hasSize(artifactCount);

        // try remove
        final long artifactId = softwareModuleManagement.find(softwareModuleId).get().getArtifacts().stream().findFirst().get().getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.delete(artifactId));
        assertThat(softwareModuleManagement.find(softwareModuleId).get().getArtifacts())
                .as("Artifact shall not be removed from a locked SM.")
                .hasSize(artifactCount);
    }

    /**
     * Artifacts of a locked SM can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test
    void lockedContainingDistributionSetApplied() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        final List<SoftwareModule> modules = distributionSet.getModules().stream().toList();
        assertThat(modules).hasSizeGreaterThan(1);

        // try to delete while DS is not locked
        softwareModuleManagement.delete(modules.get(0).getId());

        distributionSetManagement.lock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false)).isTrue();

        // try to delete SM of a locked DS
        final Long moduleId = modules.get(1).getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to delete a software module of a locked DS should throw an exception")
                .isThrownBy(() -> softwareModuleManagement.delete(moduleId));
    }

    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void failIfReferNotExistingEntity() {
        testdataFactory.createSoftwareModuleApp();

        final Create noType = Create.builder().name("xxx").type(null).build();
        final List<Create> noTypeList = List.of(noType);
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> softwareModuleManagement.create(noTypeList));
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> softwareModuleManagement.create(noType));

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetadata(NOT_EXIST_IDL, "xxx", new MetadataValueCreate("xxx")), "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetadata(NOT_EXIST_IDL, Map.of("xxx", new MetadataValueCreate("xxx"))), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(Collections.singletonList(NOT_EXIST_IDL)), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetadata(NOT_EXIST_IDL, "xxx"), "SoftwareModule");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetadata(NOT_EXIST_IDL, "xxx", new MetadataValueCreate("xxx")), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findByAssignedTo(NOT_EXIST_IDL, PAGE), "DistributionSet");
        verifyThrownExceptionBy(() -> softwareModuleManagement.getMetadata(NOT_EXIST_IDL, NOT_EXIST_ID), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.getMetadata(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "SoftwareModule");
    }

    /**
     * Verifies that when no SoftwareModules are assigned to a Distribution then the DistributionSet is not complete.
     */
    @Test
    void incompleteIfSoftwareModule() {
        final SoftwareModuleType softwareModuleType = softwareModuleTypeManagement
                .create(SoftwareModuleTypeManagement.Create.builder().key("newType").name("new Type").minArtifacts(1).build());
        final SoftwareModule softwareModuleIncomplete = softwareModuleManagement
                .create(SoftwareModuleManagement.Create.builder().name("ds").version("1.0.0").type(softwareModuleType).build());
        assertThat(softwareModuleIncomplete.isComplete()).isFalse();
        assertThatExceptionOfType(IncompleteSoftwareModuleException.class)
                .isThrownBy(() -> softwareModuleManagement.lock(softwareModuleIncomplete));

        // add artifact - so it should become complete
        artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(randomBytes(10)), null, 10, null,
                softwareModuleIncomplete.getId(), "file1", false));

        final SoftwareModule softwareModuleComplete = softwareModuleManagement.get(softwareModuleIncomplete.getId());
        assertThat(softwareModuleComplete.isComplete()).isTrue();
        assertThatNoException().isThrownBy(() -> softwareModuleManagement.lock(softwareModuleComplete));
        assertThat(softwareModuleManagement.get(softwareModuleIncomplete.getId()).isComplete()).isTrue();
    }

    private SoftwareModule createSoftwareModuleWithArtifacts(
            final SoftwareModuleType type, final String name, final String version, final int numberArtifacts) {
        final long countSoftwareModule = softwareModuleRepository.count();

        // create SoftwareModule
        SoftwareModule softwareModule = softwareModuleManagement.create(Create.builder()
                .type(type).name(name).version(version).description("description of artifact " + name).build());

        final int artifactSize = 5 * 1024;
        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.create(new ArtifactUpload(
                    new RandomGeneratedInputStream(artifactSize), null, artifactSize, null,
                    softwareModule.getId(), "file" + (i + 1), false));
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareModuleManagement.find(softwareModule.getId()).get();
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtifactNotNull(artifacts.toArray(new Artifact[0]));
        }

        artifacts.forEach(artifact -> assertThat(artifactRepository.findById(artifact.getId())).isNotNull());
        return softwareModule;
    }

    private void assertArtifactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(artifactStorage.getBySha1(AccessContext.tenant(), result.getSha1Hash())).isNotNull();
        }
    }

    private void assertArtifactDoesntExist(final Artifact... results) {
        for (final Artifact result : results) {
            final String currentTenant = AccessContext.tenant();
            final String sha1Hash = result.getSha1Hash();
            assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                    .isThrownBy(() -> artifactStorage.getBySha1(currentTenant, sha1Hash));
        }
    }
}