/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when deleting a distribution
 * set tag on the Deployment View.
 */
public class DeleteDistributionSetTagLayout extends AbstractDistributionSetTagLayoutForModify {

    private static final long serialVersionUID = 1L;

    private final List<String> selectedTags;

    DeleteDistributionSetTagLayout(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final List<String> selectedTags, final String selectedTagId) {
        super(i18n, distributionSetTagManagement, entityFactory, eventBus, permChecker, uiNotification, selectedTagId);
        this.selectedTags = selectedTags;
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getContentLayout().removeComponent(getColorLabelLayout());
        disableFields();
    }

    @Override
    protected void disableFields() {
        getTagDesc().setEnabled(false);
        getTagName().setEnabled(false);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.delete") + " " + getI18n().getMessage("caption.tag");
    }

    @Override
    protected void saveEntity() {
        deleteDistributionTag();
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

    @Override
    protected boolean isDeleteAction() {
        return true;
    }

    private void deleteDistributionTag() {
        final String tagNameToDelete = getTagName().getValue();
        final Optional<DistributionSetTag> tagToDelete = getDistributionSetTagManagement().getByName(tagNameToDelete);
        tagToDelete.ifPresent(tag -> {
            if (selectedTags.contains(tagNameToDelete)) {
                getUiNotification().displayValidationError(getI18n().getMessage("message.tag.delete", tagNameToDelete));
            } else {
                getDistributionSetTagManagement().delete(tagNameToDelete);
                getEventBus().publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, tag));
                getUiNotification().displaySuccess(getI18n().getMessage("message.delete.success", tagNameToDelete));
            }
        });
    }

}
