/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;

/**
 * Controller for auto assignment window
 */
public class AutoAssignmentWindowController
        extends AbstractEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery> {

    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final AutoAssignmentWindowLayout layout;

    /**
     * Constructor for AutoAssignmentWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetManagement
     *            TargetManagement
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param layout
     *            AutoAssignmentWindowLayout
     */
    public AutoAssignmentWindowController(final UIConfiguration uiConfig, final TargetManagement targetManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final AutoAssignmentWindowLayout layout) {
        super(uiConfig);

        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.layout = layout;
    }

    @Override
    protected ProxyTargetFilterQuery buildEntityFromProxy(final ProxyTargetFilterQuery proxyEntity) {
        final ProxyTargetFilterQuery autoAssignmentFilter = new ProxyTargetFilterQuery();

        autoAssignmentFilter.setId(proxyEntity.getId());
        autoAssignmentFilter.setQuery(proxyEntity.getQuery());

        if (proxyEntity.getDistributionSetInfo() != null) {
            autoAssignmentFilter.setAutoAssignmentEnabled(true);
            autoAssignmentFilter.setAutoAssignActionType(proxyEntity.getAutoAssignActionType());
            autoAssignmentFilter.setDistributionSetInfo(proxyEntity.getDistributionSetInfo());
        } else {
            autoAssignmentFilter.setAutoAssignmentEnabled(false);
            autoAssignmentFilter.setAutoAssignActionType(ActionType.FORCED);
            autoAssignmentFilter.setDistributionSetInfo(null);
        }

        return autoAssignmentFilter;
    }

    @Override
    public EntityWindowLayout<ProxyTargetFilterQuery> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyTargetFilterQuery proxyEntity) {
        layout.switchAutoAssignmentInputsVisibility(layout.getEntity().isAutoAssignmentEnabled());
    }

    @Override
    protected void persistEntity(final ProxyTargetFilterQuery entity) {
        if (entity.isAutoAssignmentEnabled() && entity.getDistributionSetInfo() != null) {
            final Long autoAssignDsId = entity.getDistributionSetInfo().getId();
            final Long targetsForAutoAssignmentCount = targetManagement.countByRsqlAndNonDS(autoAssignDsId,
                    entity.getQuery());

            final String confirmationCaption = getI18n()
                    .getMessage(UIMessageIdProvider.CAPTION_CONFIRM_AUTO_ASSIGN_CONSEQUENCES);
            final String confirmationQuestion = targetsForAutoAssignmentCount == 0
                    ? getI18n().getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_NONE)
                    : getI18n().getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_TEXT,
                            targetsForAutoAssignmentCount);

            showConsequencesDialog(confirmationCaption, confirmationQuestion, entity.getId(), autoAssignDsId,
                    entity.getAutoAssignActionType(), entity);
        } else {
            targetFilterQueryManagement
                    .updateAutoAssignDS(getEntityFactory().targetFilterQuery().updateAutoAssign(entity.getId()).ds(null));
            publishModifiedEvent(entity.getId());
        }
    }

    private void showConsequencesDialog(final String confirmationCaption, final String confirmationQuestion,
            final Long targetFilterId, final Long autoAssignDsId, final ActionType autoAssignActionType,
            final ProxyTargetFilterQuery entity) {
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(getI18n(), confirmationCaption, confirmationQuestion,
                ok -> {
                    if (ok) {
                        targetFilterQueryManagement.updateAutoAssignDS(getEntityFactory().targetFilterQuery()
                                .updateAutoAssign(targetFilterId).ds(autoAssignDsId).actionType(autoAssignActionType));
                        publishModifiedEvent(entity.getId());
                    }
                }, UIComponentIdProvider.DIST_SET_SELECT_CONS_WINDOW_ID);

        confirmDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void publishModifiedEvent(final Long entityId) {
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyTargetFilterQuery.class, entityId));
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (entity.isAutoAssignmentEnabled()
                && (entity.getAutoAssignActionType() == null || entity.getDistributionSetInfo() == null)) {
            getUiNotification().displayValidationError(
                    getI18n().getMessage(UIMessageIdProvider.MESSAGE_AUTOASSIGN_CREATE_ERROR_MISSINGELEMENTS));
            return false;
        }

        return true;
    }
}
