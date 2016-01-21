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
import org.eclipse.hawkbit.ui.management.event.DistributionTagEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTagEvent.DistTagComponentEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent.TargetTagComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
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
import com.vaadin.ui.DragAndDropWrapper;
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
        /* Get the source component */
        final Component source = event.getTransferable().getSourceComponent();
        /*
         * Since this delete drop layout used only to drop target table,
         * distribution table, Target tag and distribution tag to delete.
         */
        if (!isComponentDeletable(source)) {
            notification.displayValidationError(i18n.get("message.cannot.delete"));
        } else {
            if (isTargetTable(source) && canTargetBeDeleted()) {
                addInDeleteTargetList((Table) source, (TableTransferable) event.getTransferable());
                updateActionCount();
            } else if (isDistributionTable(source) && canDSBeDeleted()) {
                addInDeleteDistributionList((Table) source, (TableTransferable) event.getTransferable());
                updateActionCount();
            } else if (isTargetTag(source) && canTargetBeDeleted()) {
                deleteTargetTag(source);
            } else if (isDistributionTag(source) && canDSBeDeleted()) {
                deleteDistributionTag(source);
            }
        }
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        hideDropHints();
    }

    private Boolean isComponentDeletable(final Component source) {
        if (isTargetTable(source) || isDistributionTable(source) || isTargetTag(source) || isDistributionTag(source)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
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
    protected void reloadActionCount() {
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
            eventBus.publish(this, new DistributionTagEvent(DistTagComponentEvent.DELETE_DIST_TAG, tagName));
        }
    }

    private void deleteTargetTag(final Component source) {
        final String tagName = HawkbitCommonUtil.removePrefix(source.getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (managementUIState.getTargetTableFilters().getClickedTargetTags().contains(tagName)) {
            notification.displayValidationError(i18n.get("message.tag.delete", new Object[] { tagName }));
        } else {
            tagManagementService.deleteTargetTag(tagName);
            notification.displaySuccess(i18n.get("message.delete.success", new Object[] { tagName }));
            eventBus.publish(this, new TargetTagEvent(TargetTagComponentEvent.DELETE_TARGETTAG, tagName));
        }
    }

    /**
     * @param source
     */
    private void addInDeleteDistributionList(final Table sourceTable, final TableTransferable transferable) {
        @SuppressWarnings("unchecked")
        final Set<DistributionSetIdName> distSelected = (Set<DistributionSetIdName>) sourceTable.getValue();
        final Set<DistributionSetIdName> distributionIdNameSet = new HashSet<DistributionSetIdName>();
        if (!distSelected.contains(transferable.getData(SPUIDefinitions.ITEMID))) {
            distributionIdNameSet.add((DistributionSetIdName) transferable.getData(SPUIDefinitions.ITEMID));
        } else {
            distributionIdNameSet.addAll(distSelected);
        }

        /*
         * Flags to identify whether all dropped distributions are already in
         * the deleted list (or) some distributions are already in the deleted
         * distribution list.
         */
        final int existingDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();
        managementUIState.getDeletedDistributionList().addAll(distributionIdNameSet);
        final int newDeletedDistributionsSize = managementUIState.getDeletedDistributionList().size();
        if (newDeletedDistributionsSize == existingDeletedDistributionsSize) {
            /*
             * No new distributions are added, all distributions dropped now are
             * already available in the delete list. Hence display warning
             * message accordingly.
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

    /**
     * @param source
     * @param transferable
     */
    private void addInDeleteTargetList(final Table sourceTable, final TableTransferable transferable) {
        @SuppressWarnings("unchecked")
        final Set<TargetIdName> targetSelected = (Set<TargetIdName>) sourceTable.getValue();
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

    private boolean isTargetTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.TARGET_TABLE_ID);
    }

    private boolean isDistributionTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.DIST_TABLE_ID);
    }

    private boolean isTargetTag(final Component source) {
        if (source instanceof DragAndDropWrapper) {
            final String wrapperData = ((DragAndDropWrapper) source).getData().toString();
            final String id = wrapperData.replace(SPUIDefinitions.TARGET_TAG_BUTTON, "");
            if (wrapperData.contains(SPUIDefinitions.TARGET_TAG_BUTTON) && !id.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isDistributionTag(final Component source) {
        if (source instanceof DragAndDropWrapper) {
            final String wrapperData = ((DragAndDropWrapper) source).getData().toString();
            final String id = wrapperData.replace(SPUIDefinitions.DISTRIBUTION_TAG_BUTTON, "");
            if (wrapperData.contains(SPUIDefinitions.DISTRIBUTION_TAG_BUTTON) && !id.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
