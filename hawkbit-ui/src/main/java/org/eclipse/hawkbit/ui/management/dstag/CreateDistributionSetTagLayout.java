/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when creating a distribution set
 * tag on the Deployment View.
 */
public class CreateDistributionSetTagLayout extends AbstractDistributionSetTagLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param distributionSetTagManagement
     *            DistributionSetTagManagement
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public CreateDistributionSetTagLayout(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, distributionSetTagManagement);
    }

    @Override
    protected void saveEntity() {
        createNewTag();
    }

    private void createNewTag() {
        if (!StringUtils.isEmpty(getTagName().getValue())) {
            setColorPicked(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
            String colour = ColorPickerConstants.START_COLOR.getCSS();
            if (!StringUtils.isEmpty(getColorPicked())) {
                colour = getColorPicked();
            }
            final DistributionSetTag newDistTag = getDistributionSetTagManagement().create(getEntityFactory().tag()
                    .create().name(getTagName().getValue()).description(getTagDesc().getValue()).colour(colour));
            getEventBus().publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newDistTag));
            displaySuccess(newDistTag.getName());
        } else {
            displayValidationError(getI18n().getMessage(getMessageErrorMissingTagname()));
        }
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.create.new", getI18n().getMessage("caption.tag"));
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

}
