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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

/**
 * Bulk target upload handler.
 * 
 *
 *
 */
public class BulkUploadHandler extends CustomComponent implements SucceededListener, FailedListener, Receiver {

    /**
    * 
    */
    private static final long serialVersionUID = -1273494705754674501L;
    private static final Logger LOG = LoggerFactory.getLogger(BulkUploadHandler.class);

    private final transient TargetManagement targetManagement;

    private final ComboBox comboBox;
    private final TextArea descTextArea;
    private final I18N i18n;
    private final transient DeploymentManagement deploymentManagement;
    private final UINotification uINotification;

    protected File tempFile = null;
    private final ProgressBar progressBar;
    private final ManagementUIState managementUIState;
    private final TargetBulkTokenTags targetBulkTokenTags;

    private final Label targetsCountLabel;
    private long failedTargetCount = 0;
    private long successfullTargetCount = 0;
    private final Set<TargetIdName> targetIdNames = new HashSet<>();

    private final transient Executor executor;

    /**
     * 
     * @param targetBulkUpdateWindowLayout
     * @param targetManagement
     * @param managementUIState
     * @param deploymentManagement
     * @param uINotification
     * @param i18n
     */
    public BulkUploadHandler(final TargetBulkUpdateWindowLayout targetBulkUpdateWindowLayout,
            final TargetManagement targetManagement, final ManagementUIState managementUIState,
            final DeploymentManagement deploymentManagement, final UINotification uINotification, final I18N i18n) {
        this.comboBox = targetBulkUpdateWindowLayout.getDsNamecomboBox();
        this.descTextArea = targetBulkUpdateWindowLayout.getDescTextArea();
        this.targetManagement = targetManagement;
        this.progressBar = targetBulkUpdateWindowLayout.getProgressBar();
        this.managementUIState = managementUIState;
        this.deploymentManagement = deploymentManagement;
        this.uINotification = uINotification;
        this.targetsCountLabel = targetBulkUpdateWindowLayout.getTargetsCountLabel();
        this.targetBulkTokenTags = targetBulkUpdateWindowLayout.getTargetBulkTokenTags();
        this.i18n = i18n;
        executor = (Executor) SpringContextHelper.getBean("uiExecutor");
    }

