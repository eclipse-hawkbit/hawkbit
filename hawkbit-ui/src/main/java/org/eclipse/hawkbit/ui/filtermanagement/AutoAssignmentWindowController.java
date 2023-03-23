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
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.ui.common.AbstractUpdateEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.util.StringUtils;

import static org.eclipse.hawkbit.ui.utils.UIMessageIdProvider.MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_CONF_HINT;

/**
 * Controller for auto assignment window
 */
public class AutoAssignmentWindowController extends
        AbstractUpdateEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery, TargetFilterQuery> {

    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final AutoAssignmentWindowLayout layout;
    private final TenantConfigHelper configHelper;
    private final TenantAware tenantAware;

    /**
     * Constructor for AutoAssignmentWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param layout
     *            AutoAssignmentWindowLayout
     */
    public AutoAssignmentWindowController(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final TenantConfigHelper configHelper, final TenantAware tenantAware,
            final AutoAssignmentWindowLayout layout) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.configHelper = configHelper;
        this.tenantAware = tenantAware;
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
            autoAssignmentFilter.setConfirmationRequired(proxyEntity.isConfirmationRequired());
        } else {
            autoAssignmentFilter.setAutoAssignmentEnabled(false);
            autoAssignmentFilter.setAutoAssignActionType(ActionType.FORCED);
            autoAssignmentFilter.setDistributionSetInfo(null);
            autoAssignmentFilter.setConfirmationRequired(configHelper.isConfirmationFlowEnabled());
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
        // super.persistEntity couldn't be used because of the two cases
        // store/show dialog
        if (entity.isAutoAssignmentEnabled() && entity.getDistributionSetInfo() != null) {
            final Long autoAssignDsId = entity.getDistributionSetInfo().getId();
            final Long targetsForAutoAssignmentCount = targetManagement.countByRsqlAndNonDSAndCompatible(autoAssignDsId,
                    entity.getQuery());

            final String confirmationCaption = getI18n()
                    .getMessage(UIMessageIdProvider.CAPTION_CONFIRM_AUTO_ASSIGN_CONSEQUENCES);
            final String confirmationQuestion = targetsForAutoAssignmentCount == 0
                    ? getI18n().getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_NONE)
                    : getI18n().getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_TEXT,
                            targetsForAutoAssignmentCount);

            final String conformationHint = configHelper.isConfirmationFlowEnabled() && !entity.isConfirmationRequired()
                    ? getI18n().getMessage(MESSAGE_CONFIRM_AUTO_ASSIGN_CONSEQUENCES_CONF_HINT,
                            tenantAware.getCurrentUsername())
                    : null;

            showConsequencesDialog(confirmationCaption, confirmationQuestion, conformationHint, entity.getId(), autoAssignDsId,
                    entity.getAutoAssignActionType(), entity.isConfirmationRequired());
        } else {
            final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.updateAutoAssignDS(
                    getEntityFactory().targetFilterQuery().updateAutoAssign(entity.getId()).ds(null));
            publishModifiedEvent(createModifiedEventPayload(targetFilterQuery));
        }
    }

    private void showConsequencesDialog(final String confirmationCaption, final String confirmationQuestion,
            final String confirmationHint, final Long targetFilterId, final Long autoAssignDsId,
            final ActionType autoAssignActionType, final boolean confirmationRequired) {
        final ConfirmationDialog confirmDialog = ConfirmationDialog
                .newBuilder(getI18n(), UIComponentIdProvider.DIST_SET_SELECT_CONS_WINDOW_ID)
                .caption(confirmationCaption).question(confirmationQuestion).hint(confirmationHint)
                .icon(StringUtils.hasText(confirmationHint) ? VaadinIcons.WARNING : null).onSaveOrUpdate(() -> {
                    final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.updateAutoAssignDS(
                            getEntityFactory().targetFilterQuery().updateAutoAssign(targetFilterId).ds(autoAssignDsId)
                                    .actionType(autoAssignActionType).confirmationRequired(confirmationRequired));
                    publishModifiedEvent(createModifiedEventPayload(targetFilterQuery));
                }).build();

        confirmDialog.getWindow().setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    @Override
    protected TargetFilterQuery persistEntityInRepository(final ProxyTargetFilterQuery entity) {
        // this subclass cares itself for persisting the entity because of
        // special requirements (multiple confirmation dialogs).
        return null;
    }

    @Override
    protected String getDisplayableName(final TargetFilterQuery entity) {
        return entity.getName();
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxyTargetFilterQuery entity) {
        return entity.getName();
    }

    @Override
    protected Long getId(final TargetFilterQuery entity) {
        return entity.getId();
    }

    @Override
    protected String getPersistSuccessMessageKey() {
        // this subclass cares itself for persisting the entity because of
        // special requirements (multiple confirmation dialogs)
        return null;
    }

    @Override
    protected String getPersistFailureMessageKey() {
        // this subclass cares itself for persisting the entity because of
        // special requirements (multiple confirmation dialogs)
        return null;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyTargetFilterQuery.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (entity.isAutoAssignmentEnabled()
                && (entity.getAutoAssignActionType() == null || entity.getDistributionSetInfo() == null)) {
            displayValidationError(UIMessageIdProvider.MESSAGE_AUTOASSIGN_CREATE_ERROR_MISSINGELEMENTS);
            return false;
        }

        return true;
    }
}
