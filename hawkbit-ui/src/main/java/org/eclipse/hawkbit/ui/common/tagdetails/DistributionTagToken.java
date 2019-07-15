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
import java.util.List;
import java.util.Objects;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.push.DistributionSetTagCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.DistributionSetTagDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.DistributionSetTagUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Sets;

/**
 * Implementation of target/ds tag token layout.
 *
 */
public class DistributionTagToken extends AbstractTagToken<DistributionSet> {

    private static final long serialVersionUID = -8022738301736043396L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    // To Be Done : have to set this value based on view???
    // private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;

    public DistributionTagToken(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState,
            final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState);
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected String getTagStyleName() {
        return "distribution-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.getMessage("combo.type.tag.name");
    }

    // @Override
    // public void assignTag(final String tagName) {
    // if (tagName != null) {
    // final DistributionSetTagAssignmentResult result =
    // toggleAssignment(tagName);
    // if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
    // eventBus.publish(this, ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG);
    // }
    // } else {
    // uinotification.displayValidationError(i18n.getMessage("message.error.missing.tagname"));
    // }
    // }

    //
    // @Override
    // public void unassignTag(final String tagName) {
    // final DistributionSetTagAssignmentResult result =
    // toggleAssignment(tagName);
    // if (result.getUnassigned() >= 1) {
    // eventBus.publish(this, ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG);
    // }
    // }
    //
    // private DistributionSetTagAssignmentResult toggleAssignment(final String
    // tagNameSelected) {
    // final DistributionSetTagAssignmentResult result =
    // distributionSetManagement
    // .toggleTagAssignment(Sets.newHashSet(selectedEntity.getId()),
    // tagNameSelected);
    // processTargetTagAssigmentResult(result);
    // uinotification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(tagNameSelected,
    // result, i18n));
    // return result;
    // }

    @Override
    protected void assignTag(final TagData tagData) {
        final List<DistributionSet> assignedDistributionSets = distributionSetManagement
                .assignTag(Sets.newHashSet(selectedEntity.getId()), tagData.getId());
        if (checkAssignmentResult(assignedDistributionSets, managementUIState.getLastSelectedDsIdName())) {
            uinotification.displaySuccess(
                    i18n.getMessage("message.target.assigned.one", selectedEntity.getName(), tagData.getName()));
            eventBus.publish(this, ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG);
            tagPanel.setAssignedTag(tagData);
        }
    }

    @Override
    protected void unassignTag(final TagData tagData) {
        final DistributionSet unAssignedDistributionSet = distributionSetManagement.unAssignTag(selectedEntity.getId(),
                tagData.getId());
        if (checkUnassignmentResult(unAssignedDistributionSet, managementUIState.getLastSelectedDsIdName())) {
            uinotification.displaySuccess(
                    i18n.getMessage("message.target.unassigned.one", selectedEntity.getName(), tagData.getName()));
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG);
            tagPanel.removeAssignedTag(tagData);
        }
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateRepositoryPermission();
    }

    // @Override
    // public void displayAlreadyAssignedTags() {
    // // removePreviouslyAddedTokens();
    // if (selectedEntity != null) {
    // distributionSetTagManagement.findByDistributionSet(PageRequest.of(0,
    // MAX_TAG_QUERY), selectedEntity.getId())
    // .getContent().stream().forEach(tag -> addNewToken(tag.getId(),
    // tag.getName(), tag.getColour()));
    // }
    // }

    // @Override
    // protected void populateContainer() {
    // // container.removeAllItems();
    // tagPanel.removeAllTokens();
    // tagDetailsById.clear();
    // distributionSetTagManagement.findAll(PageRequest.of(0,
    // MAX_TAG_QUERY)).getContent().stream()
    // .forEach(tag -> setContainerPropertValues(tag.getId(), tag.getName(),
    // tag.getColour()));
    // }

    @Override
    protected List<TagData> getAllAssignableTags() {
        final List<TagData> allTags = new ArrayList<>();
        distributionSetTagManagement.findAll(PageRequest.of(0, MAX_TAG_QUERY)).getContent().stream()
                .forEach(tag -> allTags.add(new TagData(tag.getId(), tag.getName(), tag.getColour())));
        return allTags;
    }

    @Override
    protected List<TagData> getAssignedTags() {
        final List<TagData> assignedTags = new ArrayList<>();
        if (selectedEntity != null) {
            distributionSetTagManagement.findByDistributionSet(PageRequest.of(0, MAX_TAG_QUERY), selectedEntity.getId())
                    .getContent().stream()
                    .forEach(tag -> assignedTags.add(new TagData(tag.getId(), tag.getName(), tag.getColour())));
        }
        return assignedTags;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagCreatedEvent(final DistributionSetTagCreatedEventContainer eventContainer) {
        eventContainer.getEvents().stream().filter(Objects::nonNull).map(DistributionSetTagCreatedEvent::getEntity)
                .forEach(tag -> tagCreated(new TagData(tag.getId(), tag.getName(), tag.getColour())));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagDeletedEvent(final DistributionSetTagDeletedEventContainer eventContainer) {
        eventContainer.getEvents().stream().map(event -> getTagIdByTagName(event.getEntityId()))
                .forEach(this::tagDeleted);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagUpdateEvent(final DistributionSetTagUpdatedEventContainer eventContainer) {
        eventContainer.getEvents().stream().filter(Objects::nonNull).map(DistributionSetTagUpdatedEvent::getEntity)
                .forEach(tag -> tagUpdated(new TagData(tag.getId(), tag.getName(), tag.getColour())));
    }

    // private void processTargetTagAssigmentResult(final
    // DistributionSetTagAssignmentResult assignmentResult) {
    // final DistributionSetTag tag = assignmentResult.getDistributionSetTag();
    // if (isAssign(assignmentResult)) {
    // addNewToken(tag.getId(), tag.getName(), tag.getColour());
    // } else if (isUnassign(assignmentResult)) {
    // removeTokenItem(tag.getId(), tag.getName());
    // }
    // }

    // protected boolean isAssign(final DistributionSetTagAssignmentResult
    // assignmentResult) {
    // if (assignmentResult.getAssigned() > 0 &&
    // managementUIState.getLastSelectedDsIdName() != null) {
    // final List<Long> assignedDsNames =
    // assignmentResult.getAssignedEntity().stream().map(t -> t.getId())
    // .collect(Collectors.toList());
    // if
    // (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName())) {
    // return true;
    // }
    // }
    // return false;
    // }

    // protected boolean isUnassign(final DistributionSetTagAssignmentResult
    // assignmentResult) {
    // if (assignmentResult.getUnassigned() > 0 &&
    // managementUIState.getLastSelectedDsIdName() != null) {
    // final List<Long> assignedDsNames =
    // assignmentResult.getUnassignedEntity().stream().map(t -> t.getId())
    // .collect(Collectors.toList());
    // if
    // (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName())) {
    // return true;
    // }
    // }
    // return false;
    // }

}
