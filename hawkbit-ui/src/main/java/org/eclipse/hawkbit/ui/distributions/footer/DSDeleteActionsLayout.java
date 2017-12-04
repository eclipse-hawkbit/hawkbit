/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.footer;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;

/**
 * Distributions footer layout implementation.
 */
public class DSDeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = 3494052985006132714L;

    private final transient SystemManagement systemManagement;
    private final transient DistributionSetManagement distributionSetManagement;

    private final ManageDistUIState manageDistUIState;

    private final DistributionsConfirmationWindowLayout distConfirmationWindowLayout;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    public DSDeleteActionsLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final UINotification notification, final SystemManagement systemManagement,
            final ManageDistUIState manageDistUIState,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final DistributionSetManagement distributionSetManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, permChecker, eventBus, notification);
        this.systemManagement = systemManagement;
        this.manageDistUIState = manageDistUIState;
        this.distConfirmationWindowLayout = new DistributionsConfirmationWindowLayout(i18n, eventBus,
                distributionSetManagement, distributionSetTypeManagement, softwareModuleManagement,
                softwareModuleTypeManagement, manageDistUIState);
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
        this.distributionSetManagement = distributionSetManagement;
        init();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event != null) {
            UI.getCurrent().access(() -> {
                if (!hasUnsavedActions()) {
                    closeUnsavedActionsWindow();
                    final String message = distConfirmationWindowLayout.getConsolidatedMessage();
                    if (message != null && message.length() > 0) {
                        notification.displaySuccess(message);
                    }
                }
                updateDSActionCount();
            });
        }
    }

    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteRepositoryPermission();

    }

    @Override
    protected boolean hasUpdatePermission() {

        return permChecker.hasUpdateRepositoryPermission();
    }

    @Override
    protected String getDeleteAreaLabel() {
        return i18n.getMessage("label.components.drop.area");
    }

    @Override
    protected String getDeleteAreaId() {

        return UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID;
    }

    @Override
    protected AcceptCriterion getDeleteLayoutAcceptCriteria() {
        return distributionsViewClientCriterion;
    }

    @Override
    protected void processDroppedComponent(final DragAndDropEvent event) {
        final Component sourceComponent = event.getTransferable().getSourceComponent();
        if (sourceComponent instanceof Table) {
            final Table sourceTable = (Table) event.getTransferable().getSourceComponent();
            if (isDistributionTable(sourceTable)) {
                addInDeleteDistributionList(sourceTable, (TableTransferable) event.getTransferable());
                updateDSActionCount();
            } else if (isSoftwareModuleTable(sourceTable)) {
                addToSWDeleteList(sourceTable, (TableTransferable) event.getTransferable());
                updateDSActionCount();
            }
        } else if (sourceComponent.getId().startsWith(SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS)) {
            processDeleteDitSetType(sourceComponent.getId());
        } else if (sourceComponent.getId().startsWith(SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS)) {
            processDeleteSWType(sourceComponent.getId());

        }

    }

    private void processDeleteDitSetType(final String distTypeId) {
        final String distTypeName = HawkbitCommonUtil.removePrefix(distTypeId,
                SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS);
        if (isDsTypeSelected(distTypeName)) {
            notification.displayValidationError(
                    i18n.getMessage("message.dist.type.check.delete", new Object[] { distTypeName }));
        } else if (isDefaultDsType(distTypeName)) {
            notification.displayValidationError(i18n.getMessage("message.cannot.delete.default.dstype"));
        } else {
            manageDistUIState.getSelectedDeleteDistSetTypes().add(distTypeName);
            updateDSActionCount();
        }
    }

    /**
     * Check if distribution set type is selected.
     *
     * @param distTypeName
     * @return true if ds type is selected
     */
    private boolean isDsTypeSelected(final String distTypeName) {
        return null != manageDistUIState.getManageDistFilters().getClickedDistSetType() && manageDistUIState
                .getManageDistFilters().getClickedDistSetType().getName().equalsIgnoreCase(distTypeName);
    }

    private void processDeleteSWType(final String swTypeId) {
        final String swModuleTypeName = HawkbitCommonUtil.removePrefix(swTypeId,
                SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS);

        if (manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
                .map(type -> type.getName().equalsIgnoreCase(swModuleTypeName)).orElse(false)) {
            notification.displayValidationError(
                    i18n.getMessage("message.swmodule.type.check.delete", new Object[] { swModuleTypeName }));
        } else {
            manageDistUIState.getSelectedDeleteSWModuleTypes().add(swModuleTypeName);
            updateDSActionCount();
        }

    }

    private void addInDeleteDistributionList(final Table sourceTable, final TableTransferable transferable) {
        final AbstractTable<?> table = (AbstractTable<?>) sourceTable;
        final Set<Long> ids = table.getDeletedEntityByTransferable(transferable);
        final List<DistributionSet> findDistributionSetAllById = distributionSetManagement
                .get(ids);

        if (findDistributionSetAllById.isEmpty()) {
            notification.displayWarning(i18n.getMessage("distributionsets.not.exists"));
            return;
        }

        final Set<DistributionSetIdName> distributionIdNameSet = findDistributionSetAllById.stream()
                .map(DistributionSetIdName::new).collect(Collectors.toSet());

        final int existingDeletedDistributionsSize = manageDistUIState.getDeletedDistributionList().size();
        manageDistUIState.getDeletedDistributionList().addAll(distributionIdNameSet);
        final int newDeletedDistributionsSize = manageDistUIState.getDeletedDistributionList().size();
        if (newDeletedDistributionsSize == existingDeletedDistributionsSize) {
            notification.displayValidationError(i18n.getMessage("message.targets.already.deleted"));
        } else if (newDeletedDistributionsSize - existingDeletedDistributionsSize != distributionIdNameSet.size()) {
            notification.displayValidationError(i18n.getMessage("message.dist.deleted.pending"));
        }

    }

    private void addToSWDeleteList(final Table sourceTable, final TableTransferable transferable) {
        final AbstractTable<?> swTable = (AbstractTable<?>) sourceTable;
        final Set<Long> swModuleIdNameSet = swTable.getDeletedEntityByTransferable(transferable);

        swModuleIdNameSet.forEach(id -> {
            final String swModuleName = (String) sourceTable.getContainerDataSource().getItem(id)
                    .getItemProperty(SPUILabelDefinitions.NAME_VERSION).getValue();
            manageDistUIState.getDeleteSofwareModulesList().put(id, swModuleName);
        });
    }

    /**
     * Update the software module delete count.
     */
    private void updateDSActionCount() {
        int count = manageDistUIState.getSelectedDeleteDistSetTypes().size()
                + manageDistUIState.getSelectedDeleteSWModuleTypes().size()
                + manageDistUIState.getDeleteSofwareModulesList().size()
                + manageDistUIState.getDeletedDistributionList().size();

        for (final Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> mapEntry : manageDistUIState
                .getAssignedList().entrySet()) {
            count += manageDistUIState.getAssignedList().get(mapEntry.getKey()).size();
        }
        updateActionsCount(count);
    }

    /**
     * DistributionsUIEvent.
     *
     * @param event
     *            as instance of {@link DistributionsUIEvent}
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.UPDATE_COUNT) {
            updateDSActionCount();
        }

    }

    private boolean isDistributionTable(final Component source) {
        return UIComponentIdProvider.DIST_TABLE_ID.equals(source.getId());
    }

    private boolean isSoftwareModuleTable(final Component source) {
        return UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE.equals(source.getId());
    }

    @Override
    protected void restoreActionCount() {
        updateDSActionCount();

    }

    @Override
    protected void unsavedActionsWindowClosed() {
        final String message = distConfirmationWindowLayout.getConsolidatedMessage();
        if (message != null && message.length() > 0) {
            notification.displaySuccess(message);
        }

    }

    @Override
    protected Component getUnsavedActionsWindowContent() {
        distConfirmationWindowLayout.initialize();
        return distConfirmationWindowLayout;
    }

    @Override
    protected boolean hasUnsavedActions() {
        boolean unSavedActionsTypes = false;
        boolean unSavedActionsTables = false;
        if (!manageDistUIState.getSelectedDeleteDistSetTypes().isEmpty()
                || !manageDistUIState.getSelectedDeleteSWModuleTypes().isEmpty()) {
            unSavedActionsTypes = true;
        } else if (!manageDistUIState.getDeleteSofwareModulesList().isEmpty()
                || !manageDistUIState.getDeletedDistributionList().isEmpty()
                || !manageDistUIState.getAssignedList().isEmpty()) {
            unSavedActionsTables = true;
        }

        return unSavedActionsTables || unSavedActionsTypes;
    }

    private DistributionSetType getCurrentDistributionSetType() {
        return systemManagement.getTenantMetadata().getDefaultDsType();
    }

    /**
     * Check if the distribution set type is default.
     *
     * @param dsTypeName
     *            distribution set name
     * @return true if distribution set type is set default in configuration
     */
    private boolean isDefaultDsType(final String dsTypeName) {
        return getCurrentDistributionSetType() != null && getCurrentDistributionSetType().getName().equals(dsTypeName);
    }

}
