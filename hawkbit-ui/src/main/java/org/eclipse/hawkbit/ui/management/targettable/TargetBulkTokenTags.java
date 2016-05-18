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

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTargetTagToken;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Target tag layout in bulk upload popup.
 *
 */
@SpringComponent
@ViewScope
public class TargetBulkTokenTags extends AbstractTargetTagToken {
    private static final long serialVersionUID = 4159616629565523717L;

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.TargetTagToken#assignTag(java.
     * lang.String)
     */
    @Override
    protected void assignTag(final String tagNameSelected) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().add(tagNameSelected);

    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.TargetTagToken#unassignTag(java.
     * lang.String)
     */
    @Override
    protected void unassignTag(final String tagName) {
        managementUIState.getTargetTableFilters().getBulkUpload().getAssignedTagNames().remove(tagName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.AbstractTagToken#getTagStyleName
     * ()
     */
    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.tagdetails.AbstractTagToken#
     * hasUpdatePermission()
     */
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
            addNewToken(tagManagement.findTargetTag(tagName).getId());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.tagdetails.AbstractTagToken#
     * populateContainer()
     */
    @Override
    protected void populateContainer() {
        container.removeAllItems();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }

    }

    public Map<Long, TagData> getTokensAdded() {
        return tokensAdded;
    }
}
