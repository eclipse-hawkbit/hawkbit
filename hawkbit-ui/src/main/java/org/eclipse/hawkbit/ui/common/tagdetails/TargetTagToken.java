/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.eventbus.event.TargetTagAssigmentResultEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Implementation of Target tag token.
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetTagToken extends AbstractTargetTagToken {

    private static final long serialVersionUID = 7124887018280196721L;

    // To Be Done : have to set this value based on view???
    private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;

    @Autowired
    private UINotification uinotification;

    @Autowired
    private transient TargetManagement targetManagement;

    private Target selectedTarget;

    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    @Override
    protected void assignTag(final String tagNameSelected) {
        if (tagNameSelected != null) {
            final TargetTagAssigmentResult result = toggleAssignment(tagNameSelected);
            if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_TARGET_TAG);
            }
        } else {
            uinotification.displayValidationError(i18n.get("message.error.missing.tagname"));
        }
    }

    private TargetTagAssigmentResult toggleAssignment(final String tagNameSelected) {
        final Set<String> targetList = new HashSet<>();
        targetList.add(selectedTarget.getControllerId());
        final TargetTagAssigmentResult result = targetManagement.toggleTagAssignment(targetList, tagNameSelected);
        uinotification.displaySuccess(HawkbitCommonUtil.getTargetTagAssigmentMsg(tagNameSelected, result, i18n));
        return result;
    }

    @Override
    protected void unassignTag(final String tagName) {
        final TargetTagAssigmentResult result = toggleAssignment(tagName);
        if (result.getUnassigned() >= 1 && (isClickedTagListEmpty() || getClickedTagList().contains(tagName))) {
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_TARGET_TAG);
        }
    }

    private Boolean isClickedTagListEmpty() {
        if (getClickedTagList() == null || getClickedTagList() != null && !getClickedTagList().isEmpty()) {
            return true;
        }
        return false;
    }

    /* To Be Done : this implementation will vary in views */
    private List<String> getClickedTagList() {
        return new ArrayList<>();
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateTargetPermission();
    }

    @Override
    protected void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        if (selectedTarget != null) {
            for (final TargetTag tag : selectedTarget.getTags()) {
                addNewToken(tag.getId());
            }
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetTagUpdateEvent(final TargetTagUpdateEvent event) {
        final TargetTag entity = event.getEntity();
        final Item item = container.getItem(entity.getId());
        if (item != null) {
            updateItem(entity.getName(), entity.getColour(), item);
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetTagAssigmentResultEvent(final TargetTagAssigmentResultEvent event) {
        final TargetTagAssigmentResult assignmentResult = event.getAssigmentResult();
        final TargetTag targetTag = assignmentResult.getTargetTag();
        if (isAssign(assignmentResult)) {
            addNewToken(targetTag.getId());
        } else if (isUnassign(assignmentResult)) {
            removeTokenItem(targetTag.getId(), targetTag.getName());
        }

    }

    protected boolean isAssign(final TargetTagAssigmentResult assignmentResult) {
        if (assignmentResult.getAssigned() > 0) {
            final List<String> assignedTargetNames = assignmentResult.getAssignedTargets().stream()
                    .map(t -> t.getControllerId()).collect(Collectors.toList());
            if (assignedTargetNames.contains(managementUIState.getLastSelectedTargetIdName().getControllerId())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isUnassign(final TargetTagAssigmentResult assignmentResult) {
        if (assignmentResult.getUnassigned() > 0) {
            final List<String> unassignedTargetNamesList = assignmentResult.getUnassignedTargets().stream()
                    .map(t -> t.getControllerId()).collect(Collectors.toList());
            if (unassignedTargetNamesList.contains(managementUIState.getLastSelectedTargetIdName().getControllerId())) {
                return true;
            }
        }
        return false;
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent targetTableEvent) {
        if (targetTableEvent.getTargetComponentEvent() == TargetComponentEvent.SELECTED_TARGET
                && targetTableEvent.getTarget() != null) {
            ui.access(() -> {
                /**
                 * targetTableEvent.getTarget() is null when table has no data.
                 */
                selectedTarget = targetTableEvent.getTarget();
                repopulateToken();
            });
        }
    }

}
