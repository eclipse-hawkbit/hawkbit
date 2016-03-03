/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.RandomGeneratedInputStream;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Software Management")
public class SoftwareManagementTest extends AbstractIntegrationTestWithMongoDB {

    @Test
    @Description("searched for software modules based on the various filter options, e.g. name,desc,type, version.")
    public void findSoftwareModuleByFilters() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        final SoftwareModule ah2 = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.2", null, ""));
        DistributionSet ds = distributionSetManagement.createDistributionSet(
                TestDataUtil.buildDistributionSet("ds-1", "1.0.1", standardDsType, os, jvm, ah2));

        final Target target = targetManagement.createTarget(new Target("test123"));
        ds = assignSet(target, ds).getDistributionSet();

        // standard searches
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "poky", osType).getContent()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "poky", osType).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "oracle%", runtimeType).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "oracle%", runtimeType).getContent().get(0))
                .isEqualTo(jvm);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0.1", appType).getContent()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0.1", appType).getContent().get(0))
                .isEqualTo(ah);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType).getContent()).hasSize(2);

        // no we search with on entity marked as deleted
        softwareManagement.deleteSoftwareModule(
                softwareManagement.findSoftwareModuleByAssignedToAndType(pageReq, ds, appType).getContent().get(0));

        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType).getContent()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType).getContent().get(0))
                .isEqualTo(ah);
    }

    private Action assignSet(final Target target, final DistributionSet ds) {
        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { target.getControllerId() });
        assertThat(
                targetManagement.findTargetByControllerID(target.getControllerId()).getTargetInfo().getUpdateStatus())
                        .isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getAssignedDistributionSet())
                .isEqualTo(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(pageReq, target, ds).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    @Test
    @Description("Searches for software modules based on a list of IDs.")
    public void findSoftwareModulesByIdAndType() {

        final List<Long> modules = new ArrayList<Long>();

        modules.add(softwareManagement.createSoftwareModule(new SoftwareModule(osType, "poky-una", "3.0.2", null, ""))
                .getId());
        modules.add(softwareManagement.createSoftwareModule(new SoftwareModule(osType, "poky-u2na", "3.0.3", null, ""))
                .getId());
        modules.add(624355263L);

        assertThat(softwareManagement.findSoftwareModulesById(modules)).hasSize(2);
    }

    @Test
    @Description("Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    public void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);

        SoftwareModuleType type = softwareManagement.createSoftwareModuleType(
                new SoftwareModuleType("bundle", "OSGi Bundle", "fancy stuff", Integer.MAX_VALUE));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        // delete unassigned
        softwareManagement.deleteSoftwareModuleType(type);
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);
        assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains(osType, runtimeType, appType);

        type = softwareManagement.createSoftwareModuleType(
                new SoftwareModuleType("bundle2", "OSGi Bundle2", "fancy stuff", Integer.MAX_VALUE));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        softwareManagement
                .createSoftwareModule(new SoftwareModule(type, "Test SM", "1.0", "cool module", "from meeee"));

        // delete assigned
        softwareManagement.deleteSoftwareModuleType(type);
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains(osType, runtimeType, appType,
                softwareModuleTypeRepository.findOne(type.getId()));
    }

    @Test
    @Description("Deletes an artifact, which is not assigned to a Distribution Set")
    public void hardDeleteOfNotAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with Artifacts
        final SoftwareModule unassignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);
        final Iterator<Artifact> artifactsIt = unassignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();

        // [STEP2]: Delete unassigned SoftwareModule
        softwareManagement.deleteSoftwareModule(unassignedModule);

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModule is deleted
        assertThat(softwareModuleRepository.findAll()).hasSize(0);
        assertThat(softwareManagement.findSoftwareModuleById(unassignedModule.getId())).isNull();

        // verify: binary data of artifact is deleted
        assertArtfiactNull(artifact1, artifact2);

        // verify: meta data of artifact is deleted
        assertThat(artifactRepository.findOne(artifact1.getId())).isNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNull();
    }

    @Test
    @Description("Deletes an artifact, which is assigned to a Distribution Set")
    public void softDeleteOfAssignedArtifact() {

        // Init DistributionSet
        final DistributionSet disSet = distributionSetManagement
                .createDistributionSet(new DistributionSet("ds1", "v1.0", "test ds", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        distributionSetManagement.assignSoftwareModules(disSet, Sets.newHashSet(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule);

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId());
        assertTrue("The module should be flagged as deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtfiactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findOne(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an artifact, which has been assigned to a rolled out DistributionSet in the past")
    public void softDeleteOfHistoricalAssignedArtifact() {

        // Init target and DistributionSet
        final Target target = targetManagement.createTarget(new Target("test123"));
        final DistributionSet disSet = distributionSetManagement
                .createDistributionSet(new DistributionSet("ds1", "v1.0", "test ds", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        distributionSetManagement.assignSoftwareModules(disSet, Sets.newHashSet(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        deploymentManagement.assignDistributionSet(disSet, Lists.newArrayList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.deleteDistributionSet(disSet);

        // [STEP5]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule);

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId());
        assertTrue("The found module should be flagged deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtfiactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findOne(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an softwaremodule with an artifact, which is also used by another softwaremodule.")
    public void deleteSoftwareModulesWithSharedArtifact() throws IOException {

        // Precondition: Make sure MongoDB is Empty
        assertThat(operations.find(new Query())).hasSize(0);

        // Init artifact binary data, target and DistributionSets
        final byte[] source = RandomUtils.nextBytes(1024);

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        // [STEP2]: Create newArtifactX and add it to SoftwareModuleX
        artifactManagement.createLocalArtifact(new ByteArrayInputStream(source), moduleX.getId(), "artifactx", false);
        moduleX = softwareManagement.findSoftwareModuleWithDetails(moduleX.getId());
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.createLocalArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleWithDetails(moduleY.getId());
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // verify: that only one entry was created in mongoDB
        assertThat(operations.find(new Query())).hasSize(1);

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX);

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleById(moduleX.getId())).isNull();
        assertThat(softwareManagement.findSoftwareModuleById(moduleY.getId())).isNotNull();

        // verify: binary data of artifact is not deleted
        assertArtfiactNotNull(artifactY);

        // verify: meta data of artifactX is deleted
        assertThat(artifactRepository.findOne(artifactX.getId())).isNull();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactRepository.findOne(artifactY.getId())).isNotNull();
    }

    @Test
    @Description("Delete two assigned softwaremodules which share an artifact.")
    public void deleteMultipleSoftwareModulesWhichShareAnArtifact() throws IOException {

        // Precondition: Make sure MongoDB is Empty
        assertThat(operations.find(new Query())).hasSize(0);

        // Init artifact binary data, target and DistributionSets
        final byte[] source = RandomUtils.nextBytes(1024);
        final Target target = targetManagement.createTarget(new Target("test123"));
        final DistributionSet disSetX = distributionSetManagement
                .createDistributionSet(new DistributionSet("dsX", "v1.0", "test dsX", standardDsType, null));
        final DistributionSet disSetY = distributionSetManagement
                .createDistributionSet(new DistributionSet("dsY", "v1.0", "test dsY", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(source), moduleX.getId(), "artifactx", false);
        moduleX = softwareManagement.findSoftwareModuleWithDetails(moduleX.getId());
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleWithDetails(moduleY.getId());
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // verify: that only one entry was created in mongoDB
        assertThat(operations.find(new Query())).hasSize(1);

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        distributionSetManagement.assignSoftwareModules(disSetX, Sets.newHashSet(moduleX));
        deploymentManagement.assignDistributionSet(disSetX, Lists.newArrayList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        distributionSetManagement.assignSoftwareModules(disSetY, Sets.newHashSet(moduleY));
        deploymentManagement.assignDistributionSet(disSetY, Lists.newArrayList(target));

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX);

        // [STEP6]: Delete SoftwareModuleY
        softwareManagement.deleteSoftwareModule(moduleY);

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId());
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId());

        // verify: SoftwareModuleX and SofwtareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue("The module should be flagged deleted", moduleX.isDeleted());
        assertTrue("The module should be flagged deleted", moduleY.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtfiactNull(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactRepository.findOne(artifactY.getId())).isNotNull();
    }

    private SoftwareModule createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
            final String version, final int numberArtifacts) {

        final long countSoftwareModule = softwareModuleRepository.count();

        // create SoftwareModule
        SoftwareModule softwareModule = softwareManagement
                .createSoftwareModule(new SoftwareModule(type, name, version, "description of artifact " + name, ""));

        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024), softwareModule.getId(),
                    "file" + (i + 1), false);
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareManagement.findSoftwareModuleWithDetails(softwareModule.getId());
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtfiactNotNull(artifacts.toArray(new Artifact[artifacts.size()]));
        }

        artifacts.forEach(artifact -> {
            assertThat(artifactRepository.findOne(artifact.getId())).isNotNull();
        });
        return softwareModule;
    }

    private void assertArtfiactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(operations.findOne(new Query()
                    .addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                            .isNotNull();
        }
    }

    private void assertArtfiactNull(final Artifact... results) {
        for (final Artifact result : results) {
            assertThat(operations.findOne(new Query()
                    .addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                            .isNull();
        }
    }

    /**
     *
     * @param findAll
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Collection iterable2Collection(final Iterable iterable) {
        final Collection<Object> col = new ArrayList<Object>();
        for (final Object o : iterable) {
            col.add(o);
        }
        return col;
    }

    /**
    *
    */
    private void printDSTags() {
        System.out.println("==============================================================================");
        for (final DistributionSet d : distributionSetRepository.findAll()) {
            System.out.printf("%s\t[", d.getName());
            for (final DistributionSetTag t : d.getTags()) {
                System.out.printf("%s ", t.getName());
            }
            System.out.println("]");
        }
    }

    @Test
    @Description("Checks that metadata for a software module can be created.")
    public void createSoftwareModuleMetadata() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final String knownKey2 = "myKnownKey2";
        final String knownValue2 = "myKnownValue2";

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        assertThat(ah.getOptLockRevision()).isEqualTo(1L);

        final SoftwareModuleMetadata swMetadata1 = new SoftwareModuleMetadata(knownKey1, ah, knownValue1);

        final SoftwareModuleMetadata swMetadata2 = new SoftwareModuleMetadata(knownKey2, ah, knownValue2);

        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement
                .createSoftwareModuleMetadata(Lists.newArrayList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2L);

        assertThat(softwareModuleMetadata).hasSize(2);
        assertThat(softwareModuleMetadata.get(0)).isNotNull();
        assertThat(softwareModuleMetadata.get(0).getValue()).isEqualTo(knownValue1);
        assertThat(softwareModuleMetadata.get(0).getId().getKey()).isEqualTo(knownKey1);
        assertThat(softwareModuleMetadata.get(0).getSoftwareModule().getId()).isEqualTo(ah.getId());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a software module can be updated.")
    public void updateSoftwareModuleMetadata() throws InterruptedException {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a base software module
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1L);

        // create an software module meta data entry
        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement.createSoftwareModuleMetadata(
                Collections.singleton(new SoftwareModuleMetadata(knownKey, ah, knownValue)));
        assertThat(softwareModuleMetadata).hasSize(1);
        // base software module should have now the opt lock revision one
        // because we are modifying the
        // base software module
        SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2L);

        // modifying the meta data value
        softwareModuleMetadata.get(0).setValue(knownUpdateValue);
        softwareModuleMetadata.get(0).setSoftwareModule(softwareManagement.findSoftwareModuleById(ah.getId()));
        softwareModuleMetadata.get(0).setKey(knownKey);

        // update the software module metadata
        Thread.sleep(100);
        final SoftwareModuleMetadata updated = softwareManagement
                .updateSoftwareModuleMetadata(softwareModuleMetadata.get(0));
        // we are updating the sw meta data so also modiying the base software
        // module so opt lock
        // revision must be two
        changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(3L);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getSoftwareModule().getId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllSoftwareModuleMetadataBySwId() {

        SoftwareModule sw1 = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        SoftwareModule sw2 = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "os", "1.0.1", null, ""));

        for (int index = 0; index < 10; index++) {
            sw1 = softwareManagement
                    .createSoftwareModuleMetadata(new SoftwareModuleMetadata("key" + index, sw1, "value" + index))
                    .getSoftwareModule();
        }

        for (int index = 0; index < 20; index++) {
            sw2 = softwareManagement
                    .createSoftwareModuleMetadata(new SoftwareModuleMetadata("key" + index, sw2, "value" + index))
                    .getSoftwareModule();
        }

        final Page<SoftwareModuleMetadata> metadataOfSw1 = softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(sw1.getId(), new PageRequest(0, 100));

        final Page<SoftwareModuleMetadata> metadataOfSw2 = softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(sw2.getId(), new PageRequest(0, 100));

        assertThat(metadataOfSw1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfSw1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfSw2.getNumberOfElements()).isEqualTo(20);
        assertThat(metadataOfSw2.getTotalElements()).isEqualTo(20);
    }
}
