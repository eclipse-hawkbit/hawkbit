/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.push.DistributionSetTagCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.DistributionSetTagDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.DistributionSetTagUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Sets;
import com.vaadin.data.Item;

/**
 * Implementation of target/ds tag token layout.
 *
 */
public class DistributionTagToken extends AbstractTagToken<DistributionSet> {

    private static final long serialVersionUID = -8022738301736043396L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    // To Be Done : have to set this value based on view???
    private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;

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

    @Override
    protected void assignTag(final String tagNameSelected) {
        if (tagNameSelected != null) {
            final DistributionSetTagAssignmentResult result = toggleAssignment(tagNameSelected);
            if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG);
            }
        } else {
            uinotification.displayValidationError(i18n.getMessage("message.error.missing.tagname"));
        }
    }

    private DistributionSetTagAssignmentResult toggleAssignment(final String tagNameSelected) {
        final DistributionSetTagAssignmentResult result = distributionSetManagement
                .toggleTagAssignment(Sets.newHashSet(selectedEntity.getId()), tagNameSelected);
        processTargetTagAssigmentResult(result);
        uinotification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(tagNameSelected, result, i18n));
        return result;
    }

    @Override
    protected void unassignTag(final String tagName) {
        final DistributionSetTagAssignmentResult result = toggleAssignment(tagName);
        if (result.getUnassigned() >= 1) {
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG);
        }
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateRepositoryPermission();
    }

    @Override
    public void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        if (selectedEntity != null) {
            distributionSetTagManagement
                    .findByDistributionSet(new PageRequest(0, MAX_TAG_QUERY), selectedEntity.getId())
                    .getContent().stream().forEach(tag -> addNewToken(tag.getId()));
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        tagDetails.clear();
        distributionSetTagManagement.findAll(new PageRequest(0, MAX_TAG_QUERY)).getContent().stream()
                .forEach(tag -> setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour()));

    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagCreatedBulkEvent(final DistributionSetTagCreatedEventContainer eventContainer) {
        eventContainer.getEvents().stream().filter(Objects::nonNull).map(DistributionSetTagCreatedEvent::getEntity)
                .forEach(distributionSetTag -> setContainerPropertValues(distributionSetTag.getId(),
                        distributionSetTag.getName(), distributionSetTag.getColour()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagDeletedEvent(final DistributionSetTagDeletedEventContainer eventContainer) {
        eventContainer.getEvents().stream().map(event -> getTagIdByTagName(event.getEntityId()))
                .forEach(this::removeTagFromCombo);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagUpdateEvent(final DistributionSetTagUpdatedEventContainer eventContainer) {
        eventContainer.getEvents().stream().filter(Objects::nonNull).map(DistributionSetTagUpdatedEvent::getEntity)
                .forEach(entity -> {
                    final Item item = container.getItem(entity.getId());
                    if (item != null) {
                        updateItem(entity.getName(), entity.getColour(), item);
                    }
                });
    }

    private void processTargetTagAssigmentResult(final DistributionSetTagAssignmentResult assignmentResult) {
        final DistributionSetTag tag = assignmentResult.getDistributionSetTag();
        if (isAssign(assignmentResult)) {
            addNewToken(tag.getId());
        } else if (isUnassign(assignmentResult)) {
            removeTokenItem(tag.getId(), tag.getName());
        }
    }

    protected boolean isAssign(final DistributionSetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getAssigned() > 0 && managementUIState.getLastSelectedDsIdName() != null) {
            final List<Long> assignedDsNames = assignmentResult.getAssignedEntity().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isUnassign(final DistributionSetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getUnassigned() > 0 && managementUIState.getLastSelectedDsIdName() != null) {
            final List<Long> assignedDsNames = assignmentResult.getUnassignedEntity().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName())) {
                return true;
            }
        }
        return false;
    }

}
