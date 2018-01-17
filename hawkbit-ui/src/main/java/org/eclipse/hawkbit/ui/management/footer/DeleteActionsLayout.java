/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.BulkUploadPopupEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;

/**
 * Layout for the action button footer on the Deployment View.
 */
public class DeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    private final transient TargetTagManagement targetTagManagement;

    private final ManagementViewClientCriterion managementViewClientCriterion;

    private final ManagementUIState managementUIState;

    private final ManangementConfirmationWindowLayout manangementConfirmationWindowLayout;

    private final CountMessageLabel countMessageLabel;

    private final transient TargetManagement targetManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    public DeleteActionsLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final UINotification notification, final TargetTagManagement targetTagManagement,
            final DistributionSetTagManagement distributionSetTagManagement,
            final ManagementViewClientCriterion managementViewClientCriterion,
            final ManagementUIState managementUIState, final TargetManagement targetManagement,
            final TargetTable targetTable, final DeploymentManagement deploymentManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(i18n, permChecker, eventBus, notification);
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.targetTagManagement = targetTagManagement;
        this.managementViewClientCriterion = managementViewClientCriterion;
        this.managementUIState = managementUIState;
        this.manangementConfirmationWindowLayout = new ManangementConfirmationWindowLayout(i18n, eventBus,
                managementUIState, targetManagement, deploymentManagement, distributionSetManagement);
        this.countMessageLabel = new CountMessageLabel(eventBus, targetManagement, i18n, managementUIState,
                targetTable);
        this.targetManagement = targetManagement;
        this.distributionSetManagement = distributionSetManagement;
        init();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.UPDATE_COUNT) {
            UI.getCurrent().access(this::updateActionCount);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event != null) {
            UI.getCurrent().access(() -> {
                if (!hasUnsavedActions()) {
                    closeUnsavedActionsWindow();
                    final String message = manangementConfirmationWindowLayout.getConsolidatedMessage();
                    if (message != null && message.length() > 0) {
                        notification.displaySuccess(message);
                    }
                }
                updateActionCount();
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final BulkUploadPopupEvent event) {
        if (BulkUploadPopupEvent.MINIMIZED == event) {
            UI.getCurrent().access(this::enableBulkUploadStatusButton);
        } else if (BulkUploadPopupEvent.CLOSED == event) {
            UI.getCurrent().access(this::hideBulkUploadStatusButton);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent event) {
        if (!managementUIState.isTargetTableMaximized()) {
            if (TargetComponentEvent.BULK_TARGET_CREATED == event.getTargetComponentEvent()) {
                this.getUI()
                        .access(() -> setUploadStatusButtonCaption(managementUIState.getTargetTableFilters()
                                .getBulkUpload().getFailedUploadCount()
                                + managementUIState.getTargetTableFilters().getBulkUpload().getSucessfulUploadCount()));
            } else if (TargetComponentEvent.BULK_UPLOAD_COMPLETED == event.getTargetComponentEvent()) {
                this.getUI().access(this::updateUploadBtnIconToComplete);
            } else if (TargetComponentEvent.BULK_TARGET_UPLOAD_STARTED == event.getTargetComponentEvent()) {
                this.getUI().access(this::updateUploadBtnIconToProgressIndicator);
            }
        }
    }

    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteRepositoryPermission() || permChecker.hasDeleteTargetPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateTargetPermission() && permChecker.hasReadRepositoryPermission();
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
        return managementViewClientCriterion;
    }

    @Override
    protected void processDroppedComponent(final DragAndDropEvent event) {
        final Component source = event.getTransferable().getSourceComponent();
        if (!DeleteActionsLayoutHelper.isComponentDeletable(source)) {
            notification.displayValidationError(i18n.getMessage("message.cannot.delete"));
        } else {
            processDeletion(event, source);
        }
    }

    private void processDeletion(final DragAndDropEvent event, final Component source) {
        if (DeleteActionsLayoutHelper.isTargetTable(source) && canTargetBeDeleted()) {
            addInDeleteTargetList((Table) source, (TableTransferable) event.getTransferable());
            updateActionCount();
        } else if (DeleteActionsLayoutHelper.isDistributionTable(source) && canDSBeDeleted()) {
            addInDeleteDistributionList((Table) source, (TableTransferable) event.getTransferable());
            updateActionCount();
        } else if (DeleteActionsLayoutHelper.isTargetTag(source) && canTargetBeDeleted()
                && tagNotInUSeInBulkUpload(source)) {
            deleteTargetTag(source);
        } else if (DeleteActionsLayoutHelper.isDistributionTag(source) && canDSBeDeleted()) {
            deleteDistributionTag(source);
        }
    }

    private boolean tagNotInUSeInBulkUpload(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().contains(tagName)) {
            notification.displayValidationError(i18n.getMessage("message.tag.use.bulk.upload", tagName));
            return false;
        }
        return true;
    }

    @Override
    protected void restoreActionCount() {
        updateActionCount();
    }

    @Override
    protected void unsavedActionsWindowClosed() {
        final String message = manangementConfirmationWindowLayout.getConsolidatedMessage();
        if (message != null && message.length() > 0) {
            notification.displaySuccess(message);
        }
    }

    @Override
    protected Component getUnsavedActionsWindowContent() {
        manangementConfirmationWindowLayout.initialize();
        return manangementConfirmationWindowLayout;
    }

    @Override
    protected boolean hasUnsavedActions() {
        return !managementUIState.getDeletedDistributionList().isEmpty()
                || !managementUIState.getDeletedTargetList().isEmpty()
                || !managementUIState.getAssignedList().isEmpty();
    }

    @Override
    protected boolean hasCountMessage() {
        return permChecker.hasTargetReadPermission();
    }

    @Override
    protected Label getCountMessageLabel() {
        return countMessageLabel;
    }

    private void deleteDistributionTag(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(),
                SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);
        if (managementUIState.getDistributionTableFilters().getDistSetTags().contains(tagName)) {
            notification.displayValidationError(i18n.getMessage("message.tag.delete", new Object[] { tagName }));
        } else {
            distributionSetTagManagement.delete(tagName);

            if (source instanceof DragAndDropWrapper) {
                final Long id = DeleteActionsLayoutHelper.getDistributionTagId((DragAndDropWrapper) source);
                eventBus.publish(this,
                        new DistributionSetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, Arrays.asList(id)));
            }

            notification.displaySuccess(i18n.getMessage("message.delete.success", new Object[] { tagName }));
        }
    }

    private void deleteTargetTag(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (managementUIState.getTargetTableFilters().getClickedTargetTags().contains(tagName)) {
            notification.displayValidationError(i18n.getMessage("message.tag.delete", new Object[] { tagName }));
        } else {
            targetTagManagement.delete(tagName);

            if (source instanceof DragAndDropWrapper) {
                final Long id = DeleteActionsLayoutHelper.getTargetTagId((DragAndDropWrapper) source);
                eventBus.publish(this, new TargetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, Arrays.asList(id)));
            }

            notification.displaySuccess(i18n.getMessage("message.delete.success", new Object[] { tagName }));
        }
    }

    private void addInDeleteDistributionList(final Table sourceTable, final TableTransferable transferable) {
        final AbstractTable<?> distTable = (AbstractTable<?>) sourceTable;
        final Set<Long> ids = distTable.getDeletedEntityByTransferable(transferable);

        final Long dsInBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload().getDsNameAndVersion();
        if (isDsInUseInBulkUpload(ids, dsInBulkUpload)) {
            ids.remove(dsInBulkUpload);
        }

        if (ids.isEmpty()) {
            return;
        }

        final List<DistributionSet> findDistributionSetAllById = distributionSetManagement.get(ids);

        if (findDistributionSetAllById.isEmpty()) {
            notification.displayWarning(i18n.getMessage("distributionsets.not.exists"));
            return;
        }

        final Set<DistributionSetIdName> distributionIdNameSet = findDistributionSetAllById.stream()
                .map(distributionSet -> new DistributionSetIdName(distributionSet)).collect(Collectors.toSet());

        checkDeletedDistributionSets(distributionIdNameSet);
    }

    private void checkDeletedDistributionSets(final Set<DistributionSetIdName> distributionIdNameSet) {
        final int existingDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();
        managementUIState.getDeletedDistributionList().addAll(distributionIdNameSet);
        final int newDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();

        showAlreadyDeletedDistributionSetNotfication(existingDeletedDistributionsSize, newDeletedDistributionsSize,
                "message.dists.already.deleted");
        showPendingDeletedNotifaction(distributionIdNameSet, existingDeletedDistributionsSize,
                newDeletedDistributionsSize, "message.dist.deleted.pending");
    }

    private void showPendingDeletedNotifaction(final Set<?> currentValues, final int existingDeletedSize,
            final int newDeletedSize, final String messageKey) {
        if (newDeletedSize - existingDeletedSize == currentValues.size()) {
            return;
        }
        notification.displayValidationError(i18n.getMessage(messageKey));
    }

    private void showAlreadyDeletedDistributionSetNotfication(final int existingDeletedSize, final int newDeletedSize,
            final String messageKey) {

        if (newDeletedSize != existingDeletedSize) {
            return;
        }
        notification.displayValidationError(i18n.getMessage(messageKey));
    }

    private boolean isDsInUseInBulkUpload(final Set<Long> distributionIdNameSet, final Long dsInBulkUpload) {
        if (distributionIdNameSet.contains(dsInBulkUpload)) {
            final Optional<DistributionSet> distributionSet = distributionSetManagement.get(dsInBulkUpload);
            if (!distributionSet.isPresent()) {
                notification.displayWarning(i18n.getMessage("distributionset.not.exists"));
                return true;
            }
            notification.displayValidationError(i18n.getMessage("message.tag.use.bulk.upload", HawkbitCommonUtil
                    .getFormattedNameVersion(distributionSet.get().getName(), distributionSet.get().getVersion())));
            return true;
        }
        return false;
    }

    private void addInDeleteTargetList(final Table sourceTable, final TableTransferable transferable) {
        final TargetTable targetTable = (TargetTable) sourceTable;
        final Set<Long> targetIdSet = targetTable.getDeletedEntityByTransferable(transferable);
        final Collection<Target> findTargetAllById = targetManagement.get(targetIdSet);
        if (findTargetAllById.isEmpty()) {
            notification.displayWarning(i18n.getMessage("targets.not.exists"));
            return;
        }

        final Set<TargetIdName> targetIdNames = findTargetAllById.stream().map(target -> new TargetIdName(target))
                .collect(Collectors.toSet());
        checkDeletedTargets(targetIdNames);
    }

    private void checkDeletedTargets(final Set<TargetIdName> targetIdSet) {
        final int existingDeletedTargetsSize = managementUIState.getDeletedTargetList().size();
        managementUIState.getDeletedTargetList().addAll(targetIdSet);
        final int newDeletedTargetsSize = managementUIState.getDeletedTargetList().size();

        showAlreadyDeletedDistributionSetNotfication(existingDeletedTargetsSize, newDeletedTargetsSize,
                "message.targets.already.deleted");

        showPendingDeletedNotifaction(targetIdSet, existingDeletedTargetsSize, newDeletedTargetsSize,
                "message.target.deleted.pending");
    }

    private void updateActionCount() {
        final int count = managementUIState.getDeletedTargetList().size()
                + managementUIState.getDeletedDistributionList().size() + managementUIState.getAssignedList().size();
        updateActionsCount(count);
    }

    private Boolean canTargetBeDeleted() {
        if (!permChecker.hasDeleteTargetPermission()) {
            notification.displayValidationError(
                    i18n.getMessage("message.permission.insufficient", SpPermission.DELETE_TARGET));
            return false;
        }
        return true;
    }

    private Boolean canDSBeDeleted() {
        if (!permChecker.hasDeleteRepositoryPermission()) {
            notification.displayValidationError(
                    i18n.getMessage("message.permission.insufficient", SpPermission.DELETE_REPOSITORY));
            return false;
        }
        return true;
    }

    @Override
    protected boolean hasBulkUploadPermission() {
        return permChecker.hasCreateTargetPermission();
    }

    @Override
    protected void showBulkUploadWindow() {
        eventBus.publish(this, BulkUploadPopupEvent.MAXIMIMIZED);
    }

    @Override
    protected void restoreBulkUploadStatusCount() {
        final int failedCount = managementUIState.getTargetTableFilters().getBulkUpload().getFailedUploadCount();
        final int successCount = managementUIState.getTargetTableFilters().getBulkUpload().getSucessfulUploadCount();
        if (failedCount != 0 || successCount != 0) {
            setUploadStatusButtonCaption(failedCount + successCount);
            enableBulkUploadStatusButton();
            if (Math.abs(managementUIState.getTargetTableFilters().getBulkUpload().getProgressBarCurrentValue()
                    - 1) < 0.00001) {
                updateUploadBtnIconToComplete();
            } else {
                updateUploadBtnIconToProgressIndicator();
            }

        }
    }
}
