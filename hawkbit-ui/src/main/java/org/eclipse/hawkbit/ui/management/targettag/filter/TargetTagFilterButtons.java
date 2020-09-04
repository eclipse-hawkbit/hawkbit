/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import java.util.Collections;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.mappers.TagToProxyTagMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTagDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractTagFilterButtons;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetsToTagAssignmentSupport;
import org.eclipse.hawkbit.ui.management.targettag.TargetTagWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Target Tag filter buttons table.
 */
public class TargetTagFilterButtons extends AbstractTagFilterButtons {
    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;
    private final transient TargetTagWindowBuilder targetTagWindowBuilder;

    TargetTagFilterButtons(final VaadinMessageSource i18n, final UIEventBus eventBus, final UINotification notification,
            final SpPermissionChecker permChecker, final TargetTagManagement targetTagManagement,
            final TargetManagement targetManagement, final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
            final TargetTagWindowBuilder targetTagWindowBuilder) {
        super(eventBus, i18n, notification, permChecker, targetTagFilterLayoutUiState);

        this.targetTagManagement = targetTagManagement;
        this.targetTagWindowBuilder = targetTagWindowBuilder;

        final TargetsToTagAssignmentSupport targetsToTagAssignment = new TargetsToTagAssignmentSupport(notification,
                i18n, eventBus, permChecker, targetManagement);

        setDragAndDropSupportSupport(new DragAndDropSupport<>(this, i18n, notification,
                Collections.singletonMap(UIComponentIdProvider.TARGET_TABLE_ID, targetsToTagAssignment), eventBus));
        getDragAndDropSupportSupport().ignoreSelection(true);
        getDragAndDropSupportSupport().addDragAndDrop();

        init();
        setDataProvider(new TargetTagDataProvider(targetTagManagement, new TagToProxyTagMapper<>()));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.TARGET_TAG_TABLE_ID;
    }

    @Override
    protected String getMessageKeyEntityTypeSing() {
        return UIMessageIdProvider.CAPTION_TARGET_TAG;
    }

    @Override
    protected String getMessageKeyEntityTypePlur() {
        return "caption.entity.target.tags";
    }

    @Override
    protected String getFilterButtonIdPrefix() {
        return UIComponentIdProvider.TARGET_TAG_ID_PREFIXS;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getFilterMasterEntityType() {
        return ProxyTarget.class;
    }

    @Override
    protected EventView getView() {
        return EventView.DEPLOYMENT;
    }

    @Override
    protected void deleteTag(final ProxyTag tagToDelete) {
        targetTagManagement.delete(tagToDelete.getName());
    }

    @Override
    protected boolean isDeletionAllowed() {
        return permissionChecker.hasDeleteTargetPermission();
    }

    @Override
    protected boolean isEditAllowed() {
        return permissionChecker.hasUpdateRepositoryPermission();
    }

    @Override
    protected Window getUpdateWindow(final ProxyTag clickedFilter) {
        return targetTagWindowBuilder.getWindowForUpdate(clickedFilter);
    }
}
