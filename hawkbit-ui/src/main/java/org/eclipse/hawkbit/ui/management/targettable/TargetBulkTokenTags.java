/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTargetTagToken;
import org.eclipse.hawkbit.ui.common.tagdetails.TagData;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Target tag layout in bulk upload popup.
 *
 */
public class TargetBulkTokenTags extends AbstractTargetTagToken<Target> {
    private static final long serialVersionUID = 4159616629565523717L;

    TargetBulkTokenTags(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState,
            final TargetTagManagement tagManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState, tagManagement);
    }

    @Override
    protected void assignTag(final TagData tagData) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().add(tagData.getName());
        tagPanelLayout.setAssignedTag(tagData);
    }

    @Override
    protected void unassignTag(final TagData tagData) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().remove(tagData.getName());
        tagPanelLayout.removeAssignedTag(tagData);
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasCreateTargetPermission();
    }

    /**
     * Initializes the Tags
     */
    public void initializeTags() {
        repopulateTags();
    }

    public boolean isTagSelectedForAssignment() {
        return !tagPanelLayout.getAssignedTags().isEmpty();
    }

    @Override
    protected List<TagData> getAllTags() {
        return tagManagement.findAll(PageRequest.of(0, MAX_TAG_QUERY)).stream()
                .map(tag -> new TagData(tag.getId(), tag.getName(), tag.getColour())).collect(Collectors.toList());
    }

    @Override
    protected List<TagData> getAssignedTags() {
        // this view doesn't belong to a specific target, so the current
        // selected target in the target table is ignored and therefore there
        // are no assigned tags
        return Collections.emptyList();
    }

    public List<TagData> getSelectedTagsForAssignment() {
        return tagPanelLayout.getAssignedTags().stream().map(tagDetailsByName::get).collect(Collectors.toList());
    }
}
