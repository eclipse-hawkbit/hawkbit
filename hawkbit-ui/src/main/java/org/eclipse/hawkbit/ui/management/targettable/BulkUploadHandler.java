/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken.TagData;
import org.eclipse.hawkbit.ui.components.HawkbitErrorNotificationMessage;
import org.eclipse.hawkbit.ui.management.event.BulkUploadValidationMessageEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetBulkUpload;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

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

    private final transient TargetManagement targetManagement;
    private final transient TagManagement tagManagement;

    private final ComboBox comboBox;
    private final TextArea descTextArea;
    private final I18N i18n;
    private final transient DeploymentManagement deploymentManagement;
    private final transient DistributionSetManagement distributionSetManagement;

    protected File tempFile;
    private Upload upload;

    private final ProgressBar progressBar;
    private final ManagementUIState managementUIState;
    private final TargetBulkTokenTags targetBulkTokenTags;

    private final Label targetsCountLabel;
    private long failedTargetCount;
    private long successfullTargetCount;

    private final transient Executor executor;
    private final transient EventBus.UIEventBus eventBus;

    private transient EntityFactory entityFactory;
    private final UI uiInstance;

    BulkUploadHandler(final TargetBulkUpdateWindowLayout targetBulkUpdateWindowLayout,
            final TargetManagement targetManagement, final ManagementUIState managementUIState,
            final DeploymentManagement deploymentManagement, final I18N i18n, final UI uiInstance) {
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
        executor = (Executor) SpringContextHelper.getBean("uiExecutor");
        this.eventBus = targetBulkUpdateWindowLayout.getEventBus();
        distributionSetManagement = SpringContextHelper.getBean(DistributionSetManagement.class);
        tagManagement = SpringContextHelper.getBean(TagManagement.class);
        entityFactory = SpringContextHelper.getBean(EntityFactory.class);
    }

    /**
     * Intialize layout.
     */
    public void buildLayout() {
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
        return new NullOutputStream();
    }

    @Override
    public void uploadFailed(final FailedEvent event) {
        LOG.info("Upload failed for file :{} due to {}", event.getFilename(), event.getReason());
    }

    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        executor.execute(new UploadAsync(event));
    }

    class UploadAsync implements Runnable {

        final SucceededEvent event;

        /**
         *
         * @param event
         */
        public UploadAsync(final SucceededEvent event) {
            this.event = event;
        }

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
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(tempStream, Charset.defaultCharset()))) {
                LOG.info("Bulk file upload started");
                long innerCounter = 0;
                final double totalFileSize = getTotalNumberOfLines();

                /**
                 * Once control is in upload succeeded method automatically
                 * upload button is re-enabled. To disable the button firing
                 * below event.
                 */
                eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_UPLOAD_PROCESS_STARTED));
                while ((line = reader.readLine()) != null) {
                    innerCounter++;
                    readEachLine(line, innerCounter, totalFileSize);
                }

            } catch (final IOException e) {
                LOG.error("Error reading file {}", tempFile.getName(), e);
            } catch (final RuntimeException e) {
                uiInstance.getErrorHandler().error(new ConnectorErrorEvent(uiInstance, e));
            } finally {
                deleteFile();
            }
            syncCountAfterUpload();
            doAssignments();
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_UPLOAD_COMPLETED));
            // Clearing after assignments are done
            managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().clear();
            resetCounts();
        }

        private void syncCountAfterUpload() {
            if (managementUIState.getTargetTableFilters().getBulkUpload()
                    .getSucessfulUploadCount() != successfullTargetCount) {
                managementUIState.getTargetTableFilters().getBulkUpload()
                        .setSucessfulUploadCount(successfullTargetCount);
                eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_TARGET_CREATED));
            }
            if (managementUIState.getTargetTableFilters().getBulkUpload().getFailedUploadCount() != failedTargetCount) {
                managementUIState.getTargetTableFilters().getBulkUpload().setSucessfulUploadCount(failedTargetCount);
            }
        }

        private double getTotalNumberOfLines() {

            double totalFileSize = 0;
            try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(tempFile),
                    Charset.defaultCharset())) {
                try (BufferedReader readerForSize = new BufferedReader(inputStreamReader)) {
                    totalFileSize = readerForSize.lines().count();
                }
            } catch (final FileNotFoundException e) {
                LOG.error("Error reading file {}", tempFile.getName(), e);
            } catch (final IOException e) {
                LOG.error("Error while closing reader of file {}", tempFile.getName(), e);
            }

            return totalFileSize;
        }

        private void resetCounts() {
            successfullTargetCount = 0;
            failedTargetCount = 0;
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

        private void readEachLine(final String line, final double innerCounter, final double totalFileSize) {
            final String csvDelimiter = ",";
            final String[] targets = line.split(csvDelimiter);
            if (targets.length == 2) {
                final String controllerId = targets[0];
                final String targetName = targets[1];
                addNewTarget(controllerId, targetName);
            } else {
                failedTargetCount++;
            }
            final float current = managementUIState.getTargetTableFilters().getBulkUpload()
                    .getProgressBarCurrentValue();
            final float next = (float) (innerCounter / totalFileSize);
            if (Math.abs(next - 0.1) < 0.00001 || current - next >= 0 || next - current >= 0.05
                    || Math.abs(next - 1) < 0.00001) {
                managementUIState.getTargetTableFilters().getBulkUpload().setProgressBarCurrentValue(next);
                managementUIState.getTargetTableFilters().getBulkUpload()
                        .setSucessfulUploadCount(successfullTargetCount);
                managementUIState.getTargetTableFilters().getBulkUpload().setFailedUploadCount(failedTargetCount);
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
            final DistributionSetIdName dsSelected = (DistributionSetIdName) comboBox.getValue();
            if (distributionSetManagement.findDistributionSetById(dsSelected.getId()) == null) {
                return i18n.get("message.bulk.upload.assignment.failed");
            }
            deploymentManagement.assignDistributionSet(targetBulkUpload.getDsNameAndVersion().getId(), actionType,
                    forcedTimeStamp, targetsList);
            return null;
        }

        private String tagAssignment() {
            final Map<Long, TagData> tokensSelected = targetBulkTokenTags.getTokensAdded();
            final List<String> deletedTags = new ArrayList<>();
            for (final TagData tagData : tokensSelected.values()) {
                if (tagManagement.findTargetTagById(tagData.getId()) == null) {
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
                return i18n.get("message.bulk.upload.tag.assignment.failed", deletedTags.get(0));
            }
            return i18n.get("message.bulk.upload.tag.assignments.failed");
        }

        private boolean ifTagsSelected() {
            return targetBulkTokenTags.getTokenField().getValue() != null;
        }

        /**
         * @return
         */
        private boolean ifDsSelected() {
            return comboBox.getValue() != null;
        }

        /**
         * @return
         */
        private boolean ifTargetsCreatedSuccessfully() {
            return !managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().isEmpty();
        }

        /**
         * @param errorMessage
         * @param dsAssignmentFailedMsg
         * @param tagAssignmentFailedMsg
         */
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

        private void addNewTarget(final String controllerId, final String name) {
            final String newControllerId = HawkbitCommonUtil.trimAndNullIfEmpty(controllerId);
            if (mandatoryCheck(newControllerId) && duplicateCheck(newControllerId)) {
                final String newName = HawkbitCommonUtil.trimAndNullIfEmpty(name);
                final String newDesc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());

                /* create new target entity */
                targetManagement.createTarget(entityFactory.target().create().controllerId(newControllerId)
                        .name(newName).description(newDesc));
                managementUIState.getTargetTableFilters().getBulkUpload().getTargetsCreated().add(newControllerId);
                successfullTargetCount++;
            }

        }
    }

    private boolean mandatoryCheck(final String newControlllerId) {
        if (newControlllerId == null) {
            failedTargetCount++;
            return false;
        } else {
            return true;
        }
    }

    private boolean duplicateCheck(final String newControlllerId) {
        final Target existingTarget = targetManagement.findTargetByControllerID(newControlllerId.trim());
        if (existingTarget != null) {
            failedTargetCount++;
            return false;
        } else {
            return true;
        }
    }

    private static class NullOutputStream extends OutputStream {
        /**
         * null output stream.
         *
         * @param i
         *            byte
         */
        @Override
        public void write(final int i) throws IOException {
            // do nothing
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
                    i18n.get("bulk.targets.upload"), true).show(Page.getCurrent());
            LOG.error("Wrong file format for file {}", event.getFilename());
            upload.interruptUpload();
        } else {
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.BULK_TARGET_UPLOAD_STARTED));
        }
    }

}
