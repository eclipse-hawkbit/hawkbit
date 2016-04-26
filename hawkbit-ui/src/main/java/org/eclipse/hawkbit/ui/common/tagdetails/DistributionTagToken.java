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

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagAssigmentResultEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Implementation of target/ds tag token layout.
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagToken extends AbstractTagToken<DistributionSet> {

    private static final long serialVersionUID = -8022738301736043396L;
    @Autowired
    private transient TagManagement tagManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    // To Be Done : have to set this value based on view???
    private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;

    @Override
    protected String getTagStyleName() {
        return "distribution-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    @Override
    protected void assignTag(final String tagNameSelected) {
        if (tagNameSelected != null) {
            final DistributionSetTagAssignmentResult result = toggleAssignment(tagNameSelected);
            if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG);
            }
        } else {
            uinotification.displayValidationError(i18n.get("message.error.missing.tagname"));
        }
    }

    private DistributionSetTagAssignmentResult toggleAssignment(final String tagNameSelected) {
        final Set<Long> distributionList = new HashSet<>();
        distributionList.add(selectedEntity.getId());
        final DistributionSetTagAssignmentResult result = distributionSetManagement
                .toggleTagAssignment(distributionList, tagNameSelected);
        uinotification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(tagNameSelected, result, i18n));
        return result;
    }

    @Override
    protected void unassignTag(final String tagName) {
        final DistributionSetTagAssignmentResult result = toggleAssignment(tagName);
        if (result.getUnassigned() >= 1 && (isClickedTagListEmpty() || getClickedTagList().contains(tagName))) {
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG);
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
        return checker.hasUpdateDistributionPermission();
    }

    @Override
    public void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        if (selectedEntity != null) {
            for (final DistributionSetTag tag : selectedEntity.getTags()) {
                addNewToken(tag.getId());
            }
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        for (final DistributionSetTag tag : tagManagement.findAllDistributionSetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onDistributionSetTagCreatedBulkEvent(final DistributionSetTagCreatedBulkEvent event) {
        for (final DistributionSetTag distributionSetTag : event.getEntities()) {
            setContainerPropertValues(distributionSetTag.getId(), distributionSetTag.getName(),
                    distributionSetTag.getColour());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onDistributionSetTagDeletedEventBulkEvent(final DistributionSetTagDeletedEvent event) {
        final Long deletedTagId = getTagIdByTagName(event.getEntity().getName());
        removeTagFromCombo(deletedTagId);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onDistributionSetTagUpdateEvent(final DistributionSetTagUpdateEvent event) {
        final DistributionSetTag entity = event.getEntity();
        final Item item = container.getItem(entity.getId());
        if (item != null) {
            updateItem(entity.getName(), entity.getColour(), item);
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetTagAssigmentResultEvent(final DistributionSetTagAssigmentResultEvent event) {
        final DistributionSetTagAssignmentResult assignmentResult = event.getAssigmentResult();
        final DistributionSetTag tag = assignmentResult.getDistributionSetTag();
        if (isAssign(assignmentResult)) {
            addNewToken(tag.getId());
        } else if (isUnassign(assignmentResult)) {
            removeTokenItem(tag.getId(), tag.getName());
        }

    }

    protected boolean isAssign(final DistributionSetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getAssigned() > 0) {
            final List<Long> assignedDsNames = assignmentResult.getAssignedEntity().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName().getId())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isUnassign(final DistributionSetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getUnassigned() > 0) {
            final List<Long> assignedDsNames = assignmentResult.getUnassignedEntity().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName().getId())) {
                return true;
            }
        }
        return false;
    }

}
