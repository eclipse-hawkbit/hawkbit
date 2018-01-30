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

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.footer.AbstractDeleteActionsLayout;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
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
 * Upload view footer layout implementation.
 */
public class SMDeleteActionsLayout extends AbstractDeleteActionsLayout {

    private static final long serialVersionUID = -3273982053389866299L;

    private final ArtifactUploadState artifactUploadState;

    private final UploadViewConfirmationWindowLayout uploadViewConfirmationWindowLayout;

    private final UploadViewClientCriterion uploadViewClientCriterion;

    public SMDeleteActionsLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final UINotification notification, final ArtifactUploadState artifactUploadState,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final UploadViewClientCriterion uploadViewClientCriterion) {
        super(i18n, permChecker, eventBus, notification);
        this.artifactUploadState = artifactUploadState;
        this.uploadViewConfirmationWindowLayout = new UploadViewConfirmationWindowLayout(i18n, eventBus,
                softwareModuleManagement, softwareModuleTypeManagement, artifactUploadState);
        this.uploadViewClientCriterion = uploadViewClientCriterion;

        init();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
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

    @Override
    protected boolean hasDeletePermission() {
        return permChecker.hasDeleteRepositoryPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        /**
         * Footer layout should be displayed only when software modeule has
         * delete permission.So update permission need not be checked in this
         * case.
         */
        return false;
    }

    @Override
    protected String getDeleteAreaLabel() {
        return i18n.getMessage("label.software.module.drop.area");
    }

    @Override
    protected String getDeleteAreaId() {
        return UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID;
    }

    @Override
    protected AcceptCriterion getDeleteLayoutAcceptCriteria() {
        return uploadViewClientCriterion;
    }

    @Override
    protected void processDroppedComponent(final DragAndDropEvent event) {
        final Component sourceComponent = event.getTransferable().getSourceComponent();
        if (sourceComponent instanceof Table) {
            final Table sourceTable = (Table) event.getTransferable().getSourceComponent();
            addToDeleteList(sourceTable, (TableTransferable) event.getTransferable());
            updateSWActionCount();
        }
        if (sourceComponent.getId().startsWith(UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX)) {

            final String swModuleTypeName = sourceComponent.getId()
                    .replace(UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX, "");
            if (artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType()
                    .map(type -> type.getName().equalsIgnoreCase(swModuleTypeName)).orElse(false)) {
                notification.displayValidationError(
                        i18n.getMessage("message.swmodule.type.check.delete", new Object[] { swModuleTypeName }));
            } else {
                deleteSWModuleType(swModuleTypeName);
                updateSWActionCount();
            }

        }
    }

    private void deleteSWModuleType(final String swModuleTypeName) {
        artifactUploadState.getSelectedDeleteSWModuleTypes().add(swModuleTypeName);
    }

    private void addToDeleteList(final Table sourceTable, final TableTransferable transferable) {
        final AbstractTable<?> swTable = (AbstractTable<?>) sourceTable;
        final Set<Long> swModuleIdNameSet = swTable.getDeletedEntityByTransferable(transferable);

        swModuleIdNameSet.forEach(id -> {
            final String swModuleName = (String) sourceTable.getContainerDataSource().getItem(id)
                    .getItemProperty(SPUILabelDefinitions.NAME_VERSION).getValue();
            artifactUploadState.getDeleteSofwareModules().put(id, swModuleName);
        });
    }

    private void updateSWActionCount() {
        final int count = artifactUploadState.getDeleteSofwareModules().size()
                + artifactUploadState.getSelectedDeleteSWModuleTypes().size();
        updateActionsCount(count);
    }

    @Override
    protected void restoreActionCount() {
        updateSWActionCount();
    }

    @Override
    protected void unsavedActionsWindowClosed() {
        final String message = uploadViewConfirmationWindowLayout.getConsolidatedMessage();
        if (message != null && message.length() > 0) {
            notification.displaySuccess(message);
        }
    }

    @Override
    protected Component getUnsavedActionsWindowContent() {
        uploadViewConfirmationWindowLayout.initialize();
        return uploadViewConfirmationWindowLayout;
    }

    @Override
    protected boolean hasUnsavedActions() {
        return !artifactUploadState.getDeleteSofwareModules().isEmpty()
                || !artifactUploadState.getSelectedDeleteSWModuleTypes().isEmpty();
    }

}
