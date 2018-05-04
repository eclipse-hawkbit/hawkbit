/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.distributions.smtype.AbstractSoftwareModuleTypeLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for Software Module Type Layout which is used for updating and
 * deleting Software Module Types, includes the combobox for selecting the type
 * to modify
 *
 */
public abstract class AbstractSoftwareModuleTypeLayoutForModify extends AbstractSoftwareModuleTypeLayout {

    private static final long serialVersionUID = 1L;

    private final String selectedTypeName;

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
    public AbstractSoftwareModuleTypeLayoutForModify(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final String selectedTypeName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement);
        this.selectedTypeName = selectedTypeName;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTagDetails(selectedTypeName);
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        disableFields();
    }

    @Override
    protected void disableFields() {
        getTagName().setEnabled(false);
        getTypeKey().setEnabled(false);
        getAssignOptiongroup().setEnabled(false);
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    protected void setTagDetails(final String targetTagSelected) {
        getTagName().setValue(targetTagSelected);
        getSoftwareModuleTypeManagement().getByName(targetTagSelected).ifPresent(selectedTypeTag -> {
            getTagDesc().setValue(selectedTypeTag.getDescription());
            getTypeKey().setValue(selectedTypeTag.getKey());
            if (selectedTypeTag.getMaxAssignments() == 1) {
                getAssignOptiongroup().setValue(getSingleAssignStr());
            } else {
                getAssignOptiongroup().setValue(getMultiAssignStr());
            }
            setColorPickerComponentsColor(selectedTypeTag.getColour());
        });
        disableFields();
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

}
