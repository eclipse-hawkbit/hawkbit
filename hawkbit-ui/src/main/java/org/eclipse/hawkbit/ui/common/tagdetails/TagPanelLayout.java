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

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.VerticalLayout;

public class TagPanelLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final TagListField assignedTagField;
    private final TagAssignementComboBox assignableTagsComboBox;

    public TagPanelLayout(final VaadinMessageSource i18n, final boolean readOnlyMode) {

        assignableTagsComboBox = new TagAssignementComboBox(i18n, readOnlyMode);
        addComponent(assignableTagsComboBox);

        assignedTagField = new TagListField(i18n, readOnlyMode);
        addComponent(assignedTagField);
        setExpandRatio(assignedTagField, 1f);
        setExpandRatio(assignableTagsComboBox, 0f);
    }

    public void initializeTags(final List<TagData> allTags, final List<TagData> assignedTags) {
        assignableTagsComboBox.removeAllTags();
        assignedTagField.removeAllTags();

        allTags.forEach(assignableTagsComboBox::addAssignableTag);
        assignedTags.forEach(this::setAssignedTag);
    }

    public void setAssignedTag(final TagData tagData) {
        // the assigned tag is no longer assignable
        assignableTagsComboBox.removeAssignableTag(tagData);
        // show it as an assigned tag
        assignedTagField.addTag(tagData.getName(), tagData.getColor());
    }

    public void removeAssignedTag(final TagData tagData) {
        // the un-assigned tag is now assignable
        assignableTagsComboBox.addAssignableTag(tagData);
        // remove ot from the assigned tags
        assignedTagField.removeTag(tagData.getName());
    }

    public void tagCreated(final TagData tagData) {
        assignableTagsComboBox.addAssignableTag(tagData);
    }

    public void tagDeleted(final TagData tagData) {
        assignableTagsComboBox.removeAssignableTag(tagData);
        assignedTagField.removeTag(tagData.getName());
    }

    public interface TagAssignmentListener {

        public void assignTag(String tagName);

        public void unassignTag(String tagName);
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.addTagAssignmentListener(listener);
        assignedTagField.addTagAssignmentListener(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.removeTagAssignmentListener(listener);
        assignedTagField.removeTagAssignmentListener(listener);
    }

    public List<String> getAssignedTags() {
        return assignedTagField.getTags();
    }
}
