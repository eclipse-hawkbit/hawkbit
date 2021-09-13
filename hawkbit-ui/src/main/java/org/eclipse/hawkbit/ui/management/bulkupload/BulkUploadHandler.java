/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.bulkupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyBulkUploadWindow;
import org.eclipse.hawkbit.ui.common.event.BulkUploadEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

/**
 * Bulk target upload handler.
 */
public class BulkUploadHandler implements SucceededListener, FailedListener, Receiver, StartedListener {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(BulkUploadHandler.class);

    private static final Splitter SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private final VaadinMessageSource i18n;

    private final transient Executor uiExecutor;
    private final transient UIEventBus eventBus;
    private final transient EntityFactory entityFactory;

    private final transient TargetManagement targetManagement;
    private final transient TargetTagManagement tagManagement;
    private final transient DeploymentManagement deploymentManagement;
    private final transient DistributionSetManagement distributionSetManagement;

    private File tempFile;

    private final transient Supplier<ProxyBulkUploadWindow> bulkUploadInputsProvider;

    BulkUploadHandler(final CommonUiDependencies uiDependencies, final Executor uiExecutor,
            final TargetManagement targetManagement, final TargetTagManagement tagManagement,
            final DistributionSetManagement distributionSetManagement, final DeploymentManagement deploymentManagement,
            final Supplier<ProxyBulkUploadWindow> bulkUploadInputsProvider) {
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.i18n = uiDependencies.getI18n();
        this.uiExecutor = uiExecutor;
        this.eventBus = uiDependencies.getEventBus();
        this.distributionSetManagement = distributionSetManagement;
        this.tagManagement = tagManagement;
        this.entityFactory = uiDependencies.getEntityFactory();
        this.bulkUploadInputsProvider = bulkUploadInputsProvider;
    }

    @Override
    public OutputStream receiveUpload(final String filename, final String mimeType) {
        try {
            tempFile = File.createTempFile("temp", ".csv");

            return new FileOutputStream(tempFile);
        } catch (final FileNotFoundException e) {
            LOG.warn("File was not found with file name '{}': ", filename, e);
            publishUploadFailed();
        } catch (final IOException e) {
            LOG.warn("Error while reading file '{}': ", filename, e);
            publishUploadFailed();
        }

        return ByteStreams.nullOutputStream();
    }

    @Override
    public void uploadFailed(final FailedEvent event) {
        LOG.warn("Upload failed for file '{}' due to '{}'", event.getFilename(), event.getReason().getMessage());
        publishUploadFailed();
    }

    private void publishUploadFailed() {
        publishUploadFailed(i18n.getMessage("message.upload.failed"));
    }

