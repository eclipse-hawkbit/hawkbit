/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when creating a target tag on the
 * Deployment View.
 */
public class CreateTargetTagLayout extends AbstractTargetTagLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for CreateUpdateTargetTagLayoutWindow
     * 
     * @param i18n
     *            I18N
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
     */
    public CreateTargetTagLayout(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, targetTagManagement);
    }

    private void createNewTag() {
        if (!StringUtils.isEmpty(getTagName().getValue())) {
            setColorPicked(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
            String colour = ColorPickerConstants.START_COLOR.getCSS();
            if (!StringUtils.isEmpty(getColorPicked())) {
                colour = getColorPicked();
            }

            final TargetTag newTargetTag = getTargetTagManagement().create(getEntityFactory().tag().create()
                    .name(getTagName().getValue()).description(getTagDesc().getValue()).colour(colour));
            getEventBus().publish(this, new TargetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newTargetTag));
            displaySuccess(newTargetTag.getName());
        } else {
            displayValidationError(getI18n().getMessage(getMessageErrorMissingTagname()));
        }
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.create.new", getI18n().getMessage("caption.tag"));
    }

    @Override
    protected void saveEntity() {
        createNewTag();
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

}
