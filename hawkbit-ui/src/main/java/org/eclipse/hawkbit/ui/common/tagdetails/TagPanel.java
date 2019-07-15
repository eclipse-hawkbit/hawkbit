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

import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken.TagData;

import com.google.common.collect.Lists;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

public class TagPanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final String NAME_PROPERTY = "name";
    private static final String COLOR_PROPERTY = "color";

    private final IndexedContainer allAssignableTags;
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();

    private final ComboBox assignableTagsComboBox;
    private final TokenField assignedTagField;

    public TagPanel() {
        allAssignableTags = new IndexedContainer();
        allAssignableTags.addContainerProperty(NAME_PROPERTY, String.class, "");
        allAssignableTags.addContainerProperty(COLOR_PROPERTY, String.class, "");

        assignableTagsComboBox = new ComboBox();
        addComponent(assignableTagsComboBox);
        assignableTagsComboBox.setContainerDataSource(allAssignableTags);

        assignedTagField = new TokenField();
        addComponent(assignedTagField);
    }

    public void initializeTags(final List<TagData> allTags, final List<TagData> assignedTags) {
        allAssignableTags.removeAllItems();
        assignedTagField.removeAllTokens();

        allTags.forEach(this::addAssignableTag);
        assignedTags.forEach(this::setAssignedTag);
    }

    private void addAssignableTag(final TagData tagData) {
        final Item item = allAssignableTags.addItem(tagData.getName());
        if (item == null) {
            return;
        }
        item.getItemProperty(NAME_PROPERTY).setValue(tagData.getName());
        item.getItemProperty(COLOR_PROPERTY).setValue(tagData.getColor());
    }

    public void setAssignedTag(final TagData tagData) {
        // the assigned tag is no longer assignable
        allAssignableTags.removeItem(tagData.getName());
        // show it as an assigned tag
        assignedTagField.addToken(tagData.getName(), tagData.getColor());
    }

    public void removeAssignedTag(final TagData tagData) {
        // the un-assigned tag is now assignable
        addAssignableTag(tagData);
        // remove ot from the assigned tags
        assignedTagField.removeToken(tagData.getName());
    }

    public void tagCreated(final TagData tagData) {
        addAssignableTag(tagData);
    }

    public void tagDeleted(final TagData tagData) {
        // TODO implement
    }

    public void tagUpdated(final TagData tagData) {
        // TODO implement
    }

    public interface TagAssignmentListener {
        public void assignTag(String tagName);

        public void unassignTag(String tagName);
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
        assignedTagField.addTagAssignmentListener(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
        assignedTagField.removeTagAssignmentListener(listener);
    }

    private void notifyListenersTagAssigned(final String tagName) {
        listeners.forEach(listener -> listener.assignTag(tagName));
    }
}
