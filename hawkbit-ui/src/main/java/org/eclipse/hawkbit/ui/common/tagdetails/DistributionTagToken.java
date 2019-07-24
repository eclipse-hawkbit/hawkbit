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
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Implementation of target/ds tag token layout.
 *
 */
public class DistributionTagToken extends AbstractTagToken<DistributionSet> {

    private static final long serialVersionUID = -8022738301736043396L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    public DistributionTagToken(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState,
            final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState);
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected void assignTag(final TagData tagData) {
        final List<DistributionSet> assignedDistributionSets = distributionSetManagement
                .assignTag(Sets.newHashSet(selectedEntity.getId()), tagData.getId());
        if (checkAssignmentResult(assignedDistributionSets, managementUIState.getLastSelectedDsIdName())) {
            uinotification.displaySuccess(
                    i18n.getMessage("message.target.assigned.one", selectedEntity.getName(), tagData.getName()));
            eventBus.publish(this, ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG);
            tagPanelLayout.setAssignedTag(tagData);
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
            tagPanelLayout.removeAssignedTag(tagData);
        }
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateRepositoryPermission();
    }

    @Override
    protected List<TagData> getAllAssignableTags() {
        return distributionSetTagManagement.findAll(PageRequest.of(0, MAX_TAG_QUERY)).getContent().stream()
                .map(tag -> new TagData(tag.getId(), tag.getName(), tag.getColour())).collect(Collectors.toList());
    }

    @Override
    protected List<TagData> getAssignedTags() {
        if (selectedEntity != null) {
            return distributionSetTagManagement
                    .findByDistributionSet(PageRequest.of(0, MAX_TAG_QUERY), selectedEntity.getId()).getContent()
                    .stream().map(tag -> new TagData(tag.getId(), tag.getName(), tag.getColour()))
                    .collect(Collectors.toList());
        }

        return Lists.newArrayList();
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
        repopulateToken();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagUpdateEvent(final DistributionSetTagTableEvent event) {
        repopulateToken();
    }
}
