/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.footer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;

/**
 * Distributions footer layout implementation.
 * 
 *
 * 
 */
@org.springframework.stereotype.Component
@ViewScope
public class DSDeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = 3494052985006132714L;

    private static final List<Object> DISPLAY_DROP_HINT_EVENTS = new ArrayList<>(
            Arrays.asList(DragEvent.DISTRIBUTION_TYPE_DRAG, DragEvent.DISTRIBUTION_DRAG, DragEvent.SOFTWAREMODULE_DRAG,
                    DragEvent.SOFTWAREMODULE_TYPE_DRAG));

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient UINotification notification;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private DistributionsConfirmationWindowLayout distConfirmationWindowLayout;

    @Autowired
    private DistributionsViewAcceptCriteria distributionsViewAcceptCriteria;

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
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent event) {
        if (event == DragEvent.HIDE_DROP_HINT) {
            hideDropHints();
        } else if (DISPLAY_DROP_HINT_EVENTS.contains(event)) {
            showDropHints();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.footer.AbstractDeleteActionsLayout#
     * hasDeletePermission()
     */
    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteDistributionPermission();

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

        return permChecker.hasUpdateDistributionPermission();
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

        return distributionsViewAcceptCriteria;
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
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        hideDropHints();

    }

    private void processDeleteDitSetType(final String distTypeId) {
        final String distTypeName = HawkbitCommonUtil.removePrefix(distTypeId,
                SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS);

        if (null != manageDistUIState.getManageDistFilters().getClickedDistSetType() && manageDistUIState
                .getManageDistFilters().getClickedDistSetType().getName().equalsIgnoreCase(distTypeName)) {
            notification
                    .displayValidationError(i18n.get("message.dist.type.check.delete", new Object[] { distTypeName }));
        } else {
            manageDistUIState.getSelectedDeleteDistSetTypes().add(distTypeName);
            updateDSActionCount();
        }

    }

    private void processDeleteSWType(final String swTypeId) {
        final String swModuleTypeName = HawkbitCommonUtil.removePrefix(swTypeId,
                SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS);

        if (manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType().isPresent()
                && manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType().get().getName()
                        .equalsIgnoreCase(swModuleTypeName)) {
            notification.displayValidationError(
                    i18n.get("message.swmodule.type.check.delete", new Object[] { swModuleTypeName }));
        } else {
            manageDistUIState.getSelectedDeleteSWModuleTypes().add(swModuleTypeName);
            updateDSActionCount();
        }

    }

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
        final int existingDeletedDistributionsSize = manageDistUIState.getDeletedDistributionList().size();
        manageDistUIState.getDeletedDistributionList().addAll(distributionIdNameSet);
        final int newDeletedDistributionsSize = manageDistUIState.getDeletedDistributionList().size();
        if (newDeletedDistributionsSize == existingDeletedDistributionsSize) {
            /*
             * No new distributions are added, all distributions dropped now are
             * already available in the delete list. Hence display warning
             * message accordingly.
             */

            uiNotification.displayValidationError(i18n.get("message.targets.already.deleted"));
        } else if (newDeletedDistributionsSize - existingDeletedDistributionsSize != distributionIdNameSet.size()) {
            /*
             * Not the all distributions dropped now are added to the delete
             * list. There are some distributions are already there in the
             * delete list. Hence display warning message accordingly.
             */

            uiNotification.displayValidationError(i18n.get("message.dist.deleted.pending"));
        }

    }

    private void addToSWDeleteList(final Table sourceTable, final TableTransferable transferable) {

        @SuppressWarnings("unchecked")
        final Set<Long> swModuleSelected = (Set<Long>) sourceTable.getValue();
        final Set<Long> swModuleIdNameSet = new HashSet<Long>();
        if (!swModuleSelected.contains(transferable.getData(SPUIDefinitions.ITEMID))) {
            swModuleIdNameSet.add((Long) transferable.getData(SPUIDefinitions.ITEMID));
        } else {
            swModuleIdNameSet.addAll(swModuleSelected);
        }
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

        for (final Entry<DistributionSetIdName, Set<SoftwareModuleIdName>> mapEntry : manageDistUIState
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
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.UPDATE_COUNT) {
            updateDSActionCount();
        }

    }

    /**
     * 
     * @param source
     * @return true if it is distribution table
     */
    private boolean isDistributionTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.DIST_TABLE_ID);
    }

    /**
     * 
     * @param source
     * @return true if it is SoftwareModule table
     */
    private boolean isSoftwareModuleTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE);
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
        updateDSActionCount();

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
        final String message = distConfirmationWindowLayout.getConsolidatedMessage();
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
        distConfirmationWindowLayout.init();
        return distConfirmationWindowLayout;
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

    @Override
    protected boolean hasCountMessage() {
        return false;
    }

    @Override
    protected Label getCountMessageLabel() {
        return null;
    }

}
