/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;

/**
 * Support for invalidate a distribution set in the ds grid.
 */
public class InvalidateDistributionSetSupport {
    private static final Logger LOG = LoggerFactory.getLogger(InvalidateDistributionSetSupport.class);

    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;
    private final UINotification notification;
    private final SpPermissionChecker permissionChecker;

    private final DistributionGrid grid;

    private final DistributionSetInvalidationManagement dsInvalidationManagement;

    private InvalidateDsConsequencesDialog consequencesDialog;

    /**
     * Constructor for InvalidateDistributionSetSupport
     *
     * @param grid
     *            Vaadin Grid
     * @param i18n
     *            VaadinMessageSource
     * @param notification
     *            UINotification
     * @param dsInvalidationManagement
     *            {@link DistributionSetInvalidationManagement}
     */
    public InvalidateDistributionSetSupport(final DistributionGrid grid, final VaadinMessageSource i18n,
            final UiProperties uiProperties, final UINotification notification,
            final SpPermissionChecker permissionChecker,
            final DistributionSetInvalidationManagement dsInvalidationManagement) {
        this.grid = grid;
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.notification = notification;
        this.permissionChecker = permissionChecker;
        this.dsInvalidationManagement = dsInvalidationManagement;
    }

    /**
     * Open confirmation pop up window for invalidate distribution set
     *
     * @param clickedDistributionSet
     *            {@link ProxyDistributionSet} that should be invalidated
     */
    public void openConsequencesWindowOnInvalidateAction(final ProxyDistributionSet clickedDistributionSet) {
        final List<ProxyDistributionSet> allDistributionSetsForInvalidation = getDistributionSetsForInvalidation(
                clickedDistributionSet);

        consequencesDialog = new InvalidateDsConsequencesDialog(allDistributionSetsForInvalidation, i18n, uiProperties,
                ok -> {
                    if (Boolean.TRUE.equals(ok) && hasSufficientPermission()) {
                        openAffectedEntitiesWindowOnInvalidateAction(allDistributionSetsForInvalidation);
                    }
                });
        consequencesDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(consequencesDialog.getWindow());
        consequencesDialog.getWindow().bringToFront();
    }

    private void openAffectedEntitiesWindowOnInvalidateAction(
            final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {

        final DistributionSetInvalidationCount entitiesForInvalidationCount = dsInvalidationManagement
                .countEntitiesForInvalidation(
                        getDistributionSetInvalidation(consequencesDialog.isStopRolloutsSelected(),
                                getDistributionSetIds(allDistributionSetsForInvalidation),
                                consequencesDialog.getCancelationType()));

        final InvalidateDsAffectedEntitiesDialog affectedEntitiesDialog = new InvalidateDsAffectedEntitiesDialog(
                allDistributionSetsForInvalidation, i18n, ok -> {
                    if (Boolean.TRUE.equals(ok) && hasSufficientPermission()) {
                        handleOkForInvalidateDistributionSet(allDistributionSetsForInvalidation);
                    }
                }, entitiesForInvalidationCount);
        affectedEntitiesDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(affectedEntitiesDialog.getWindow());
        affectedEntitiesDialog.getWindow().bringToFront();
    }

    private void handleOkForInvalidateDistributionSet(
            final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        try {
            dsInvalidationManagement.invalidateDistributionSet(
                    getDistributionSetInvalidation(consequencesDialog.isStopRolloutsSelected(),
                            getDistributionSetIds(allDistributionSetsForInvalidation),
                            consequencesDialog.getCancelationType()));
            notification.displaySuccess(createSuccessNotificationText(allDistributionSetsForInvalidation));
            grid.refreshAll();
        } catch (final RuntimeException ex) {
            LOG.warn("Invalidating DistributionSets '{}' failed: {}", StringUtils.collectionToCommaDelimitedString(
                    getDistributionSetIds(allDistributionSetsForInvalidation)), ex.getMessage());
            notification.displayWarning(createFailureNotificationText(allDistributionSetsForInvalidation));
            throw ex;
        }
    }

    private boolean hasSufficientPermission() {
        if (consequencesDialog.isStopRolloutsSelected() && !permissionChecker.hasRolloutUpdatePermission()) {
            notification.displayValidationError(i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_ERROR_PERMISSION_INSUFFICIENT, SpPermission.UPDATE_ROLLOUT));
            return false;
        }
        if (consequencesDialog.getCancelationType() != CancelationType.NONE
                && !permissionChecker.hasUpdateTargetPermission()) {
            notification.displayValidationError(i18n
                    .getMessage(UIMessageIdProvider.MESSAGE_ERROR_PERMISSION_INSUFFICIENT, SpPermission.UPDATE_TARGET));
            return false;
        }
        return true;
    }

    private DistributionSetInvalidation getDistributionSetInvalidation(final boolean stopRollouts,
            final List<Long> distSetIds, final CancelationType cancelationType) {
        return new DistributionSetInvalidation(distSetIds, cancelationType, stopRollouts);
    }

    private String createFailureNotificationText(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        String failureNotificationText = "";
        if (allDistributionSetsForInvalidation.size() == 1) {
            failureNotificationText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_FAIL_SINGULAR,
                    allDistributionSetsForInvalidation.get(0).getNameVersion());
        } else {
            failureNotificationText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_FAIL_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
        return failureNotificationText;
    }

    private String createSuccessNotificationText(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        String successNotificationText = "";
        if (allDistributionSetsForInvalidation.size() == 1) {
            successNotificationText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_SUCCESS_SINGULAR,
                    allDistributionSetsForInvalidation.get(0).getNameVersion());
        } else {
            successNotificationText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_SUCCESS_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
        return successNotificationText;
    }

    private static List<Long> getDistributionSetIds(
            final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        return allDistributionSetsForInvalidation.stream().map(ProxyDistributionSet::getId)
                .collect(Collectors.toList());
    }

    private List<ProxyDistributionSet> getDistributionSetsForInvalidation(final ProxyDistributionSet clickedItem) {
        final List<ProxyDistributionSet> selectedItems = Lists.newArrayList(grid.getSelectedItems());

        if (selectedItems.contains(clickedItem)) {
            // consider only valid DS for invalidation
            return selectedItems.stream().filter(ProxyDistributionSet::getIsValid).collect(Collectors.toList());
        } else {
            // only clicked item should be invalidated if it is not part of the
            // selection
            grid.deselectAll();
            grid.select(clickedItem);

            return Collections.singletonList(clickedItem);
        }
    }
}
