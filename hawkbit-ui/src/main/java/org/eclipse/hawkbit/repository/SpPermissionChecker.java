/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.Serializable;

import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Bean which contains all SP permissions.
 *
 *
 *
 *
 */
@Validated
@Service
public class SpPermissionChecker implements Serializable {
    private static final long serialVersionUID = 2757865286212875704L;

    @Autowired
    private transient PermissionService permissionService;

    /**
     * Gets the SP monitor View Permission.
     * 
     * @return SYSTEM_MONITOR boolean value
     */
    public boolean hasSpMonitorViewPermission() {
        return permissionService.hasPermission(SpPermission.SYSTEM_MONITOR);
    }

    /**
     * Gets the SP diagnosis retrieval Permission.
     * 
     * @return SYSTEM_DIAG boolean value
     */
    public boolean hasSpdiagnosisViewPermission() {
        return permissionService.hasPermission(SpPermission.SYSTEM_DIAG);
    }

    /**
     * Gets the SP administration retrieval Permission.
     * 
     * @return SYSTEM_ADMIN boolean value
     */
    public boolean hasSpAdminViewPermission() {
        return permissionService.hasPermission(SpPermission.SYSTEM_ADMIN);
    }

    /**
     * Gets the SP read Target & Dist Permission.
     * 
     * @return TARGET_REPOSITORY_READ boolean value
     */
    public boolean hasTargetAndRepositoryReadPermission() {
        return hasTargetReadPermission() && hasReadDistributionPermission();
    }

    /**
     * Gets the SP read Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasTargetReadPermission() {
        return permissionService.hasPermission(SpPermission.READ_TARGET);
    }

    /**
     * Gets the SP create Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasCreateTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.CREATE_TARGET);
    }

    /**
     * Gets the SP update Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasUpdateTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.UPDATE_TARGET);
    }

    /**
     * Gets the SP delete Target Permission.
     * 
     * @return READ_TARGET boolean value
     */
    public boolean hasDeleteTargetPermission() {
        return hasTargetReadPermission() && permissionService.hasPermission(SpPermission.DELETE_TARGET);
    }

    /**
     * Gets the SP READ Distribution Permission.
     * 
     * @return READ_REPOSITORY boolean value
     */
    public boolean hasReadDistributionPermission() {
        return permissionService.hasPermission(SpPermission.READ_REPOSITORY);
    }

    /**
     * Gets the SP create Distribution Permission.
     * 
     * @return CREATE_REPOSITORY boolean value
     */
    public boolean hasCreateDistributionPermission() {
        return hasReadDistributionPermission() && permissionService.hasPermission(SpPermission.CREATE_REPOSITORY);
    }

    /**
     * Gets the SP update Distribution Permission.
     * 
     * @return UPDATE_REPOSITORY boolean value
     */
    public boolean hasUpdateDistributionPermission() {
        return hasReadDistributionPermission() && permissionService.hasPermission(SpPermission.UPDATE_REPOSITORY);
    }

    /**
     * Gets the SP delete Distribution Permission.
     * 
     * @return DELETE_REPOSITORY boolean value
     */
    public boolean hasDeleteDistributionPermission() {
        return hasReadDistributionPermission() && permissionService.hasPermission(SpPermission.DELETE_REPOSITORY);
    }

}
