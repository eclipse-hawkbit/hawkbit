/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetTypeToProxyTargetTypeMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractTargetTypeFilterButtons;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetsToTargetTypeAssignmentSupport;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.management.targettag.targettype.TargetTypeWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import java.util.Collection;
import org.springframework.util.CollectionUtils;

/**
 * Target Type filter buttons table.
 */
public class TargetTypeFilterButtons extends AbstractTargetTypeFilterButtons {
    private static final long serialVersionUID = 1L;

    private final transient TargetTypeManagement targetTypeManagement;
    private final transient TargetTypeWindowBuilder targetTypeWindowBuilder;

    TargetTypeFilterButtons(final CommonUiDependencies uiDependencies,
                            final TargetTypeManagement targetTypeManagement, final TargetManagement targetManagement, final TagFilterLayoutUiState tagFilterLayoutUiState,
                            final TargetTypeWindowBuilder targetTypeWindowBuilder) {
        super(uiDependencies, tagFilterLayoutUiState);

        this.targetTypeManagement = targetTypeManagement;
        this.targetTypeWindowBuilder = targetTypeWindowBuilder;

        final TargetsToTargetTypeAssignmentSupport targetsToTargetTypeAssignment = new TargetsToTargetTypeAssignmentSupport(uiDependencies,
                targetManagement);

        setDragAndDropSupportSupport(new DragAndDropSupport<>(this, i18n, uiNotification,
                Collections.singletonMap(UIComponentIdProvider.TARGET_TABLE_ID, targetsToTargetTypeAssignment), eventBus));
        getDragAndDropSupportSupport().ignoreSelection(true);
        getDragAndDropSupportSupport().addDragAndDrop();

        init();
        setDataProvider(
                new TargetTypeDataProvider(targetTypeManagement, new TargetTypeToProxyTargetTypeMapper<>()));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.TARGET_TYPE_TABLE_ID;
    }

    @Override
    protected String getMessageKeyEntityTypeSing() {
        return "caption.entity.target.type";
    }

    @Override
    protected String getMessageKeyEntityTypePlur() {
        return "caption.entity.target.types";
    }

    @Override
    protected boolean deleteFilterButtons(Collection<ProxyTargetType> filterButtonsToDelete) {
        final ProxyTargetType targetTypeToDelete = filterButtonsToDelete.iterator().next();
        final String targetTypeToDeleteName = targetTypeToDelete.getName();
        final Long targetTypeToDeleteId = targetTypeToDelete.getId();

        final Set<Long> clickedTypeIds = getFilterButtonClickBehaviour().getPreviouslyClickedFilterIds();

        if (!CollectionUtils.isEmpty(clickedTypeIds) && clickedTypeIds.contains(targetTypeToDeleteId)) {
            uiNotification.displayValidationError(i18n.getMessage("message.targettype.delete", targetTypeToDeleteName));
            return false;
        } else {
            deleteTag(targetTypeToDelete);

            eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                    new EntityModifiedEventPayload(EntityModifiedEventPayload.EntityModifiedEventType.ENTITY_REMOVED, getFilterMasterEntityType(),
                            ProxyTargetType.class, targetTypeToDeleteId));
            return true;
        }
    }

    @Override
    protected String getFilterButtonIdPrefix() {
        return UIComponentIdProvider.TARGET_TYPE_ID_PREFIXS;
    }

    @Override
    protected void editButtonClickListener(ProxyTargetType clickedFilter) {
        final Window updateWindow = targetTypeWindowBuilder.getWindowForUpdate(clickedFilter);

        updateWindow.setCaption(i18n.getMessage("caption.update", i18n.getMessage("caption.type")));
        UI.getCurrent().addWindow(updateWindow);
        updateWindow.setVisible(Boolean.TRUE);
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
    protected void deleteTag(ProxyTargetType tagToDelete) {
            targetTypeManagement.delete(tagToDelete.getId());
    }

    @Override
    protected Window getUpdateWindow(ProxyTag clickedFilter) {
        return null;
    }

    @Override
    protected Collection<Long> filterExistingTagIds(Collection<Long> tagIds) {
        return targetTypeManagement.get(tagIds).stream().map(Identifiable::getId).collect(Collectors.toSet());
    }

    @Override
    protected boolean isDeletionAllowed() {
        return permissionChecker.hasDeleteTargetPermission();
    }

    @Override
    protected boolean isEditAllowed() {
        return permissionChecker.hasUpdateRepositoryPermission();
    }

}
