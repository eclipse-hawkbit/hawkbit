/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when deleting a target tag on the
 * Deployment View.
 */
public class DeleteTargetTagLayout extends AbstractTargetTagLayoutForModify {

    private static final long serialVersionUID = 1L;

    private final List<String> selectedTags;

    DeleteTargetTagLayout(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification, final List<String> selectedTags) {
        super(i18n, targetTagManagement, entityFactory, eventBus, permChecker, uiNotification);
        this.selectedTags = selectedTags;
    }

    @Override
    public void init() {
        super.init();
        getUpdateCombobox().getComboLabel()
                .setValue(getI18n().getMessage("label.choose.tag", getI18n().getMessage("label.choose.tag.delete")));
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        disableFields();
        getContentLayout().removeComponent(getColorLabelLayout());
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
        deleteTargetTag();
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

    @Override
    protected boolean isDeleteAction() {
        return true;
    }

    private void deleteTargetTag() {
        final String tagNameToDelete = getTagName().getValue();
        final Optional<TargetTag> tagToDelete = getTargetTagManagement().getByName(tagNameToDelete);
        tagToDelete.ifPresent(tag -> {
            if (selectedTags.contains(tagNameToDelete)) {
                getUiNotification().displayValidationError(getI18n().getMessage("message.tag.delete", tagNameToDelete));
            } else {
                getTargetTagManagement().delete(tagNameToDelete);
                getEventBus().publish(this, new TargetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, tag));
                getUiNotification().displaySuccess(getI18n().getMessage("message.delete.success", getTagName()));
            }
        });
    }

}
