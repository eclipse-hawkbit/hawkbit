/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.dnd.DropTargetExtension;
import com.vaadin.ui.dnd.event.DropEvent;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.TargetTypeInUseException;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetTypeToProxyTargetTypeMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractTargetTypeFilterButtons;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.AssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetsToNoTargetTypeAssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetsToTargetTypeAssignmentSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityDraggingListener;
import org.eclipse.hawkbit.ui.management.targettag.targettype.TargetTypeWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Target Type filter buttons table.
 */
@SuppressWarnings("squid:S2160")
public class TargetTypeFilterButtons extends AbstractTargetTypeFilterButtons {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TargetTypeFilterButtons.class);

    private final transient TargetManagement targetManagement;
    private final transient TargetTypeManagement targetTypeManagement;
    private final transient TargetTypeWindowBuilder targetTypeWindowBuilder;
    private final transient CommonUiDependencies uiDependencies;
    private transient EntityDraggingListener draggingListener;

    TargetTypeFilterButtons(final CommonUiDependencies uiDependencies,
                            final TargetTypeManagement targetTypeManagement, final TargetManagement targetManagement, final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
                            final TargetTypeWindowBuilder targetTypeWindowBuilder) {
        super(uiDependencies, targetTagFilterLayoutUiState, targetTypeManagement);

        this.targetManagement = targetManagement;
        this.targetTypeManagement = targetTypeManagement;
        this.targetTypeWindowBuilder = targetTypeWindowBuilder;
        this.uiDependencies = uiDependencies;

        final Map<String, AssignmentSupport<?, ProxyTargetType>> sourceTargetAssignmentStrategies = new HashMap<>();
        final TargetsToTargetTypeAssignmentSupport targetsToTargetTypeAssignment = new TargetsToTargetTypeAssignmentSupport(uiDependencies,
                targetManagement);

        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.TARGET_TABLE_ID, targetsToTargetTypeAssignment);

        setDropSupportToNoType();
        setDragAndDropSupportSupport(new DragAndDropSupport<>(this, i18n, uiNotification,
                sourceTargetAssignmentStrategies, eventBus));
        getDragAndDropSupportSupport().ignoreSelection(true);
        getDragAndDropSupportSupport().addDragAndDrop();

        init();
        setDataProvider(
                new TargetTypeDataProvider<>(targetTypeManagement, new TargetTypeToProxyTargetTypeMapper<>()));
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

        final Long clickedTargetTypeId = getFilterButtonClickBehaviour().getPreviouslyClickedFilterId();

        if (clickedTargetTypeId != null && clickedTargetTypeId.equals(targetTypeToDeleteId)) {
            uiNotification.displayValidationError(i18n.getMessage("message.targettype.delete", targetTypeToDeleteName));
        } else {
            return deleteTargetType(targetTypeToDelete);
        }
        return false;
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
    protected boolean deleteTargetType(ProxyTargetType targetTypeToDelete) {
        try{
            targetTypeManagement.delete(targetTypeToDelete.getId());
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                    new EntityModifiedEventPayload(EntityModifiedEventPayload.EntityModifiedEventType.ENTITY_REMOVED, getFilterMasterEntityType(),
                            ProxyTargetType.class, targetTypeToDelete.getId()));
            return true;
        } catch (TargetTypeInUseException exception){
            LOG.trace("Target type already in use exception: {}", exception.getMessage());
            uiNotification.displayValidationError(i18n.getMessage(exception.getMessage()));
        }

        return false;
    }

    @Override
    protected boolean isDeletionAllowed() {
        return permissionChecker.hasDeleteTargetPermission();
    }

    @Override
    protected boolean isEditAllowed() {
        return permissionChecker.hasUpdateRepositoryPermission();
    }

    private void setDropSupportToNoType() {
        final TargetsToNoTargetTypeAssignmentSupport targetsToNoTargetTypeAssignmentSupport = new TargetsToNoTargetTypeAssignmentSupport(
                uiDependencies, targetManagement);

        final DropTargetExtension<Button> dropExtension = new DropTargetExtension<>(getNoTargetTypeButton());

        dropExtension.addDropListener(event -> {
            List<ProxyTarget> droppedTargets = getDroppedTargets(event);
            targetsToNoTargetTypeAssignmentSupport.assignSourceItemsToTargetItem(droppedTargets, null);
        });
        addDropStylingListener();

    }

    private void addDropStylingListener() {
        if (draggingListener == null) {
            draggingListener = new EntityDraggingListener(eventBus,
                    Collections.singletonList(UIComponentIdProvider.TARGET_TABLE_ID), getNoTargetTypeButton());
        }

        draggingListener.subscribe();
    }

    private static List<ProxyTarget> getDroppedTargets(final DropEvent<?> dropEvent) {
        final List<ProxyTarget> list = new ArrayList<>();
        dropEvent.getDragSourceExtension().ifPresent(dragSource -> {
            final Object dragData = dragSource.getDragData();
            if (dragData instanceof ProxyTarget) {
                list.add((ProxyTarget) dragData);
            }
            if (dragData instanceof List
                    && ((List<?>) dragData).stream().allMatch(element -> element instanceof ProxyTarget)) {
                list.addAll(((List<?>) dragData).stream().map(element -> (ProxyTarget) element)
                        .collect(Collectors.toList()));
            }
        });
        return list;
    }

}