    private void publishUploadFailed(final String failureReason) {
        eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                BulkUploadEventPayload.buildUploadFailed(failureReason));
    }

    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        uiExecutor.execute(new UploadAsync(VaadinSession.getCurrent(), UI.getCurrent()));
    }

    private class UploadAsync implements Runnable {
        private final VaadinSession vaadinSession;
        private final UI vaadinUI;

        private ProxyBulkUploadWindow bulkUploadInputs;

        private List<String> provisionedControllerIds;
        private float currentProgress;

        /**
         * Constructor for UploadAsync
         *
         * @param vaadinSession
         *            VaadinSession
         * @param vaadinUI
         *            UI
         */
        public UploadAsync(final VaadinSession vaadinSession, final UI vaadinUI) {
            this.vaadinSession = vaadinSession;
            this.vaadinUI = vaadinUI;

            this.bulkUploadInputs = bulkUploadInputsProvider.get();

            this.provisionedControllerIds = new ArrayList<>();
            this.currentProgress = 0;
        }

        @Override
        public void run() {
            if (tempFile == null) {
                return;
            }

            VaadinSession.setCurrent(vaadinSession);
            UI.setCurrent(vaadinUI);
            eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                    BulkUploadEventPayload.buildTargetProvisioningStarted());

            try (InputStream tempStream = new FileInputStream(tempFile)) {
                readFileStream(tempStream);
            } catch (final FileNotFoundException e) {
                LOG.warn("Temporary file not found with name '{}': ", tempFile.getName(), e);
                publishUploadFailed();
            } catch (final IOException e) {
                LOG.warn("Error while opening temporary file ", e);
                publishUploadFailed();
            } finally {
                bulkUploadInputs = null;
                provisionedControllerIds = null;
                currentProgress = 0;
            }
        }

        private void readFileStream(final InputStream tempStream) {
            String line;
            final BigDecimal totalNumberOfLines = new BigDecimal(getTotalNumberOfLines());
            try (final LineNumberReader reader = new LineNumberReader(
                    new InputStreamReader(tempStream, Charset.defaultCharset()))) {
                while ((line = reader.readLine()) != null) {
                    readLine(line, reader.getLineNumber(), totalNumberOfLines);
                }
            } catch (final IOException | RuntimeException e) {
                LOG.warn("Error reading file '{}': ", tempFile.getName(), e);
                publishUploadFailed(i18n.getMessage("message.upload.failed.with.reason", e.getLocalizedMessage()));
            } finally {
                deleteFile();
            }

            eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                    BulkUploadEventPayload.buildTagsAndDsAssignmentStarted());
            doAssignments();

            eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                    BulkUploadEventPayload.buildBulkUploadCompleted(provisionedControllerIds.size(),
                            totalNumberOfLines.intValue() - provisionedControllerIds.size()));
        }

        private long getTotalNumberOfLines() {
            try (final Stream<String> linesStream = Files.lines(tempFile.toPath())) {
                return linesStream.count();
            } catch (final IOException e) {
                LOG.warn("Error while reading temp file for upload: ", e);
                publishUploadFailed();
            }

            return 0L;
        }

        private void deleteFile() {
            try {
                Files.deleteIfExists(tempFile.toPath());
            } catch (final IOException e) {
                LOG.warn("File '{}' was not deleted! Trying again...", tempFile.getName());

                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (final IOException ex) {
                    LOG.warn("File '{}' was not deleted! The reason is: '{}'", tempFile.getName(), ex.getMessage());
                }
            }

            tempFile = null;
        }

        private void readLine(final String line, final int lineNumber, final BigDecimal totalNumberOfLines) {
            final List<String> targets = SPLITTER.splitToList(line);
            if (targets.size() == 2) {
                final String controllerId = targets.get(0);
                final String targetName = targets.get(1);
                addNewTarget(controllerId, targetName);
            }

            final float linesProcessedPercentage = new BigDecimal(lineNumber)
                    .divide(totalNumberOfLines, 2, RoundingMode.UP).floatValue();

            if (linesProcessedPercentage > currentProgress) {
                currentProgress = linesProcessedPercentage;

                eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                        BulkUploadEventPayload.buildTargetProvisioningProgressUpdated(currentProgress));
            }
        }

        private void doAssignments() {
            String dsAssignmentFailedMsg = null;
            String tagAssignmentFailedMsg = null;

            if (areTargetsCreatedSuccessfully()) {
                if (areTagsSelected()) {
                    tagAssignmentFailedMsg = tagAssignment();
                }
                if (isDsSelected()) {
                    dsAssignmentFailedMsg = saveAllAssignments();
                }
            }
            displayValidationMessage(dsAssignmentFailedMsg, tagAssignmentFailedMsg);
        }

        private String saveAllAssignments() {
            final ActionType actionType = ActionType.FORCED;
            final long forcedTimeStamp = new Date().getTime();
            final Long dsId = bulkUploadInputs.getDistributionSetInfo().getId();

            if (!distributionSetManagement.get(dsId).isPresent()) {
                return i18n.getMessage("message.bulk.upload.assignment.failed");
            }

            final List<DeploymentRequest> deploymentRequests = provisionedControllerIds.stream()
                    .map(controllerId -> DeploymentManagement.deploymentRequest(controllerId, dsId)
                            .setActionType(actionType).setForceTime(forcedTimeStamp).build())
                    .collect(Collectors.toList());
            try {
                deploymentManagement.assignDistributionSets(deploymentRequests);
            } catch (EntityNotFoundException | IncompleteDistributionSetException | AssignmentQuotaExceededException
                    | MultiAssignmentIsNotEnabledException e) {
                LOG.warn("Bulk uploaded targets assignment to ds id '{}' failed due to '{}'", dsId, e.getMessage());
                return i18n.getMessage("message.bulk.upload.assignment.failed");
            }

            return null;
        }

        private String tagAssignment() {
            final List<String> deletedTags = new ArrayList<>();
            final Map<Long, String> tagIdsWithName = bulkUploadInputs.getTagIdsWithNameToAssign();

            for (final Entry<Long, String> tagIdWithName : tagIdsWithName.entrySet()) {
                if (!tagManagement.get(tagIdWithName.getKey()).isPresent()) {
                    deletedTags.add(tagIdWithName.getValue());
                } else {
                    targetManagement.toggleTagAssignment(provisionedControllerIds, tagIdWithName.getValue());
                }
            }
            if (deletedTags.isEmpty()) {
                return null;
            }
            if (deletedTags.size() == 1) {
                return i18n.getMessage("message.bulk.upload.tag.assignment.failed", deletedTags.get(0));
            }
            return i18n.getMessage("message.bulk.upload.tag.assignments.failed");
        }

        private boolean areTagsSelected() {
            return !CollectionUtils.isEmpty(bulkUploadInputs.getTagIdsWithNameToAssign());
        }

        private boolean isDsSelected() {
            return bulkUploadInputs.getDistributionSetInfo() != null;
        }

        private boolean areTargetsCreatedSuccessfully() {
            return !CollectionUtils.isEmpty(provisionedControllerIds);
        }

        private void displayValidationMessage(final String dsAssignmentFailedMsg, final String tagAssignmentFailedMsg) {
            final StringBuilder errorMessage = new StringBuilder();

            if (dsAssignmentFailedMsg != null) {
                errorMessage.append(dsAssignmentFailedMsg);
            }
            if (errorMessage.length() > 0) {
                errorMessage.append("\n");
            }
            if (tagAssignmentFailedMsg != null) {
                errorMessage.append(tagAssignmentFailedMsg);
            }
            if (errorMessage.length() > 0) {
                eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                        BulkUploadEventPayload.buildTagsAndDsAssignmentFailed(errorMessage.toString()));
            }
        }

        // Exception squid:S1166 - Targets that exist already are simply ignored
        @SuppressWarnings("squid:S1166")
        private void addNewTarget(final String controllerId, final String name) {
            try {
                targetManagement.create(entityFactory.target().create().controllerId(controllerId).name(name)
                        .description(bulkUploadInputs.getDescription())
                        .targetType(bulkUploadInputs.getTypeInfo() != null ? bulkUploadInputs.getTypeInfo().getId() : null));

                provisionedControllerIds.add(controllerId);
            } catch (final EntityAlreadyExistsException ex) {
                // Targets that exist already are simply ignored
                LOG.trace("Entity '{} - {}' already exists and will be ignored", controllerId, name);
            }
        }
    }

    @Override
    public void uploadStarted(final StartedEvent event) {
        if (!event.getFilename().endsWith(".csv")) {
            publishUploadFailed(i18n.getMessage("bulkupload.wrong.file.format"));
            event.getUpload().interruptUpload();
        } else {
            eventBus.publish(EventScope.SESSION, EventTopics.BULK_UPLOAD_CHANGED, this,
                    BulkUploadEventPayload.buildUploadStarted());
        }
    }

}
