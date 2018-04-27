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
import java.util.Set;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the create or update software module type.
 *
 */
public class DeleteSoftwareTypeLayout extends UpdateSoftwareTypeLayout {

    private static final long serialVersionUID = 1L;

    private final Set<String> selectedTypes;

    /**
     * Constructor for CreateUpdateSoftwareTypeLayout
     * 
     * @param i18n
     *            I18N
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            management for {@link SoftwareModuleType}s
     */
    public DeleteSoftwareTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final Set<String> selectedTypes) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement);
        this.selectedTypes = selectedTypes;
    }

    @Override
    public void init() {
        super.init();
        getUpdateCombobox().getComboLabel()
                .setValue(getI18n().getMessage("label.choose.type", getI18n().getMessage("label.choose.tag.delete")));
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.delete"),
                getI18n().getMessage("caption.type"));
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getTagDesc().setEnabled(false);
        getTagName().setEnabled(false);
        getContentLayout().removeComponent(getColorLabelLayout());
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

    // TODO MR: selectedTypes sind nicht korrekt. Je nach View werden andere
    // Werte gesetzt!!! Events!!! Klasse muss für jede View extra generiert
    // werden!!!!
    private void deleteSoftwareModuleType() {
        final String tagNameToDelete = getTagName().getValue();
        final Optional<SoftwareModuleType> swmTypeToDelete = getSoftwareModuleTypeManagement()
                .getByName(tagNameToDelete);
        swmTypeToDelete.ifPresent(tag -> {
            if (selectedTypes.contains(tagNameToDelete)) {
                getUiNotification().displayValidationError(getI18n().getMessage("message.tag.delete", tagNameToDelete));
            } else {
                getSoftwareModuleTypeManagement().delete(swmTypeToDelete.get().getId());
                // TODO MR EVENTS!!!
                getEventBus().publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE);
                getEventBus().publish(this, SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES);
                getUiNotification().displaySuccess(getI18n().getMessage("message.sw.module.type.delete"));
                selectedTypes.remove(tagNameToDelete);
            }
        });
    }

}
