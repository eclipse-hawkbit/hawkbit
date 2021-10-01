/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtCancelationType;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtRestModelMapper {

    // private constructor, utility class
    private MgmtRestModelMapper() {

    }

    static void mapBaseToBase(final MgmtBaseEntity response, final TenantAwareBaseEntity base) {
        response.setCreatedBy(base.getCreatedBy());
        response.setLastModifiedBy(base.getLastModifiedBy());
        if (base.getCreatedAt() > 0) {
            response.setCreatedAt(base.getCreatedAt());
        }
        if (base.getLastModifiedAt() > 0) {
            response.setLastModifiedAt(base.getLastModifiedAt());
        }
    }

    static void mapNamedToNamed(final MgmtNamedEntity response, final NamedEntity base) {
        mapBaseToBase(response, base);

        response.setName(base.getName());
        response.setDescription(base.getDescription());
    }

    /**
     * Convert the given {@link MgmtActionType} into a corresponding repository
     * {@link ActionType}.
     *
     * @param actionTypeRest
     *            the REST representation of the action type
     *
     * @return <null> or the repository action type
     */
    public static ActionType convertActionType(final MgmtActionType actionTypeRest) {
        if (actionTypeRest == null) {
            return null;
        }

        switch (actionTypeRest) {
        case SOFT:
            return ActionType.SOFT;
        case FORCED:
            return ActionType.FORCED;
        case TIMEFORCED:
            return ActionType.TIMEFORCED;
        case DOWNLOAD_ONLY:
            return ActionType.DOWNLOAD_ONLY;
        default:
            throw new IllegalStateException("Action Type is not supported");
        }
    }

    /**
     * Converts the given repository {@link ActionType} into a corresponding
     * {@link MgmtActionType}.
     *
     * @param actionType
     *            the repository representation of the action type
     *
     * @return <null> or the REST action type
     */
    public static MgmtActionType convertActionType(final ActionType actionType) {
        if (actionType == null) {
            return null;
        }

        switch (actionType) {
        case SOFT:
            return MgmtActionType.SOFT;
        case FORCED:
            return MgmtActionType.FORCED;
        case TIMEFORCED:
            return MgmtActionType.TIMEFORCED;
        case DOWNLOAD_ONLY:
            return MgmtActionType.DOWNLOAD_ONLY;
        default:
            throw new IllegalStateException("Action Type is not supported");
        }
    }

    /**
     * Converts the given repository {@link CancelationType} into a
     * corresponding {@link MgmtCancelationType}.
     *
     * @param cancelationType
     *            the repository representation of the cancellation type
     *
     * @return <null> or the REST cancellation type
     */
    public static CancelationType convertCancelationType(final MgmtCancelationType cancelationType) {
        if (cancelationType == null) {
            return null;
        }

        switch (cancelationType) {
        case SOFT:
            return CancelationType.SOFT;
        case FORCE:
            return CancelationType.FORCE;
        case NONE:
            return CancelationType.NONE;
        default:
            throw new IllegalStateException("Action Cancelation Type is not supported");
        }
    }
}
