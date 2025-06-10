/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtCancelationType;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtRestModelMapper {

    /**
     * Convert the given {@link MgmtActionType} into a corresponding repository {@link ActionType}.
     *
     * @param actionTypeRest the REST representation of the action type
     * @return <null> or the repository action type
     */
    public static ActionType convertActionType(final MgmtActionType actionTypeRest) {
        if (actionTypeRest == null) {
            return null;
        }

        return switch (actionTypeRest) {
            case SOFT -> ActionType.SOFT;
            case FORCED -> ActionType.FORCED;
            case TIMEFORCED -> ActionType.TIMEFORCED;
            case DOWNLOAD_ONLY -> ActionType.DOWNLOAD_ONLY;
        };
    }

    /**
     * Converts the given repository {@link ActionType} into a corresponding {@link MgmtActionType}.
     *
     * @param actionType the repository representation of the action type
     * @return <null> or the REST action type
     */
    public static MgmtActionType convertActionType(final ActionType actionType) {
        if (actionType == null) {
            return null;
        }

        return switch (actionType) {
            case SOFT -> MgmtActionType.SOFT;
            case FORCED -> MgmtActionType.FORCED;
            case TIMEFORCED -> MgmtActionType.TIMEFORCED;
            case DOWNLOAD_ONLY -> MgmtActionType.DOWNLOAD_ONLY;
        };
    }

    /**
     * Converts the given repository {@link CancelationType} into a corresponding {@link MgmtCancelationType}.
     *
     * @param cancelationType the repository representation of the cancellation type
     * @return <null> or the REST cancellation type
     */
    public static CancelationType convertCancelationType(final MgmtCancelationType cancelationType) {
        if (cancelationType == null) {
            return null;
        }

        return switch (cancelationType) {
            case SOFT -> CancelationType.SOFT;
            case FORCE -> CancelationType.FORCE;
            case NONE -> CancelationType.NONE;
        };
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

    static void mapTypeToType(final MgmtTypeEntity response, final Type base) {
        mapNamedToNamed(response, base);

        response.setKey(base.getKey());
        response.setColour(base.getColour());
        response.setDeleted(base.isDeleted());
    }
}