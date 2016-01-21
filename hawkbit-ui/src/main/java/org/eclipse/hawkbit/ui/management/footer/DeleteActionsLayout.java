/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.management.dstable.DistributionTable;
import org.eclipse.hawkbit.ui.management.event.BulkUploadPopupEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = -8112907467821886253L;
    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient UINotification notification;

    @Autowired
    private transient TagManagement tagManagementService;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private ManangementConfirmationWindowLayout manangementConfirmationWindowLayout;

    @Autowired
    private CountMessageLabel countMessageLabel;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#init()
     */
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.UPDATE_COUNT) {
            UI.getCurrent().access(() -> updateActionCount());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent event) {
        if (event == DragEvent.HIDE_DROP_HINT) {
            hideDropHints();
        } else if (event == DragEvent.TARGET_TAG_DRAG || event == DragEvent.TARGET_DRAG) {
            /**
             * Duplicate permission check required as hasDeletePermission() is
             * generic both for target and ds.
             */
            if (permChecker.hasDeleteTargetPermission()) {
                showDropHints();
            }
        } else if ((event == DragEvent.DISTRIBUTION_TAG_DRAG || event == DragEvent.DISTRIBUTION_DRAG)
                && permChecker.hasDeleteDistributionPermission()) {
            /**
             * Duplicate permission check required as hasDeletePermission() is
             * generic both for target and ds.
             */
            showDropHints();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
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

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final BulkUploadPopupEvent event) {
        if (BulkUploadPopupEvent.MINIMIZED == event) {
            UI.getCurrent().access(() -> enableBulkUploadStatusButton());
        } else if (BulkUploadPopupEvent.CLOSED == event) {
            UI.getCurrent().access(() -> hideBulkUploadStatusButton());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent event) {
        if (!managementUIState.isTargetTableMaximized()) {
            if (TargetComponentEvent.BULK_TARGET_CREATED == event.getTargetComponentEvent()) {
                this.getUI().access(
                        () -> setUploadStatusButtonCaption(managementUIState.getTargetTableFilters().getBulkUpload()
                                .getFailedUploadCount()
                                + managementUIState.getTargetTableFilters().getBulkUpload().getSucessfulUploadCount()));
            } else if (TargetComponentEvent.BULK_UPLOAD_COMPLETED == event.getTargetComponentEvent()) {
                this.getUI().access(() -> updateUploadBtnIconToComplete());
            } else if (TargetComponentEvent.BULK_TARGET_UPLOAD_STARTED == event.getTargetComponentEvent()) {
                this.getUI().access(() -> updateUploadBtnIconToProgressIndicator());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * hasDeletePermission()
     */
    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteDistributionPermission() || permChecker.hasDeleteTargetPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * hasUpdatePermission()
     */
    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateDistributionPermission() || permChecker.hasUpdateTargetPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getDeleteAreaLabel()
     */
    @Override
    protected String getDeleteAreaLabel() {
        return i18n.get("label.components.drop.area");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getDeleteAreaId()
     */
    @Override
    protected String getDeleteAreaId() {
        return SPUIComponetIdProvider.DELETE_BUTTON_WRAPPER_ID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getDeleteLayoutAcceptCriteria ()
     */
    @Override
    protected AcceptCriterion getDeleteLayoutAcceptCriteria() {
        return managementViewAcceptCriteria;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * processDroppedComponent(com .vaadin.event.dd.DragAndDropEvent)
     */
    @Override
    protected void processDroppedComponent(final DragAndDropEvent event) {
        final Component source = event.getTransferable().getSourceComponent();
        if (!DeleteActionsLayoutHelper.isComponentDeletable(source)) {
            notification.displayValidationError(i18n.get("message.cannot.delete"));
        } else {
            processDeletion(event, source);
        }
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        hideDropHints();
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
            notification.displayValidationError(i18n.get("message.tag.use.bulk.upload", tagName));
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getNoActionsButtonLabel()
     */
    @Override
    protected String getNoActionsButtonLabel() {
        return i18n.get("button.no.actions");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getActionsButtonLabel()
     */
    @Override
    protected String getActionsButtonLabel() {
        return i18n.get("button.actions");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * reloadActionCount()
     */
    @Override
    protected void restoreActionCount() {
        updateActionCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getUnsavedActionsWindowCaption ()
     */
    @Override
    protected String getUnsavedActionsWindowCaption() {
        return i18n.get("caption.save.window");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * unsavedActionsWindowClosed()
     */
    @Override
    protected void unsavedActionsWindowClosed() {
        final String message = manangementConfirmationWindowLayout.getConsolidatedMessage();
        if (message != null && message.length() > 0) {
            notification.displaySuccess(message);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getUnsavedActionsWindowContent ()
     */
    @Override
    protected Component getUnsavedActionsWindowContent() {
        manangementConfirmationWindowLayout.init();
        return manangementConfirmationWindowLayout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * hasUnsavedActions()
     */
    @Override
    protected boolean hasUnsavedActions() {
        if (!managementUIState.getDeletedDistributionList().isEmpty()
                || !managementUIState.getDeletedTargetList().isEmpty()
                || !managementUIState.getAssignedList().isEmpty()) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * hasCountMessage()
     */
    @Override
    protected boolean hasCountMessage() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * getCountMessageLabel()
     */
    @Override
    protected Label getCountMessageLabel() {
        return countMessageLabel;
    }

    private void deleteDistributionTag(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(),
                SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);
        if (managementUIState.getDistributionTableFilters().getDistSetTags().contains(tagName)) {
            notification.displayValidationError(i18n.get("message.tag.delete", new Object[] { tagName }));
        } else {
            tagManagementService.deleteDistributionSetTag(tagName);
            notification.displaySuccess(i18n.get("message.delete.success", new Object[] { tagName }));
        }
    }

    private void deleteTargetTag(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (managementUIState.getTargetTableFilters().getClickedTargetTags().contains(tagName)) {
            notification.displayValidationError(i18n.get("message.tag.delete", new Object[] { tagName }));
        } else {
            tagManagementService.deleteTargetTag(tagName);
            notification.displaySuccess(i18n.get("message.delete.success", new Object[] { tagName }));
        }
    }

    /**
     * 
     * Prepare deleted distribution set .
     * 
     * @param sourceTable
     *            {@link DistributionTable}
     * @param transferable
     *            {@link TableTransferable}
     * 
     */
    private void addInDeleteDistributionList(final Table sourceTable, final TableTransferable transferable) {
        final Set<DistributionSetIdName> distSelected = HawkbitCommonUtil.getSelectedDSDetails(sourceTable);
        final Set<DistributionSetIdName> distributionIdNameSet = new HashSet<DistributionSetIdName>();

        if (!distSelected.contains(transferable.getData(SPUIDefinitions.ITEMID))) {
            distributionIdNameSet.add((DistributionSetIdName) transferable.getData(SPUIDefinitions.ITEMID));
        } else {
            distributionIdNameSet.addAll(distSelected);
        }

        final DistributionSetIdName dsInBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload()
                .getDsNameAndVersion();
        if (isDsInUseInBulkUpload(distributionIdNameSet, dsInBulkUpload)) {
            distributionIdNameSet.remove(dsInBulkUpload);
        }

        if (!distributionIdNameSet.isEmpty()) {

            /*
             * Flags to identify whether all dropped distributions are already
             * in the deleted list (or) some distributions are already in the
             * deleted distribution list.
             */
            final int existingDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();
            managementUIState.getDeletedDistributionList().addAll(distributionIdNameSet);
            final int newDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();
            if (newDeletedDistributionsSize == existingDeletedDistributionsSize) {
                /*
                 * No new distributions are added, all distributions dropped now
                 * are already available in the delete list. Hence display
                 * warning message accordingly.
                 */
                notification.displayValidationError(i18n.get("message.targets.already.deleted"));
            } else if (newDeletedDistributionsSize - existingDeletedDistributionsSize != distributionIdNameSet.size()) {
                /*
                 * Not the all distributions dropped now are added to the delete
                 * list. There are some distributions are already there in the
                 * delete list. Hence display warning message accordingly.
                 */
                notification.displayValidationError(i18n.get("message.dist.deleted.pending"));
            }
        }
    }

    private boolean isDsInUseInBulkUpload(final Set<DistributionSetIdName> distributionIdNameSet,
            final DistributionSetIdName dsInBulkUpload) {
        if (distributionIdNameSet.contains(dsInBulkUpload)) {
            notification.displayValidationError(i18n.get("message.tag.use.bulk.upload",
                    HawkbitCommonUtil.getFormattedNameVersion(dsInBulkUpload.getName(), dsInBulkUpload.getVersion())));
            return true;
        }
        return false;
    }

    /**
     * Prepare deleted target list.
     * 
     * @param sourceTable
     *            {@link TargetTable}
     * @param transferable
     *            {@link TableTransferable}
     * 
     */
    private void addInDeleteTargetList(final Table sourceTable, final TableTransferable transferable) {
        final Set<TargetIdName> targetSelected = HawkbitCommonUtil.getSelectedTargetDetails(sourceTable);

        final Set<TargetIdName> targetIdNameSet = new HashSet<>();
        if (!targetSelected.contains(transferable.getData(SPUIDefinitions.ITEMID))) {
            targetIdNameSet.add((TargetIdName) transferable.getData(SPUIDefinitions.ITEMID));
        } else {
            targetIdNameSet.addAll(targetSelected);
        }

        /*
         * Flags to identify whether all dropped targets are already in the
         * deleted list (or) some target are already in the deleted distribution
         * list.
         */
        final int existingDeletedTargetsSize = managementUIState.getDeletedTargetList().size();
        managementUIState.getDeletedTargetList().addAll(targetIdNameSet);
        final int newDeletedTargetsSize = managementUIState.getDeletedTargetList().size();
        if (newDeletedTargetsSize == existingDeletedTargetsSize) {
            /*
             * No new targets are added, all targets dropped now are already
             * available in the delete list. Hence display warning message
             * accordingly.
             */
            notification.displayValidationError(i18n.get("message.targets.already.deleted"));
        } else if (newDeletedTargetsSize - existingDeletedTargetsSize != targetIdNameSet.size()) {
            /*
             * Not the all targets dropped now are added to the delete list.
             * There are some targets are already there in the delete list.
             * Hence display warning message accordingly.
             */
            notification.displayValidationError(i18n.get("message.target.deleted.pending"));
        }
    }

    /**
     * Update the software module delete count.
     */
    private void updateActionCount() {
        final int count = managementUIState.getDeletedTargetList().size()
                + managementUIState.getDeletedDistributionList().size() + managementUIState.getAssignedList().size();
        updateActionsCount(count);
    }

    private Boolean canTargetBeDeleted() {
        if (!permChecker.hasDeleteTargetPermission()) {
            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        }
        return true;
    }

    private Boolean canDSBeDeleted() {
        if (!permChecker.hasDeleteDistributionPermission()) {
            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * hasBulkUploadPermission()
     */
    @Override
    protected boolean hasBulkUploadPermission() {
        return permChecker.hasCreateTargetPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * showBulkUploadWindow()
     */
    @Override
    protected void showBulkUploadWindow() {
        eventBus.publish(this, BulkUploadPopupEvent.MAXIMIMIZED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * restoreBulkUploadStatusCount()
     */
    @Override
    protected void restoreBulkUploadStatusCount() {
        final Long failedCount = managementUIState.getTargetTableFilters().getBulkUpload().getFailedUploadCount();
        final Long successCount = managementUIState.getTargetTableFilters().getBulkUpload().getSucessfulUploadCount();
        if (failedCount != 0 || successCount != 0) {
            setUploadStatusButtonCaption(failedCount + successCount);
            enableBulkUploadStatusButton();
            if (Math.abs(managementUIState.getTargetTableFilters().getBulkUpload().getProgressBarCurrentValue() - 1) < 0.00001) {
                updateUploadBtnIconToComplete();
            } else {
                updateUploadBtnIconToProgressIndicator();
            }

        }
    }

    @Override
    protected boolean hasReadPermission() {
        return permChecker.hasTargetReadPermission();
    }
}
