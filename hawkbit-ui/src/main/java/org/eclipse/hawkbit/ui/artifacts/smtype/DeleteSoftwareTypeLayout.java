/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.UploadArtifactView;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.distributions.DistributionsView;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when deleting a software module
 * type on Distributions and Upload View.
 *
 */
public class DeleteSoftwareTypeLayout extends AbstractSoftwareModuleTypeLayoutForModify {

    private static final long serialVersionUID = 1L;

    private final transient Optional<SoftwareModuleType> selectedSoftwareModuleType;

    private final String currentView;

    public DeleteSoftwareTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final Optional<SoftwareModuleType> selectedSoftwareModuleType, final String currentView,
            final String selectedTypeName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement,
                selectedTypeName);
        this.selectedSoftwareModuleType = selectedSoftwareModuleType;
        this.currentView = currentView;
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.delete") + " " + getI18n().getMessage("caption.type");
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        disableFields();
        getContentLayout().removeComponent(getColorLabelLayout());
    }

    @Override
    protected void disableFields() {
        super.disableFields();
        getTagDesc().setEnabled(false);
    }

    @Override
    protected void saveEntity() {
        deleteSoftwareModuleType();
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

    @Override
    protected boolean isDeleteAction() {
        return true;
    }

    private void deleteSoftwareModuleType() {
        final String tagNameToDelete = getTagName().getValue();
        final Optional<SoftwareModuleType> swmTypeToDelete = getSoftwareModuleTypeManagement()
                .getByName(tagNameToDelete);
        swmTypeToDelete.ifPresent(tag -> {
            if (selectedSoftwareModuleType.equals(swmTypeToDelete)) {
                getUiNotification().displayValidationError(getI18n().getMessage("message.tag.delete", tagNameToDelete));
            } else {
                getSoftwareModuleTypeManagement().delete(swmTypeToDelete.get().getId());
                if (DistributionsView.VIEW_NAME.equals(currentView)) {
                    getEventBus().publish(this, SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES);
                } else if (UploadArtifactView.VIEW_NAME.equals(currentView)) {
                    getEventBus().publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE);
                }
                getUiNotification().displaySuccess(getI18n().getMessage("message.delete.success", tagNameToDelete));
            }
        });
    }

}
