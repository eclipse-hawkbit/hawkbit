/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.footer;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadViewAcceptCriteria;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
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
import com.vaadin.ui.UI;

/**
 * Upload view footer layout implementation.
 *
 */
@SpringComponent
@ViewScope
public class SMDeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = -3273982053389866299L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient UINotification notification;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private UploadViewConfirmationWindowLayout uploadViewConfirmationWindowLayout;

    @Autowired
    private UploadViewAcceptCriteria uploadViewAcceptCriteria;

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
    void onEvent(final UploadArtifactUIEvent event) {

        if (isSoftwareEvent(event) || isSoftwareTypeEvent(event)) {

            UI.getCurrent().access(() -> {
                if (!hasUnsavedActions()) {
                    closeUnsavedActionsWindow();
                    final String message = uploadViewConfirmationWindowLayout.getConsolidatedMessage();
                    if (message != null && message.length() > 0) {
                        notification.displaySuccess(message);
                    }
                }
                updateSWActionCount();
            });
        }
        if (event == UploadArtifactUIEvent.SOFTWARE_DRAG_START
                || event == UploadArtifactUIEvent.SOFTWARE_TYPE_DRAG_START) {
            showDropHints();
        }
    }

    private boolean isSoftwareEvent(final UploadArtifactUIEvent event) {
        return event == UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE
                || event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE
                || event == UploadArtifactUIEvent.DISCARD_DELETE_SOFTWARE;
    }

    private boolean isSoftwareTypeEvent(final UploadArtifactUIEvent event) {
        return event == UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE_TYPE
                || event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE
                || event == UploadArtifactUIEvent.DISCARD_DELETE_SOFTWARE_TYPE;
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent event) {
        if (event == DragEvent.HIDE_DROP_HINT) {
            UI.getCurrent().access(() -> hideDropHints());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * hasDeletePermission()
     */
    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteDistributionPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * hasUpdatePermission()
     */
    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateDistributionPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * getDeleteAreaLabel()
     */
    @Override
    protected String getDeleteAreaLabel() {
        return i18n.get("label.software.module.drop.area");
    }

    @Override
    protected String getDeleteAreaId() {
        return SPUIComponetIdProvider.DELETE_BUTTON_WRAPPER_ID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * getDeleteLayoutAcceptCriteria()
     */
    @Override
    protected AcceptCriterion getDeleteLayoutAcceptCriteria() {
        return uploadViewAcceptCriteria;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * processDroppedComponent(com.vaadin .event.dd.DragAndDropEvent)
     */
    @Override
    protected void processDroppedComponent(final DragAndDropEvent event) {
        final Component sourceComponent = event.getTransferable().getSourceComponent();
        if (sourceComponent instanceof Table) {
            final Table sourceTable = (Table) event.getTransferable().getSourceComponent();
            addToDeleteList(sourceTable);
            updateSWActionCount();
        }
        if (sourceComponent.getId().startsWith(SPUIComponetIdProvider.UPLOAD_TYPE_BUTTON_PREFIX)) {

            final String swModuleTypeName = sourceComponent.getId().replace(
                    SPUIComponetIdProvider.UPLOAD_TYPE_BUTTON_PREFIX, "");
            if (artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType().isPresent()
                    && artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType().get().getName()
                            .equalsIgnoreCase(swModuleTypeName)) {
                notification.displayValidationError(i18n.get("message.swmodule.type.check.delete",
                        new Object[] { swModuleTypeName }));
            } else {
                deleteSWModuleType(swModuleTypeName);
                updateSWActionCount();
            }

        }
        hideDropHints();
    }

    private void deleteSWModuleType(final String swModuleTypeName) {
        artifactUploadState.getSelectedDeleteSWModuleTypes().add(swModuleTypeName);
    }

    private void addToDeleteList(final Table sourceTable) {
        final Set<Long> swModuleIds = (Set<Long>) sourceTable.getValue();
        swModuleIds.forEach(id -> {
            final String swModuleName = (String) sourceTable.getContainerDataSource().getItem(id)
                    .getItemProperty(SPUILabelDefinitions.NAME_VERSION).getValue();
            artifactUploadState.getDeleteSofwareModules().put(id, swModuleName);
        });
    }

    /**
     * Update the software module delete count.
     */
    private void updateSWActionCount() {
        final int count = artifactUploadState.getDeleteSofwareModules().size()
                + artifactUploadState.getSelectedDeleteSWModuleTypes().size();
        updateActionsCount(count);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * getNoActionsButtonLabel()
     */
    @Override
    protected String getNoActionsButtonLabel() {
        return i18n.get("button.no.actions");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.footer.DeleteActionsLayout#
     * getActionsButtonLabel()
     */
    @Override
    protected String getActionsButtonLabel() {
        return i18n.get("button.actions");
    }

    @Override
    protected void restoreActionCount() {
        updateSWActionCount();
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
        final String message = uploadViewConfirmationWindowLayout.getConsolidatedMessage();
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
        uploadViewConfirmationWindowLayout.init();
        return uploadViewConfirmationWindowLayout;
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
        return !artifactUploadState.getDeleteSofwareModules().isEmpty()
                || !artifactUploadState.getSelectedDeleteSWModuleTypes().isEmpty();
    }

    @Override
    protected boolean hasCountMessage() {
        return false;
    }

    @Override
    protected Label getCountMessageLabel() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * isBulkUploadAllowed()
     */
    @Override
    protected boolean hasBulkUploadPermission() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * showBulkUploadWindow()
     */
    @Override
    protected void showBulkUploadWindow() {
        /**
         * Bulk upload not supported .No implementation required.
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * restoreBulkUploadStatusCount()
     */
    @Override
    protected void restoreBulkUploadStatusCount() {
        /**
         * Bulk upload not supported .No implementation required.
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout#
     * hasReadPermission()
     */
    @Override
    protected boolean hasReadPermission() {
        return permChecker.hasReadDistributionPermission();
    }

}
