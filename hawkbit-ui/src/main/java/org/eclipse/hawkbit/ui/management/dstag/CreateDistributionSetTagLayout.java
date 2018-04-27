/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when creating a distribution set
 * tag on the Deployment View.
 */
public class CreateDistributionSetTagLayout extends AbstractTagLayout<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    CreateDistributionSetTagLayout(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
        init();
    }

    @Override
    protected void saveEntity() {
        createNewTag();
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(getTagName().getValue());
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        String colour = ColorPickerConstants.START_COLOR.getCSS();
        if (!StringUtils.isEmpty(getColorPicked())) {
            colour = getColorPicked();
        }

        final DistributionSetTag newDistTag = distributionSetTagManagement.create(getEntityFactory().tag().create()
                .name(getTagName().getValue()).description(getTagDesc().getValue()).colour(colour));
        getEventBus().publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newDistTag));
        displaySuccess(newDistTag.getName());
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.new"),
                getI18n().getMessage("caption.tag"));
    }

}
