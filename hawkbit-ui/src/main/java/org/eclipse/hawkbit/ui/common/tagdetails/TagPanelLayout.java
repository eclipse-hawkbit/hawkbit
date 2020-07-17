/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
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
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
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
        this.assignableTagsComboBox = new TagAssignementComboBox(i18n, readOnlyMode);
        this.assignedTagField = new TagListField(i18n, readOnlyMode);

        addComponent(assignableTagsComboBox);
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
    void initializeTags(final List<ProxyTag> allTags, final List<ProxyTag> assignedTags) {
        assignableTagsComboBox.removeAllTags();
        assignedTagField.removeAllTags();

        if (CollectionUtils.isEmpty(allTags)) {
            return;
        }

        final List<ProxyTag> assignableTags = Lists.newArrayList(allTags);
        assignableTags.removeAll(assignedTags);

        assignableTagsComboBox.initializeAssignableTags(assignableTags);
        assignedTagField.initializeAssignedTags(assignedTags);

    }

    /**
     * Sets a tag that is assigned.
     * 
     * @param tagData
     *            the {@link ProxyTag}
     */
    public void setAssignedTag(final ProxyTag tagData) {
        // the assigned tag is no longer assignable
        assignableTagsComboBox.removeAssignableTag(tagData);
        // show it as an assigned tag
        assignedTagField.addTag(tagData);
    }

    /**
     * Removes an assigned tag.
     * 
     * @param tagData
     *            the {@link ProxyTag}
     */
    public void removeAssignedTag(final ProxyTag tagData) {
        // the un-assigned tag is now assignable
        assignableTagsComboBox.addAssignableTag(tagData);
        // remove it from the assigned tags
        assignedTagField.removeTag(tagData);
    }

    /**
     * Informs the panel that a new tag was created.
     * 
     * @param tagData
     *            the {@link ProxyTag}
     */
    void tagCreated(final ProxyTag tagData) {
        assignableTagsComboBox.addAssignableTag(tagData);
    }

    /**
     * Informs the panel that a tag was updated.
     * 
     * @param tagData
     *            the {@link ProxyTag}
     */
    void tagUpdated(final ProxyTag tagData) {
        assignableTagsComboBox.updateAssignableTag(tagData);
        assignedTagField.updateTag(tagData);
    }

    /**
     * Informs the panel that a tag was deleted.
     * 
     * @param tagId
     *            the tag Id
     */
    void tagDeleted(final Long tagId) {
        assignableTagsComboBox.removeAssignableTag(tagId);
        assignedTagField.removeTag(tagId);
    }

    /**
     * Callback interface if user triggers a tag assignment or tag unassignment
     * via UI controls.
     *
     */
    interface TagAssignmentListener {

        /**
         * User triggers a tag assignment.
         * 
         * @param tagData
         *            the tag that should be assigned.
         */
        void assignTag(ProxyTag tagData);

        /**
         * User triggers a tag unassignment.
         * 
         * @param tagData
         *            the tag that should be unassigned.
         */
        void unassignTag(ProxyTag tagData);
    }

    /**
     * Registers a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener
     */
    void addTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.addTagAssignmentListener(listener);
        assignedTagField.addTagAssignmentListener(listener);
    }

    /**
     * Removes a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener
     */
    void removeTagAssignmentListener(final TagAssignmentListener listener) {
        assignableTagsComboBox.removeTagAssignmentListener(listener);
        assignedTagField.removeTagAssignmentListener(listener);
    }

    /**
     * Returns all assigned tags.
     * 
     * @return {@link List} with tags.
     */
    public List<ProxyTag> getAssignedTags() {
        return assignedTagField.getTags();
    }
}
