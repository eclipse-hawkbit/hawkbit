/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken.TagData;
import org.eclipse.hawkbit.ui.components.HawkbitErrorNotificationMessage;
import org.eclipse.hawkbit.ui.management.event.BulkUploadValidationMessageEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetBulkUpload;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
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
public class BulkUploadHandler extends CustomComponent
        implements SucceededListener, FailedListener, Receiver, StartedListener {

    private static final long serialVersionUID = -1273494705754674501L;
    private static final Logger LOG = LoggerFactory.getLogger(BulkUploadHandler.class);

    private static final Splitter SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private final transient TargetManagement targetManagement;
    private final transient TargetTagManagement tagManagement;

    private final ComboBox comboBox;
    private final TextArea descTextArea;
    private final VaadinMessageSource i18n;
    private final transient DeploymentManagement deploymentManagement;
    private final transient DistributionSetManagement distributionSetManagement;

    private File tempFile;
    private Upload upload;

    private final ProgressBar progressBar;
    private final ManagementUIState managementUIState;
    private final TargetBulkTokenTags targetBulkTokenTags;

    private final Label targetsCountLabel;
    private int successfullTargetCount;

    private final transient Executor uiExecutor;
    private final transient EventBus.UIEventBus eventBus;

    private final transient EntityFactory entityFactory;
    private final UI uiInstance;

    BulkUploadHandler(final TargetBulkUpdateWindowLayout targetBulkUpdateWindowLayout,
            final TargetManagement targetManagement, final TargetTagManagement tagManagement,
            final EntityFactory entityFactory, final DistributionSetManagement distributionSetManagement,
            final ManagementUIState managementUIState, final DeploymentManagement deploymentManagement,
            final VaadinMessageSource i18n, final UI uiInstance, final Executor uiExecutor) {
        this.uiInstance = uiInstance;
        this.comboBox = targetBulkUpdateWindowLayout.getDsNamecomboBox();
        this.descTextArea = targetBulkUpdateWindowLayout.getDescTextArea();
        this.targetManagement = targetManagement;
        this.progressBar = targetBulkUpdateWindowLayout.getProgressBar();
        this.managementUIState = managementUIState;
        this.deploymentManagement = deploymentManagement;
        this.targetsCountLabel = targetBulkUpdateWindowLayout.getTargetsCountLabel();
        this.targetBulkTokenTags = targetBulkUpdateWindowLayout.getTargetBulkTokenTags();
        this.i18n = i18n;
        this.uiExecutor = uiExecutor;
        this.eventBus = targetBulkUpdateWindowLayout.getEventBus();
        this.distributionSetManagement = distributionSetManagement;
        this.tagManagement = tagManagement;
        this.entityFactory = entityFactory;
    }

    void buildLayout() {
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        upload = new Upload();
        upload.setEnabled(false);
        upload.setButtonCaption("Bulk Upload");
        upload.setReceiver(this);
        upload.setImmediate(true);
        upload.setWidthUndefined();
        upload.addSucceededListener(this);
        upload.addFailedListener(this);
        upload.addStartedListener(this);
        horizontalLayout.addComponent(upload);
        horizontalLayout.setComponentAlignment(upload, Alignment.BOTTOM_RIGHT);
        setCompositionRoot(horizontalLayout);
    }

    @Override
    public OutputStream receiveUpload(final String filename, final String mimeType) {
        try {
            tempFile = File.createTempFile("temp", ".csv");
            progressBar.setVisible(false);
            targetsCountLabel.setVisible(false);
            return new FileOutputStream(tempFile);
        } catch (final FileNotFoundException e) {
            LOG.error("File was not found with file name {}", filename, e);
        } catch (final IOException e) {
            LOG.error("Error while reading file {}", filename, e);
        }
        return ByteStreams.nullOutputStream();
    }

    @Override
    public void uploadFailed(final FailedEvent event) {
        LOG.info("Upload failed for file :{} due to {}", event.getFilename(), event.getReason());
    }

    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        uiExecutor.execute(new UploadAsync());
    }

    class UploadAsync implements Runnable {

        @Override
        public void run() {
            if (tempFile == null) {
                return;
            }
            try (InputStream tempStream = new FileInputStream(tempFile)) {
                readFileStream(tempStream);
            } catch (final FileNotFoundException e) {
                LOG.error("Temporary file not found with name {}", tempFile.getName(), e);
            } catch (final IOException e) {
                LOG.error("Error while opening temorary file ", e);
            }
        }

        private void readFileStream(final InputStream tempStream) {
            String line;
            final BigDecimal totalNumberOfLines = getTotalNumberOfLines();
            try (final LineNumberReader reader = new LineNumberReader(
                    new InputStreamReader(tempStream, Charset.defaultCharset()))) {
                LOG.info("Bulk file upload started");

                /**
                 * Once control is in upload succeeded method automatically
                 * upload button is re-enabled. To disable the button firing
                 * below event.
                 */
                eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_UPLOAD_PROCESS_STARTED));

                while ((line = reader.readLine()) != null) {
                    readLine(line, reader.getLineNumber(), totalNumberOfLines);
                }

            } catch (final IOException e) {
                LOG.error("Error reading file {}", tempFile.getName(), e);
            } catch (final RuntimeException e) {
                uiInstance.getErrorHandler().error(new ConnectorErrorEvent(uiInstance, e));
            } finally {
                deleteFile();
            }
            syncCountAfterUpload(totalNumberOfLines.intValue());
            doAssignments();
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_UPLOAD_COMPLETED));
            // Clearing after assignments are done
            managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().clear();
            resetSuccessfullTargetCount();
        }

        private void syncCountAfterUpload(final int totalNumberOfLines) {
            final int syncedFailedTargetCount = totalNumberOfLines - successfullTargetCount;
            managementUIState.getTargetTableFilters().getBulkUpload().setSucessfulUploadCount(successfullTargetCount);
            managementUIState.getTargetTableFilters().getBulkUpload().setFailedUploadCount(syncedFailedTargetCount);
            managementUIState.getTargetTableFilters().getBulkUpload().setProgressBarCurrentValue(1);
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_TARGET_CREATED));
        }

        private BigDecimal getTotalNumberOfLines() {
            try {
                return new BigDecimal(Files.readLines(tempFile, Charset.defaultCharset(), new LineProcessor<Integer>() {
                    private int count;

                    @Override
                    public Integer getResult() {
                        return count;
                    }

                    @Override
                    public boolean processLine(final String line) {
                        count++;
                        return true;
                    }
                }));

            } catch (final IOException e) {
                LOG.error("Error while reading temp file for upload.", e);
            }

            return new BigDecimal(0);
        }

        private void resetSuccessfullTargetCount() {
            successfullTargetCount = 0;
        }

        private void deleteFile() {
            if (tempFile.exists()) {
                final boolean isDeleted = tempFile.delete();
                if (!isDeleted) {
                    LOG.info("File {} was not deleted !", tempFile.getName());
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

            final float previous = managementUIState.getTargetTableFilters().getBulkUpload()
                    .getProgressBarCurrentValue();
            final float done = new BigDecimal(lineNumber).divide(totalNumberOfLines, 2, RoundingMode.UP).floatValue();

            if (done > previous) {
                managementUIState.getTargetTableFilters().getBulkUpload()
                        .setSucessfulUploadCount(successfullTargetCount);
                managementUIState.getTargetTableFilters().getBulkUpload().setProgressBarCurrentValue(done);
                eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_TARGET_CREATED));
            }
        }

        private void doAssignments() {
            final StringBuilder errorMessage = new StringBuilder();
            String dsAssignmentFailedMsg = null;
            String tagAssignmentFailedMsg = null;
            if (ifTargetsCreatedSuccessfully()) {
                if (ifTagsSelected()) {
                    tagAssignmentFailedMsg = tagAssignment();
                }
                if (ifDsSelected()) {
                    dsAssignmentFailedMsg = saveAllAssignments();
                }
            }
            displayValidationMessage(errorMessage, dsAssignmentFailedMsg, tagAssignmentFailedMsg);
        }

        private String saveAllAssignments() {
            final ActionType actionType = ActionType.FORCED;
            final long forcedTimeStamp = new Date().getTime();
            final TargetBulkUpload targetBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload();
            final List<String> targetsList = targetBulkUpload.getTargetsCreated();
            final Long dsSelected = (Long) comboBox.getValue();
            if (!distributionSetManagement.get(dsSelected).isPresent()) {
                return i18n.getMessage("message.bulk.upload.assignment.failed");
            }
            deploymentManagement.assignDistributionSet(targetBulkUpload.getDsNameAndVersion(), actionType,
                    forcedTimeStamp, targetsList);
            return null;
        }

        private String tagAssignment() {
            final Map<Long, TagData> tokensSelected = targetBulkTokenTags.getTokensAdded();
            final List<String> deletedTags = new ArrayList<>();
            for (final TagData tagData : tokensSelected.values()) {
                if (!tagManagement.get(tagData.getId()).isPresent()) {
                    deletedTags.add(tagData.getName());
                } else {
                    targetManagement.toggleTagAssignment(
                            managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated(),
                            tagData.getName());
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

        private boolean ifTagsSelected() {
            return targetBulkTokenTags.getTokenField().getValue() != null;
        }

        private boolean ifDsSelected() {
            return comboBox.getValue() != null;
        }

        private boolean ifTargetsCreatedSuccessfully() {
            return !managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().isEmpty();
        }

        private void displayValidationMessage(final StringBuilder errorMessage, final String dsAssignmentFailedMsg,
                final String tagAssignmentFailedMsg) {
            if (dsAssignmentFailedMsg != null) {
                errorMessage.append(dsAssignmentFailedMsg);
            }
            if (errorMessage.length() > 0) {
                errorMessage.append("<br>");
            }
            if (tagAssignmentFailedMsg != null) {
                errorMessage.append(tagAssignmentFailedMsg);
            }
            if (errorMessage.length() > 0) {
                eventBus.publish(this, new BulkUploadValidationMessageEvent(errorMessage.toString()));
            }
        }

        // Exception squid:S1166 - Targets that exist already are simply ignored
        @SuppressWarnings("squid:S1166")
        private void addNewTarget(final String controllerId, final String name) {
            final String newControllerId = controllerId;
            final String description = descTextArea.getValue();

            try {
                targetManagement.create(entityFactory.target().create().controllerId(newControllerId).name(name)
                        .description(description));

                managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().add(newControllerId);
                successfullTargetCount++;

            } catch (final EntityAlreadyExistsException ex) {
                // Targets that exist already are simply ignored
            }
        }
    }

    /**
     * @return the upload
     */
    public Upload getUpload() {
        return upload;
    }

    @Override
    public void uploadStarted(final StartedEvent event) {
        if (!event.getFilename().endsWith(".csv")) {

            new HawkbitErrorNotificationMessage(SPUILabelDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, null,
                    i18n.getMessage("bulk.targets.upload"), true).show(Page.getCurrent());
            LOG.error("Wrong file format for file {}", event.getFilename());
            upload.interruptUpload();
        } else {
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_TARGET_UPLOAD_STARTED));
        }
    }

}
