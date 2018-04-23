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

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Class for Update Tag Layout of distribution set
 */
public class DeleteTargetTagLayoutWindow extends UpdateTargetTagLayoutWindow {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

    private List<String> selectedTags;

    DeleteTargetTagLayoutWindow(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(i18n, targetTagManagement, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
    }

    @Override
    public void init() {
        super.init();
        comboLabel.setValue(i18n.getMessage("label.choose.tag", i18n.getMessage("label.choose.tag.delete")));
        contentLayout.removeComponent(colorLabelLayout);
    }

    @Override
    protected void setTagDetails(final String distTagSelected) {
        super.setTagDetails(distTagSelected);
        tagDesc.setEnabled(false);
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.configure", i18n.getMessage("caption.delete"), i18n.getMessage("caption.tag"));
    }

    @Override
    protected void saveEntity() {
        if (canBeDeleted()) {
            deleteTargetTag();
        }
    }

    private void deleteTargetTag() {
        final String tagNameToDelete = tagName.getValue();
        final Optional<TargetTag> tagToDelete = targetTagManagement.getByName(tagNameToDelete);
        tagToDelete.ifPresent(tag -> {
            if (selectedTags.contains(tagNameToDelete)) {
                uiNotification.displayValidationError(i18n.getMessage("message.tag.delete", tagNameToDelete));
            } else {
                targetTagManagement.delete(tagNameToDelete);
                eventBus.publish(this, new TargetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, tag));
                uiNotification.displaySuccess(i18n.getMessage("message.delete.success", tagName));
            }
        });
    }

    private boolean canBeDeleted() {
        if (!permChecker.hasDeleteRepositoryPermission()) {
            uiNotification.displayValidationError(
                    i18n.getMessage("message.permission.insufficient", SpPermission.DELETE_REPOSITORY));
            return false;
        }
        return true;
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

    @Override
    public CommonDialogWindow getWindow() {
        reset();
        return new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(getWindowCaption()).content(this)
                .cancelButtonClickListener(event -> discard()).layout(mainLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
    }

    /**
     * 
     * Save or update the entity.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {

        @Override
        public void saveOrUpdate() {
            saveEntity();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return true;
        }

    }

    public List<String> getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(final List<String> selectedTags) {
        this.selectedTags = selectedTags;
    }

}
