/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.layouts.UpdateTag;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window.CloseListener;

/**
 * Layout for pop-up window which is created when updating a target tag on the
 * Deployment View.
 *
 */
public class UpdateTargetTagLayout extends AbstractTargetTagLayout implements UpdateTag {

    private static final long serialVersionUID = 1L;

    private final String selectedTagName;

    private final CloseListener closeListener;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param targetTagManagement
     *            TargetTagManagement
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param selectedTagName
     *            name of the selected target tag to update
     * @param closeListener
     *            CloseListener
     */
    public UpdateTargetTagLayout(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification, final String selectedTagName, final CloseListener closeListener) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, targetTagManagement);
        this.selectedTagName = selectedTagName;
        this.closeListener = closeListener;
        initUpdatePopup();
    }

    private void initUpdatePopup() {
        setTagDetails(selectedTagName);
        getWindow().addCloseListener(closeListener);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.update", getI18n().getMessage("caption.tag"));
    }

    @Override
    protected void saveEntity() {
        updateExistingTag(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, getTagName().getValue())));
    }

    private void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = getEntityFactory().tag().update(targetObj.getId()).name(getTagName().getValue())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));

        getTargetTagManagement().update(update);
        getEventBus().publish(this, new TargetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (TargetTag) targetObj));
        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", targetObj.getName()));
    }

    @Override
    public void setTagDetails(final String selectedEntity) {
        final Optional<TargetTag> selectedTargetTag = getTargetTagManagement().getByName(selectedEntity);
        selectedTargetTag.ifPresent(tag -> {
            getTagName().setValue(tag.getName());
            getTagName().setEnabled(false);
            getTagDesc().setValue(tag.getDescription());
            if (tag.getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(tag.getColour()), tag.getColour());
            }
            getWindow().setOrginaleValues();
        });
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

}
