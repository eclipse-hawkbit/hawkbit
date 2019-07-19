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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.VerticalLayout;

/**
 * A layout that shows a combobox with available tags that can be assigned to a
 * {@link Target} or {@link DistributionSet}. The layout also shows all already
 * assigned tags. This tags can also be removed.
 */
public class TagPanelLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final TagListField assignedTagField;
    private final TagAssignementComboBox assignableTagsComboBox;

    /**
     * Constructor.
     * 
     * @param i18n
     *            i18n
     * @param readOnlyMode
     *            if <code>true</code> no assignments and unassignements can be
     *            done.
     */
    TagPanelLayout(final VaadinMessageSource i18n, final boolean readOnlyMode) {

        assignableTagsComboBox = new TagAssignementComboBox(i18n, readOnlyMode);
        addComponent(assignableTagsComboBox);

        assignedTagField = new TagListField(i18n, readOnlyMode);
        addComponent(assignedTagField);
        setExpandRatio(assignedTagField, 1.0F);
        setExpandRatio(assignableTagsComboBox, 0.0F);
    }

    /**
     * Initializes the panel with all available tags and all assigned tags.
     * 
     * @param allTags
     *            all tags
     * @param assignedTags
     *            assigned tags
     */
    void initializeTags(final List<TagData> allTags, final List<TagData> assignedTags) {
        assignableTagsComboBox.removeAllTags();
        assignedTagField.removeAllTags();

        allTags.forEach(assignableTagsComboBox::addAssignableTag);
        assignedTags.forEach(this::setAssignedTag);
    }

    /**
     * Sets a tag that is assigned.
     * 
     * @param tagData
     *            the {@link TagData}
     */
    public void setAssignedTag(final TagData tagData) {
        // the assigned tag is no longer assignable
        assignableTagsComboBox.removeAssignableTag(tagData);
        // show it as an assigned tag
        assignedTagField.addTag(tagData.getName(), tagData.getColor());
    }

    /**
     * Removes an assigned tag.
     * 
     * @param tagData
     *            the {@link TagData}
     */
    public void removeAssignedTag(final TagData tagData) {
        // the un-assigned tag is now assignable
        assignableTagsComboBox.addAssignableTag(tagData);
        // remove ot from the assigned tags
        assignedTagField.removeTag(tagData.getName());
    }

    /**
     * Informs the panel that a new tag was created.
     * 
     * @param tagData
     *            the {@link TagData}
     */
    void tagCreated(final TagData tagData) {
        assignableTagsComboBox.addAssignableTag(tagData);
    }

    /**
     * Informs the panel that a tag was deleted.
     * 
     * @param tagData
     *            the {@link TagData}
     */
    public void tagDeleted(final TagData tagData) {
        assignableTagsComboBox.removeAssignableTag(tagData);
        assignedTagField.removeTag(tagData.getName());
    }

    /**
     * Callback interface if user triggers a tag assignment or tag unassignment
     * via UI controls.
     *
     */
    public interface TagAssignmentListener {

        /**
         * User triggers a tag assignment.
         * 
         * @param tagName
         *            the name of the tag that should be assigned.
         */
        public void assignTag(String tagName);

        /**
         * User triggers a tag unassignment.
         * 
         * @param tagName
         *            the name of the tag that should be unassigned.
         */
        public void unassignTag(String tagName);
    }

    /**
     * Registers a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener
     */
    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.addTagAssignmentListener(listener);
        assignedTagField.addTagAssignmentListener(listener);
    }

    /**
     * Removes a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener
     */
    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.removeTagAssignmentListener(listener);
        assignedTagField.removeTagAssignmentListener(listener);
    }

    /**
     * Returns all assigned tags.
     * 
     * @return {@link List} with tags.
     */
    public List<String> getAssignedTags() {
        return assignedTagField.getTags();
    }
}
