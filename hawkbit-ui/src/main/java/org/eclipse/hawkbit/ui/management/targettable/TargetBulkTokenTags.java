/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Map;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTargetTagToken;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Target tag layout in bulk upload popup.
 *
 */
public class TargetBulkTokenTags extends AbstractTargetTagToken {
    private static final long serialVersionUID = 4159616629565523717L;

    private static final int MAX_TAGS = 500;

    TargetBulkTokenTags(final SpPermissionChecker checker, final VaadinMessageSource i18n, final UINotification uinotification,
            final UIEventBus eventBus, final ManagementUIState managementUIState, final TargetTagManagement tagManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState, tagManagement);
    }

    @Override
    protected void assignTag(final String tagNameSelected) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().add(tagNameSelected);

    }

    @Override
    protected void unassignTag(final String tagName) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().remove(tagName);
    }

    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.getMessage("combo.type.tag.name");
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasCreateTargetPermission();
    }

    @Override
    public void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        addAlreadySelectedTags();
    }

    protected void addAlreadySelectedTags() {
        for (final String tagName : managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames()) {
            tagManagement.getByName(tagName).map(TargetTag::getId).ifPresent(this::addNewToken);
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        tagDetails.clear();
        for (final TargetTag tag : tagManagement.findAll(new PageRequest(0, MAX_TAGS))) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }

    }

    public Map<Long, TagData> getTokensAdded() {
        return tokensAdded;
    }
}
