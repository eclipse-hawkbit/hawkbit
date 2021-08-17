/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;

/**
 * Support for invalidate a distribution set in the ds grid.
 */
public class InvalidateDistributionSetSupport {
    private static final Logger LOG = LoggerFactory.getLogger(InvalidateDistributionSetSupport.class);

    private final VaadinMessageSource i18n;
    private final UINotification notification;
    private final DistributionGrid grid;

    private final DistributionSetManagement dsManagement;
    private final RolloutManagement rolloutManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;

    private InvalidateDsConsequencesDialog consequencesDialog;
    private InvalidateDsAffectedEntitiesDialog affectedEntitiesDialog;

    /**
     * Constructor for InvalidateDistributionSetSupport
     *
     * @param grid
     *            Vaadin Grid
     * @param i18n
     *            VaadinMessageSource
     * @param notification
     *            UINotification
     * @param dsManagement
     *            {@link DistributionSetManagement}
     * @param rolloutManagement
     *            {@link RolloutManagement}
     * @param targetFilterQueryManagement
     *            {@link TargetFilterQueryManagement}
     */
    public InvalidateDistributionSetSupport(final DistributionGrid grid, final VaadinMessageSource i18n,
            final UINotification notification, final DistributionSetManagement dsManagement,
            final RolloutManagement rolloutManagement, final TargetFilterQueryManagement targetFilterQueryManagement) {
        this.grid = grid;
        this.i18n = i18n;
        this.notification = notification;
        this.dsManagement = dsManagement;
        this.rolloutManagement = rolloutManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
    }

    /**
     * Open confirmation pop up window for invalidate distribution set
     *
     * @param distributionSet
     *            {@link ProxyDistributionSet} that should be invalidated
     */
    public void openConsequencesWindowOnInvalidateAction(final ProxyDistributionSet distributionSet) {

        consequencesDialog = new InvalidateDsConsequencesDialog(distributionSet, i18n, ok -> {
            if (ok) {
                openAffectedEntitiesWindowOnInvalidateAction(distributionSet);
            }
        });
        consequencesDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(consequencesDialog.getWindow());
        consequencesDialog.getWindow().bringToFront();
    }

    private void openAffectedEntitiesWindowOnInvalidateAction(final ProxyDistributionSet distributionSet) {
        final int affectedRolloutsByDSInvalidation = getAffectedRolloutsByDSInvalidation(distributionSet);
        final int affectedAutoAssignmentsByDSInvalidation = getAffectedAutoAssignmentsByDSInvalidation(distributionSet);

        affectedEntitiesDialog = new InvalidateDsAffectedEntitiesDialog(distributionSet, i18n, ok -> {
            if (ok) {
                handleOkForInvalidateDistributionSet(distributionSet,
                        i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_SUCCESS,
                                distributionSet.getName()),
                        i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_FAIL,
                                distributionSet.getName()));
            }
        }, affectedRolloutsByDSInvalidation, affectedAutoAssignmentsByDSInvalidation);
        affectedEntitiesDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(affectedEntitiesDialog.getWindow());
        affectedEntitiesDialog.getWindow().bringToFront();
    }

    private void handleOkForInvalidateDistributionSet(final ProxyDistributionSet distributionSet,
            final String successNotificationText, final String failureNotificationText) {

        try {
            // TODO use boolean flag in repo call
            final boolean stopRollouts = consequencesDialog.getStopRollouts();
            dsManagement.invalidate(distributionSet.getId());
            notification.displaySuccess(successNotificationText);
            grid.refreshItem(distributionSet);
        } catch (final RuntimeException ex) {
            LOG.warn("Invalidating DistributionSet '{}' failed: {}", distributionSet.getName(), ex.getMessage());
            notification.displayWarning(failureNotificationText);
            throw ex;
        }
    }

    private int getAffectedRolloutsByDSInvalidation(final ProxyDistributionSet distributionSet) {
        // TODO adapt
        return 0;
    }

    private int getAffectedAutoAssignmentsByDSInvalidation(final ProxyDistributionSet distributionSet) {
        // TODO adapt
        return 0;
    }

    /**
     * Gets the style of an invalid {@link DistributionSet}
     *
     * @param itemId
     *            Id of item
     *
     * @return row style
     */
    public static String getInvalidDistributionSetRowStyle(final ProxyDistributionSet distributionSet) {
        if (distributionSet == null || distributionSet.getIsValid()) {
            return null;
        }

        return SPUIDefinitions.INVALID_DISTRIBUTION;
    }
}