    /**
     * Intialize layout.
     */
    public void buildLayout() {
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        final Upload upload = new Upload();
        upload.setButtonCaption("Bulk Upload");
        upload.setReceiver(this);
        upload.setImmediate(true);
        upload.setWidthUndefined();
        upload.addSucceededListener(this);
        upload.addFailedListener(this);
        horizontalLayout.addComponent(upload);
        horizontalLayout.setComponentAlignment(upload, Alignment.BOTTOM_RIGHT);
        setCompositionRoot(horizontalLayout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Upload.Receiver#receiveUpload(java.lang.String,
     * java.lang.String)
     */
    @Override
    public OutputStream receiveUpload(final String filename, final String mimeType) {
        if (!filename.endsWith(".csv")) {
            uINotification.displayError(i18n.get("bulk.targets.upload"), null, true);
        } else {
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
        }
        return new NullOutputStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.ui.Upload.FailedListener#uploadFailed(com.vaadin.ui.Upload.
     * FailedEvent)
     */
    @Override
    public void uploadFailed(final FailedEvent event) {
        LOG.info("Upload failed for file :{} due to {}", event.getFilename(), event.getReason());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.ui.Upload.SucceededListener#uploadSucceeded(com.vaadin.ui.
     * Upload.SucceededEvent)
     */
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
            BufferedReader reader = null;
            long innerCounter = 0;
            String line;
            if (tempFile != null) {
                try {
                    LOG.info("Charset {}", Charset.defaultCharset());
                    final double totalFileSize = getTotalNumberOfLines();
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile),
                            Charset.defaultCharset()));
                    while ((line = reader.readLine()) != null) {
                        innerCounter++;
                        readEachLine(event, line, innerCounter, totalFileSize);
                    }
                    if (!targetIdNames.isEmpty()) {
                        saveAllAssignments();
                        assignTag();
                    }
                    String targetCountLabel = "";
                    if (successfullTargetCount != 0 || failedTargetCount != 0) {
                        targetCountLabel = new StringBuilder().append("Successful :").append(successfullTargetCount)
                                .append("<font color=RED> Failed :").append(failedTargetCount).append("</font>")
                                .toString();
                    }
                    final String count = targetCountLabel;
                    event.getComponent().getUI().access(() -> displayCount(count));
                } catch (final FileNotFoundException e) {
                    LOG.error("File not found with name {}", tempFile.getName(), e);
                } catch (final IOException e) {
                    LOG.error("Error reading file {}", tempFile.getName(), e);
                } finally {
                    try {
                        if (null != reader) {
                            reader.close();
                            resetCounts();
                            deleteFile();
                        }
                    } catch (final IOException e) {
                        LOG.error("Error while reading file ", e);
                    }
                }
            }

        }

    }

    private double getTotalNumberOfLines() {
        InputStreamReader inputStreamReader;
        BufferedReader readerForSize = null;
        double totalFileSize = 0;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(tempFile), Charset.defaultCharset());
            readerForSize = new BufferedReader(inputStreamReader);
            totalFileSize = readerForSize.lines().count();
        } catch (final FileNotFoundException e) {
            LOG.error("Error reading file {}", tempFile.getName(), e);
        } finally {
            if (readerForSize != null) {
                try {
                    readerForSize.close();
                } catch (final IOException e) {
                    LOG.error("Error while closing reader of file {}", tempFile.getName(), e);
                }
            }
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

    private void readEachLine(final SucceededEvent event, final String line, final double innerCounter,
            final double totalFileSize) {
        final String csvDelimiter = ",";
        final String[] targets = line.split(csvDelimiter);
        if (targets.length == 2) {
            final String controllerId = targets[0];
            final String targetName = targets[1];
            addNewTarget(controllerId, targetName);
            final float current = progressBar.getValue();
            final float next = (float) (innerCounter / totalFileSize);
            if (Math.abs(next - 0.1) < 0.00001 || current - next >= 0 || next - current >= 0.3
                    || Math.abs(next - 1) < 0.00001) {
                event.getComponent().getUI().access(() -> updateProgressBar(next));
            }
        } else {
            failedTargetCount++;
        }

    }

    private void displayCount(final String targetCountLabel) {
        targetsCountLabel.setVisible(true);
        targetsCountLabel.setCaptionAsHtml(true);
        targetsCountLabel.setCaption(targetCountLabel);
    }

    void updateProgressBar(final float value) {
        progressBar.setVisible(true);
        progressBar.setValue(value);
        progressBar.getUI().push();
    }

    private void assignTag() {
        if (targetBulkTokenTags.getTokenField().getValue() != null) {
            tagAssignment();
        }
    }

    private void addNewTarget(final String controllerId, final String name) {
        final String newControllerId = HawkbitCommonUtil.trimAndNullIfEmpty(controllerId);
        if (mandatoryCheck(newControllerId) && duplicateCheck(newControllerId)) {
            final String newName = HawkbitCommonUtil.trimAndNullIfEmpty(name);
            final String newDesc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());

            /* create new target entity */
            Target newTarget = new Target(newControllerId);
            setTargetValues(newTarget, newName, newDesc);
            newTarget = targetManagement.createTarget(newTarget);
            if (comboBox.getValue() != null) {
                dsToTargetAssignment(newTarget.getTargetIdName());
            }
            targetIdNames.add(newTarget.getTargetIdName());
            successfullTargetCount++;
        }

    }

    private void dsToTargetAssignment(final TargetIdName targetId) {
        managementUIState.getAssignedList().put(targetId, (DistributionSetIdName) comboBox.getValue());

    }

    private void setTargetValues(final Target target, final String name, final String description) {
        if (null == name) {
            target.setName(target.getControllerId());
        } else {
            target.setName(name);
        }
        target.setName(name == null ? target.getControllerId() : name);
        target.setDescription(description);
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

    private void saveAllAssignments() {
        final Set<TargetIdName> itemIds = managementUIState.getAssignedList().keySet();
        Long distId;
        List<TargetIdName> targetIdSetList;
        List<TargetIdName> tempIdList;

        final long forcedTimeStamp = new Date().getTime();
        final ActionType actionType = ActionType.FORCED;

        final Map<Long, ArrayList<TargetIdName>> saveAssignedList = new HashMap<>();
        for (final TargetIdName itemId : itemIds) {
            final DistributionSetIdName distitem = managementUIState.getAssignedList().get(itemId);
            distId = distitem.getId();

            if (saveAssignedList.containsKey(distId)) {
                targetIdSetList = saveAssignedList.get(distId);
            } else {
                targetIdSetList = new ArrayList<>();
            }
            targetIdSetList.add(itemId);
            saveAssignedList.put(distId, (ArrayList<TargetIdName>) targetIdSetList);
        }

        for (final Map.Entry<Long, ArrayList<TargetIdName>> mapEntry : saveAssignedList.entrySet()) {
            tempIdList = saveAssignedList.get(mapEntry.getKey());
            final String[] ids = tempIdList.stream().map(t -> t.getControllerId()).toArray(size -> new String[size]);
            deploymentManagement.assignDistributionSet(mapEntry.getKey(), actionType, forcedTimeStamp, ids);
        }

        managementUIState.getAssignedList().clear();
    }

    private void tagAssignment() {
        final Set<String> targetList = new HashSet<>();
        targetList.addAll(targetIdNames.stream().map(t -> t.getControllerId()).collect(Collectors.toList()));
        for (final String tagName : targetBulkTokenTags.getAssignedTagNames()) {
            targetManagement.toggleTagAssignment(targetList, tagName);
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

}
