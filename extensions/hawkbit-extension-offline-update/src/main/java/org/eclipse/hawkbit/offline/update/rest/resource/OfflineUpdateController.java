/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.rest.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.offline.update.OfflineUpdateControllerRestApi;
import org.eclipse.hawkbit.offline.update.exception.ArtifactUploadException;
import org.eclipse.hawkbit.offline.update.exception.DistributionSetTypeNotFoundException;
import org.eclipse.hawkbit.offline.update.model.OfflineUpdateData;
import org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo;
import org.eclipse.hawkbit.offline.update.repository.OfflineUpdateDeploymentManagement;
import org.eclipse.hawkbit.offline.update.util.OfflineUpdateMapperUtil;
import org.eclipse.hawkbit.offline.update.util.PropertiesAutoConfiguration;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.MultiPartFileUploadException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link OfflineUpdateController} implements the REST controller for API
 * that allows recieving and persisting necessary information regarding offline
 * software updates performed for a set of targets.
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OfflineUpdateController implements OfflineUpdateControllerRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(OfflineUpdateController.class);
    private static final int DEFAULT_OFFSET_PAGE_LIMIT_PARAM = 1000;
    private static final int DEFAULT_OFFSET_PAGE_PARAM = 0;
    private static final long DEFAULT_FORCED_TIME = 0;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private DistributionSetTypeManagement distributionSetTypeManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    private PropertiesAutoConfiguration propertiesAutoConfiguration;

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    @Qualifier("OfflineUpdateDeploymentManagementImpl")
    private OfflineUpdateDeploymentManagement offlineUpdateDeploymentManagement;

    /**
     * This end point records the offline software update data for a set of
     * {@link Target}s. The method creates a set of {@link SoftwareModule}s and
     * a {@link DistributionSet} based on the provided offline update data. It
     * then assigns the created {@link DistributionSet} to the {@link Target}s,
     * and finishes the corresponding {@link Action}s.
     *
     * @param offlineUpdateInfo
     *            is the offline update data to be recorded for a set of
     *            targets. The data is expected in JSON format represented by
     *            JSON model defined in {@link OfflineUpdateData}
     * @param files
     *            are the set of files to be uploaded as artifacts. Even if no
     *            files are provided, a distribution set will still be created
     *            with a software module containing no artifact
     *
     * @return new {@link MgmtDistributionSet} created.
     */
    @Override
    public ResponseEntity<MgmtDistributionSet> recordOfflineUpdate(
            @Valid @RequestParam("offlineUpdateInfo") final String offlineUpdateInfo,
            @RequestParam("file") final MultipartFile[] files) {
        OfflineUpdateData offlineUpdateData = null;
        try {
            offlineUpdateData = mapper.readValue(offlineUpdateInfo, OfflineUpdateData.class);
        } catch (IOException e) {
            LOG.error("Error parsing JSON.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Retrieve the list of controller Ids from the input.
        List<String> controllerIds = offlineUpdateData.getControllerIds();
        LOG.info("Updating offline data for controllers : {}.", controllerIds.toString());

        // Check if the targets exist else an EntityNotFoundException is thrown.
        controllerIds.stream().forEach(controllerId -> findTargetFromControllerId(controllerId));
        LOG.info("Creating distribution set for {} modules.", offlineUpdateData.getSoftwareModules().size());

        // Create a list of software modules based on offline update data and
        // files to be uploaded.
        List<SoftwareModule> softwareModules = createSoftwareModules(offlineUpdateData.getSoftwareModules(),
                Arrays.asList(files));

        // Create a distribution set using the created software modules.
        final DistributionSet createdDistributionSet = createDistributionSet(softwareModules,
                offlineUpdateData.isMigrationStepRequired());
        LOG.info("Distribution set {} created.", createdDistributionSet.toString());

        // For each controller, create a target action with action type as
        // forced.
        Collection<TargetWithActionType> targetWithActionTypeList = controllerIds.stream()
                .map(targetId -> new TargetWithActionType(targetId, ActionType.FORCED, DEFAULT_FORCED_TIME))
                .collect(Collectors.toList());

        // Assign the distribution set to each controller and mark the Action as
        // complete.
        assignDistributionSetAndFinishAction(createdDistributionSet.getId(), targetWithActionTypeList);
        LOG.info("Assigned distribution set and completed the action.");

        return new ResponseEntity<>(OfflineUpdateMapperUtil.toResponse(createdDistributionSet), HttpStatus.OK);
    }

    /**
     * Find target with given controllerId, else throw
     * {@link EntityNotFoundException}.
     *
     * @param controllerId
     *            is the id of the controller to find the target
     *
     * @return {@link Target} found for the controller.
     */
    private Target findTargetFromControllerId(final String controllerId) {
        return targetManagement.findTargetByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    /**
     * Creates new {@link SoftwareModule}s based on {@link SoftwareModuleInfo}s.
     *
     * @param softwareModuleInfos
     *            is a list of {@link SoftwareModuleInfo}s
     * @param files
     *            is a list of {@link MultipartFile}s to be uploaded as part of
     *            the software modules. The files are stored as
     *            {@link Artifact}s for the software module they belong to.
     *
     * @return a list of {@link SoftwareModule}s created.
     */
    private List<SoftwareModule> createSoftwareModules(final List<SoftwareModuleInfo> softwareModuleInfos,
            final List<MultipartFile> files) {
        LOG.debug("Creating {} software modules.", softwareModuleInfos.size());

        if (!files.isEmpty() && isValidInputFileList(softwareModuleInfos, files)) {
            LOG.error("Invalid input files.");
            throw new MultiPartFileUploadException(new Exception("Invalid files."));
        }

        final List<SoftwareModule> softwareModules = new ArrayList<>();
        for (SoftwareModuleInfo smInfo : softwareModuleInfos) {
            SoftwareModule sm = softwareModuleManagement.createSoftwareModule(entityFactory.softwareModule().create()
                    .type(smInfo.getType()).name(smInfo.getName()).version(smInfo.getVersion())
                    .description(smInfo.getDescription()).vendor(smInfo.getVendor()));

            smInfo.setId(sm.getId());
            smInfo.getArtifacts().stream().forEach(artifact -> files.stream().forEach(file -> {
                if (artifact.getFilename().equalsIgnoreCase(file.getOriginalFilename())) {
                    artifact.setFile(file);
                }
            }));

            createMetadataForSoftwareModule(smInfo);
            softwareModules.add(sm);
        }

        // upload artifacts to SoftwareModules
        if (!files.isEmpty()) {
            uploadArtifacts(softwareModuleInfos);
        }

        LOG.debug("{} software modules created.", softwareModuleInfos.size());

        return softwareModules;
    }

    /**
     * Creates distribution set from the list of {@link SoftwareModule}s.
     *
     * @param softwareModules
     *            is a list of {@link SoftwareModule}s for which distribution
     *            set needs to be generated.
     * @param isMigrationStepRequired
     *            indicates whether this distribution set is a required or
     *            optional. This information is provided by the client through
     *            the URI parameter.
     *
     * @return {@link DistributionSet} created.
     */
    private DistributionSet createDistributionSet(List<SoftwareModule> softwareModules,
            final boolean isMigrationStepRequired) {
        LOG.debug("Creating distribution set with modules {}.",
                softwareModules.stream().map(sm -> sm.getId()).collect(Collectors.toList()));

        String distributionSetName = propertiesAutoConfiguration.getDistributionSetPrefix() + System.currentTimeMillis()
                + "_" + UUID.randomUUID();

        String distributionSetVersion = propertiesAutoConfiguration.getDistributionSetVersion();

        DistributionSetType distributionSetType = !softwareModules.isEmpty() ? getDistributionSetType(softwareModules)
                : null;

        DistributionSetCreate distributionSetCreate = entityFactory.distributionSet().create().name(distributionSetName)
                .version(distributionSetVersion).type(distributionSetType)
                .modules(softwareModules.stream().map(sm -> sm.getId()).collect(Collectors.toList()))
                .requiredMigrationStep(isMigrationStepRequired);

        return distributionSetManagement.createDistributionSet(distributionSetCreate);
    }

    /**
     * Assigns a distribution set to a controller and completes the action.
     *
     * @param controllerId
     *            is the id of the controller whose action has to be completed
     * @param distributionSetId
     *            is the id of the distribution set that was created for
     *            recording the offline update
     * @param targets
     *            is a {@link Collection} of {@link TargetWithActionType}s
     *
     * @return a list of finished {@link Action}s.
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    private List<Action> assignDistributionSetAndFinishAction(Long distributionSetId,
            Collection<TargetWithActionType> targets) {
        offlineUpdateDeploymentManagement.assignDistributionSet(distributionSetId, targets);

        return targets.stream()
                .map(target -> offlineUpdateDeploymentManagement
                        .finishAction(findActionIdFromControllerId(target.getControllerId())))
                .collect(Collectors.toList());
    }

    /**
     * Checks whether a list of multipart files includes the files specified as
     * file names in list of {@link SoftwareModuleInfo}s.
     *
     * @param softwareModules
     *            is a list of {@link SoftwareModuleInfo}s
     * @param files
     *            is a list of {@link MultipartFile}s
     *
     * @return {@code true} for invalid input.
     */
    private boolean isValidInputFileList(List<SoftwareModuleInfo> softwareModules, List<MultipartFile> files) {
        List<String> fileNames = files.stream().map(file -> file.getOriginalFilename()).collect(Collectors.toList());
        final List<String> origFileNames = new ArrayList<>();

        softwareModules.stream().forEach(softwareModule -> softwareModule.getArtifacts().stream()
                .forEach(artifact -> origFileNames.add(artifact.getFilename())));
        return !fileNames.containsAll(origFileNames);
    }

    /**
     * Assigns metadata to the {@link SoftwareModule} associated with a
     * {@link SoftwareModuleInfo}. The metadata consists of the filename and
     * corresponding link.
     *
     * @param softwareModuleInfo
     *            is the input software module for which meta data has to be
     *            generated.
     */
    private void createMetadataForSoftwareModule(SoftwareModuleInfo softwareModuleInfo) {
        Collection<MetaData> metadataList = new ArrayList<>();
        softwareModuleInfo.getArtifacts().stream().forEach(artifact -> {
            String artifactName = artifact.getFilename();
            if (!artifactName.isEmpty()) {
                metadataList.add(entityFactory.generateMetadata("fileName_" + artifactName, artifactName));
                metadataList.add(entityFactory.generateMetadata("href_" + artifactName, artifact.getHref()));
            }
        });
        softwareModuleManagement.createSoftwareModuleMetadata(softwareModuleInfo.getId(), metadataList);
        LOG.debug("Assigned metadata for softwareModule {}.", softwareModuleInfo.getName());
    }

    /**
     * Uploads artifacts for a list of {@link SoftwareModuleInfo}s using
     * {@link ArtifactManagement}. Artifact information should already be
     * updated for each {@link SoftwareModuleInfo} in the list before calling
     * this method.
     *
     * @param softwareModules
     *            is a list of {@link SoftwareModuleInfo}s for which the
     *            artifacts are being uploaded.
     */
    private void uploadArtifacts(final List<SoftwareModuleInfo> softwareModules) {
        softwareModules.stream().forEach(softwareModule -> softwareModule.getArtifacts().stream().forEach(artifact -> {
            MultipartFile file = artifact.getFile();
            try {
                artifactManagement.createArtifact(file.getInputStream(), softwareModule.getId(),
                        file.getOriginalFilename(),
                        artifact.getMd5Hash() == null ? null : artifact.getMd5Hash().toLowerCase(),
                        artifact.getSha1Hash() == null ? null : artifact.getSha1Hash().toLowerCase(), false,
                        file.getContentType());
                LOG.debug("Uploaded artifact for software module {}.", softwareModule.getName());
            } catch (IOException e) {
                LOG.error("InputStream error while uploading artifacts for offline update.", e);
                throw new ArtifactUploadException(e);
            }
        }));
    }

    /**
     * Finds a {@link DistributionSetType} which is compatible with a list of
     * {@link SoftwareModule}s. A {@link DistributionSetType} defines mandatory
     * and optional {@link SoftwareModuleType}s. This method tries to find a
     * {@link DistributionSetType} where mandatory {@link SoftwareModuleType}s
     * are at least fully covered by a subset of input {@link SoftwareModule}s,
     * and the optional {@link SoftwareModuleType}s of such
     * {@link DistributionSetType} cover the remaining (@link SoftwareModules}s.
     *
     * @param softwareModules
     *            is a list of {@link SoftwareModule}s for which a compatible
     *            distribution set type is required
     *
     * @return {@link DistributionSetType}.
     */
    private DistributionSetType getDistributionSetType(List<SoftwareModule> softwareModules) {
        final Sort sorting = new Sort(Direction.ASC, DistributionSetTypeFields.NAME.getFieldName());
        final Pageable pageable = new OffsetBasedPageRequest(DEFAULT_OFFSET_PAGE_PARAM, DEFAULT_OFFSET_PAGE_LIMIT_PARAM,
                sorting);
        Page<DistributionSetType> distributionSetTypeList = distributionSetTypeManagement
                .findDistributionSetTypesAll(pageable);

        Set<SoftwareModuleType> smTypes = softwareModules.stream().map(sm -> sm.getType()).collect(Collectors.toSet());

        for (DistributionSetType dst : distributionSetTypeList) {
            dst.getMandatoryModuleTypes();
            dst.getOptionalModuleTypes();
        }
        // for available distribution sets, check if all the mandatory module
        // types have been covered in the passed software modules' types.
        List<DistributionSetType> dsWithMandatoryTypes = distributionSetTypeList.getContent().stream()
                .filter(dst -> smTypes.containsAll(dst.getMandatoryModuleTypes())).collect(Collectors.toList());

        if (dsWithMandatoryTypes.isEmpty()) {
            String dsNotFoundType = smTypes.stream().map(smt -> smt.getKey()).collect(Collectors.toSet()).toString();
            LOG.warn("No distribution set type found where all mandatory modules are provided for: " + dsNotFoundType);
            throw new DistributionSetTypeNotFoundException(dsNotFoundType);
        }

        // for above distribution set check if all the given software modules'
        // types are covered.
        Optional<DistributionSetType> validDistributionSetType = dsWithMandatoryTypes.stream().filter(dst -> {
            Set<SoftwareModuleType> allType = dst.getOptionalModuleTypes();
            allType.addAll(dst.getMandatoryModuleTypes());
            return allType.containsAll(smTypes);
        }).findFirst();

        if (!validDistributionSetType.isPresent()) {
            LOG.warn("No compatible distribution set type found for: "
                    + smTypes.stream().map(smt -> smt.getKey()).collect(Collectors.toSet()).toString()
                    + ". Create new distribution set type including all module types.");

            throw new DistributionSetTypeNotFoundException(
                    smTypes.stream().map(smt -> smt.getKey()).collect(Collectors.toList()).toString());
        }

        return validDistributionSetType.get();
    }

    /**
     * Find action id for a given controllerId if present.
     *
     * @param controllerId
     *            is the id of the controller for which the action has to be
     *            found
     *
     * @return action id of the controller.
     */
    private Long findActionIdFromControllerId(String controllerId) {
        final Pageable page = new PageRequest(0, 1000, new Sort(Direction.ASC, "id"));
        return offlineUpdateDeploymentManagement.findActiveActionsByTarget(page, controllerId).getContent().stream()
                .findAny().orElse(null).getId();
    }
}
