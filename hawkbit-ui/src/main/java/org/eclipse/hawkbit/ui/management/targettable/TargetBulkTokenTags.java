/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    TargetBulkTokenTags(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState,
            final TargetTagManagement tagManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState, tagManagement);
    }

    // @Override
    // protected void assignTag(final String tagNameSelected) {
    // managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().add(tagNameSelected);
    // }
    //
    // @Override
    // protected void unassignTag(final String tagName) {
    // managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().remove(tagName);
    // }

    @Override
    protected void assignTag(final TagData tagData) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().add(tagData.getName());
    }

    @Override
    protected void unassignTag(final TagData tagData) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().remove(tagData.getName());
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

    // @Override
    // public void displayAlreadyAssignedTags() {
    // // removePreviouslyAddedTokens();
    // addAlreadySelectedTags();
    // }
    //
    // protected void addAlreadySelectedTags() {
    // for (final String tagName :
    // managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames())
    // {
    // //
    // tagManagement.getByName(tagName).map(TargetTag::getId).ifPresent(this::addNewToken);
    // final Optional<TargetTag> byName = tagManagement.getByName(tagName);
    // if (byName.isPresent()) {
    // final TargetTag targetTag = byName.get();
    // addNewToken(targetTag.getId(), targetTag.getName(),
    // targetTag.getColour());
    // }
    // }
    // }

    // @Override
    // protected void populateContainer() {
    // // container.removeAllItems();
    // tagPanel.removeAllTokens();
    // tagDetailsById.clear();
    // for (final TargetTag tag : tagManagement.findAll(PageRequest.of(0,
    // MAX_TAGS))) {
    // setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
    // }
    // }

    public void initializeTags() {
        repopulateToken();
    }

    @Override
    protected List<TagData> getAllAssignableTags() {
        final List<TagData> allTags = new ArrayList<>();
        tagManagement.findAll(PageRequest.of(0, MAX_TAGS))
                .forEach(tag -> allTags.add(new TagData(tag.getId(), tag.getName(), tag.getColour())));
        return allTags;
    }

    @Override
    protected List<TagData> getAssignedTags() {
        final List<TagData> assignedTags = new ArrayList<>();
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().forEach(tagName -> {
            final Optional<TargetTag> byName = tagManagement.getByName(tagName);
            if (byName.isPresent()) {
                final TargetTag targetTag = byName.get();
                assignedTags.add(new TagData(targetTag.getId(), targetTag.getName(), targetTag.getColour()));
            }
        });

        return assignedTags;
    }

    // public Map<Long, TagData> getTokensAdded() {
    // return tokensAdded;
    // }
}
