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

import org.eclipse.hawkbit.ui.common.builder.ComboBoxBuilder;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanel.TagAssignmentListener;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.google.common.collect.Lists;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class TagAssignementComboBox extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    private static final String NAME_PROPERTY = "name";
    private static final String COLOR_PROPERTY = "color";

    private final IndexedContainer allAssignableTags;
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();

    private final ComboBox assignableTagsComboBox;
    private final Button assignButton;

    public TagAssignementComboBox(final VaadinMessageSource i18n, final boolean readOnlyMode) {
        allAssignableTags = new IndexedContainer();
        allAssignableTags.addContainerProperty(NAME_PROPERTY, String.class, "");
        allAssignableTags.addContainerProperty(COLOR_PROPERTY, String.class, "");

        assignableTagsComboBox = new ComboBoxBuilder().setId(UIComponentIdProvider.TAG_SELECTION_ID)
                .setPrompt(i18n.getMessage(UIMessageIdProvider.TOOLTIP_SELECT_TAG))
                .setValueChangeListener(e -> onSelectionChanged()).buildCombBox();
        addComponent(assignableTagsComboBox);
        assignableTagsComboBox.setContainerDataSource(allAssignableTags);
        assignableTagsComboBox.setNullSelectionAllowed(true);
        assignableTagsComboBox.select(assignableTagsComboBox.getNullSelectionItemId());
        assignableTagsComboBox.addStyleName(SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
        assignableTagsComboBox.addStyleName(ValoTheme.COMBOBOX_TINY);
        assignableTagsComboBox.setEnabled(!readOnlyMode);

        assignButton = SPUIComponentProvider.getButton(UIComponentIdProvider.ASSIGN_TAG, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_ASSIGN_TAG), null, false, FontAwesome.LINK,
                SPUIButtonStyleNoBorder.class);
        addComponent(assignButton);
        assignButton.addStyleName(SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
        assignButton.addStyleName(SPUIStyleDefinitions.ASSIGN_TAG_BUTTON);
        assignButton.addClickListener(e -> assignSelectedTag());
        assignButton.setEnabled(false);
    }

    private void assignSelectedTag() {
        final String tagName = (String) assignableTagsComboBox.getValue();
        if (tagName != null) {
            allAssignableTags.removeItem(tagName);
            notifyListenersTagAssigned(tagName);
        }
        assignableTagsComboBox.select(assignableTagsComboBox.getNullSelectionItemId());
    }

    private void onSelectionChanged() {
        final Object value = assignableTagsComboBox.getValue();
        assignButton.setEnabled(value != null && value != assignableTagsComboBox.getNullSelectionItemId());
    }

    public void removeAllTags() {
        allAssignableTags.removeAllItems();
        assignableTagsComboBox.select(assignableTagsComboBox.getNullSelectionItemId());
    }

    public void addAssignableTag(final TagData tagData) {
        final Item item = allAssignableTags.addItem(tagData.getName());
        if (item == null) {
            return;
        }
        item.getItemProperty(NAME_PROPERTY).setValue(tagData.getName());
        item.getItemProperty(COLOR_PROPERTY).setValue(tagData.getColor());
    }

    public void removeAssignableTag(final TagData tagData) {
        allAssignableTags.removeItem(tagData.getName());
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssigned(final String tagName) {
        listeners.forEach(listener -> listener.assignTag(tagName));
    }
}
