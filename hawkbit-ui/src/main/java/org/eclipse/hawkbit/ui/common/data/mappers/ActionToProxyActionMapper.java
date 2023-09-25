/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction.IsActiveDecoration;

/**
 * Maps {@link Action} entities, fetched from backend, to the
 * {@link ProxyAction} entities.
 */
public class ActionToProxyActionMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyAction, Action> {

    @Override
    public ProxyAction map(final Action action) {
        final ProxyAction proxyAction = new ProxyAction();

        proxyAction.setId(action.getId());
        final String dsNameVersion = action.getDistributionSet().getName() + ":"
                + action.getDistributionSet().getVersion();
        proxyAction.setDsNameVersion(dsNameVersion);
        proxyAction.setActionType(action.getActionType());
        proxyAction.setActive(action.isActive());
        proxyAction.setIsActiveDecoration(buildIsActiveDecoration(action));
        proxyAction.setLastModifiedAt(action.getLastModifiedAt());
        proxyAction.setRolloutName(action.getRollout() != null ? action.getRollout().getName() : "");
        proxyAction.setStatus(action.getStatus());
        proxyAction
                .setMaintenanceWindow(action.hasMaintenanceSchedule() ? buildMaintenanceWindowDisplayText(action) : "");
        proxyAction.setMaintenanceWindowStartTime(action.getMaintenanceWindowStartTime().orElse(null));
        proxyAction.setForcedTime(action.getForcedTime());

        return proxyAction;
    }

    /**
     * Generates a virtual IsActiveDecoration for the presentation layer that is
     * calculated from {@link Action#isActive()} and
     * {@link Action#getActionStatus()}.
     *
     * @param action
     *            the action combined IsActiveDecoration is calculated from
     * @return IsActiveDecoration combined decoration for the presentation
     *         layer.
     */
    private static IsActiveDecoration buildIsActiveDecoration(final Action action) {
        final Action.Status status = action.getStatus();

        if (status == Action.Status.SCHEDULED) {
            return IsActiveDecoration.SCHEDULED;
        } else if (status == Action.Status.ERROR) {
            return IsActiveDecoration.IN_ACTIVE_ERROR;
        }

        return action.isActive() ? IsActiveDecoration.ACTIVE : IsActiveDecoration.IN_ACTIVE;
    }

    private static String buildMaintenanceWindowDisplayText(final Action action) {
        return action.getMaintenanceWindowSchedule() + "/" + action.getMaintenanceWindowDuration() + "/"
                + action.getMaintenanceWindowTimeZone();
    }

}
