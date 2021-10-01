/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.io.Serializable;

import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.im.authentication.SpPermission;

/**
 * Bean which contains all permissions.
 *
 */
public class SpPermissionChecker implements Serializable {
    private static final long serialVersionUID = 1L;

    protected transient PermissionService permissionService;

    protected SpPermissionChecker(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Gets the read Target and repository Permission.
     * 
     * @return TARGET_REPOSITORY_READ boolean value
     */
    public boolean hasTargetAndRepositoryReadPermission() {
        return hasTargetReadPermission() && hasReadRepositoryPermission();
    }

    /**
     * Gets the read Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasTargetReadPermission() {
        return permissionService.hasPermission(SpPermission.READ_TARGET);
    }

    /**
     * Gets the create Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasCreateTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.CREATE_TARGET);
    }

    /**
     * Gets the update Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasUpdateTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.UPDATE_TARGET);
    }

    /**
     * Gets the delete Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasDeleteTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.DELETE_TARGET);
    }

    /**
     * Gets the READ Repository Permission.
     * 
     * @return READ_REPOSITORY boolean value
     */
    public boolean hasReadRepositoryPermission() {
        return permissionService.hasPermission(SpPermission.READ_REPOSITORY);
    }

    /**
     * Has the download Repository Artifact Permission.
     * 
     * @return DOWNLOAD_REPOSITORY_ARTIFACT boolean value
     */
    public boolean hasDownloadRepositoryPermission() {
        return permissionService.hasPermission(SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT);
    }

    /**
     * Gets the create Repository Permission.
     * 
     * @return CREATE_REPOSITORY boolean value
     */
    public boolean hasCreateRepositoryPermission() {
        return hasReadRepositoryPermission() && permissionService.hasPermission(SpPermission.CREATE_REPOSITORY);
    }

    /**
     * Gets the update Repository Permission.
     * 
     * @return UPDATE_REPOSITORY boolean value
     */
    public boolean hasUpdateRepositoryPermission() {
        return hasReadRepositoryPermission() && permissionService.hasPermission(SpPermission.UPDATE_REPOSITORY);
    }

    /**
     * Has the delete Repository Permission.
     * 
     * @return DELETE_REPOSITORY boolean value
     */
    public boolean hasDeleteRepositoryPermission() {
        return hasReadRepositoryPermission() && permissionService.hasPermission(SpPermission.DELETE_REPOSITORY);
    }

    /**
     * Has the rollout update permission.
     * 
     * @return permission for rollout update
     */
    public boolean hasRolloutUpdatePermission() {
        return hasRolloutReadPermission() && permissionService.hasPermission(SpPermission.UPDATE_ROLLOUT);
    }

    /**
     * @return <code>true</code> if rollout create permission
     */
    public boolean hasRolloutCreatePermission() {
        return hasTargetReadPermission() && hasReadRepositoryPermission()
                && permissionService.hasPermission(SpPermission.CREATE_ROLLOUT);
    }

    /**
     * @return <code>true</code> if rollout read permission
     */
    public boolean hasRolloutReadPermission() {
        return permissionService.hasPermission(SpPermission.READ_ROLLOUT);
    }

    /**
     * @return <code>true</code> if rollout delete permission
     */
    public boolean hasRolloutDeletePermission() {
        return hasRolloutReadPermission() && permissionService.hasPermission(SpPermission.DELETE_ROLLOUT);
    }

    /**
     * @return <code>true</code> if rollout handle permission.
     */
    public boolean hasRolloutHandlePermission() {
        return hasRolloutReadPermission() && permissionService.hasPermission(SpPermission.HANDLE_ROLLOUT);
    }

    /**
     * @return permission to read rollout targets
     */
    public boolean hasRolloutTargetsReadPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.READ_ROLLOUT);
    }

    /**
     *
     * @return <code>true</code> if rollout can be approved by the user.
     */
    public boolean hasRolloutApprovalPermission() {
        return hasRolloutReadPermission() && permissionService.hasPermission(SpPermission.APPROVE_ROLLOUT);
    }

    /**
     *
     * @return <code>true</code> if auto assignment can be added/updated to target filter
     */
    public boolean hasAutoAssignmentUpdatePermission() {
        return hasUpdateTargetPermission() && hasReadRepositoryPermission();
    }

    /**
     *
     * @return <code>true</code> if default invalidation of distribution set is
     *         allowed
     */
    public boolean hasDistributionSetInvalidatePermission() {
        return hasUpdateRepositoryPermission() && hasUpdateTargetPermission();
    }

}
