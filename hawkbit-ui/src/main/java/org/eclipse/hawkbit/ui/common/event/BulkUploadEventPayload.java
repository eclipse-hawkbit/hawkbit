/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Payload event for bulk upload
 */
public final class BulkUploadEventPayload {
    private final BulkUploadState bulkUploadState;
    private final float bulkUploadProgress;
    private final int successBulkUploadCount;
    private final int failBulkUploadCount;
    private final String failureReason;

    private BulkUploadEventPayload(final BulkUploadState bulkUploadState) {
        this(bulkUploadState, "");
    }

    private BulkUploadEventPayload(final BulkUploadState bulkUploadState, final String failureReason) {
        this(bulkUploadState, 0, 0, 0, failureReason);
    }

    private BulkUploadEventPayload(final BulkUploadState bulkUploadState, final float bulkUploadProgress) {
        this(bulkUploadState, bulkUploadProgress, 0, 0);
    }

    private BulkUploadEventPayload(final BulkUploadState bulkUploadState, final float bulkUploadProgress,
            final int successBulkUploadCount, final int failBulkUploadCount) {
        this(bulkUploadState, bulkUploadProgress, successBulkUploadCount, failBulkUploadCount, "");
    }

    private BulkUploadEventPayload(final BulkUploadState bulkUploadState, final float bulkUploadProgress,
            final int successBulkUploadCount, final int failBulkUploadCount, final String failureReason) {
        this.bulkUploadState = bulkUploadState;
        this.bulkUploadProgress = bulkUploadProgress;
        this.successBulkUploadCount = successBulkUploadCount;
        this.failBulkUploadCount = failBulkUploadCount;
        this.failureReason = failureReason;
    }

    /**
     * Build bulk upload started
     *
     * @return Bulk upload started state
     */
    public static BulkUploadEventPayload buildUploadStarted() {
        return new BulkUploadEventPayload(BulkUploadState.UPLOAD_STARTED);
    }

    /**
     * Build bulk upload failed
     *
     * @param failureReason
     *            the reason for the failed upload
     * @return Bulk upload failed state
     */
    public static BulkUploadEventPayload buildUploadFailed(final String failureReason) {
        return new BulkUploadEventPayload(BulkUploadState.UPLOAD_FAILED, failureReason);
    }

    /**
     * Build target provisioning started
     *
     * @return Bulk upload target provisioning started state
     */
    public static BulkUploadEventPayload buildTargetProvisioningStarted() {
        return new BulkUploadEventPayload(BulkUploadState.TARGET_PROVISIONING_STARTED);
    }

    /**
     * Build target provisioning progress updated
     *
     * @param progress
     *            the progress of the bulk upload
     * @return Bulk upload target provisioning updated state
     */
    public static BulkUploadEventPayload buildTargetProvisioningProgressUpdated(final float progress) {
        return new BulkUploadEventPayload(BulkUploadState.TARGET_PROVISIONING_PROGRESS_UPDATED, progress);
    }

    /**
     * Build tags and distribution set assignment started
     *
     * @return Bulk upload tags and distribution set assignment started state
     */
    public static BulkUploadEventPayload buildTagsAndDsAssignmentStarted() {
        return new BulkUploadEventPayload(BulkUploadState.TAGS_AND_DS_ASSIGNMENT_STARTED);
    }

    /**
     * Build tags and distribution set assignment failed
     *
     * @param failureReason
     *            the reason for the failed tags or DS assignment
     * @return Bulk upload tags and distribution set assignment failed state
     */
    public static BulkUploadEventPayload buildTagsAndDsAssignmentFailed(final String failureReason) {
        return new BulkUploadEventPayload(BulkUploadState.TAGS_AND_DS_ASSIGNMENT_FAILED, failureReason);
    }

    /**
     * Build bulk upload completed
     * 
     * @param successBulkUploadCount
     *            amount of successfully uploaded targets
     * @param failBulkUploadCount
     *            amount of failed uploaded targets
     * @return Bulk upload completed state
     */
    public static BulkUploadEventPayload buildBulkUploadCompleted(final int successBulkUploadCount,
            final int failBulkUploadCount) {
        return new BulkUploadEventPayload(BulkUploadState.BULK_UPLOAD_COMPLETED, 1, successBulkUploadCount,
                failBulkUploadCount);
    }

    /**
     * @return Current state of bulk upload
     */
    public BulkUploadState getBulkUploadState() {
        return bulkUploadState;
    }

    /**
     * @return Current progress of Bulk upload
     */
    public float getBulkUploadProgress() {
        return bulkUploadProgress;
    }

    /**
     * @return Count of succeeded bulk upload
     */
    public int getSuccessBulkUploadCount() {
        return successBulkUploadCount;
    }

    /**
     * @return Count of failed bulk upload
     */
    public int getFailBulkUploadCount() {
        return failBulkUploadCount;
    }

    /**
     * @return Reason for failed bulk upload
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Bulk upload states
     */
    public enum BulkUploadState {
        UPLOAD_STARTED, UPLOAD_FAILED, TARGET_PROVISIONING_STARTED, TARGET_PROVISIONING_PROGRESS_UPDATED, TAGS_AND_DS_ASSIGNMENT_STARTED, TAGS_AND_DS_ASSIGNMENT_FAILED, BULK_UPLOAD_COMPLETED;
    }
}
