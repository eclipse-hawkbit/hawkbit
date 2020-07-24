/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanelLayout.TagAssignmentListener;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.google.common.collect.Sets;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Combobox that lists all available Tags that can be assigned to a
 * {@link Target} or {@link DistributionSet}.
 */
public class TagAssignementComboBox extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    private final Collection<ProxyTag> allAssignableTags;
    private final transient Set<TagAssignmentListener> listeners = Sets.newConcurrentHashSet();

    private final ComboBox<ProxyTag> assignableTagsComboBox;

    private final boolean readOnlyMode;

    /**
     * Constructor.
     * 
     * @param i18n
     *            the i18n
     * @param readOnlyMode
     *            if true the combobox will be disabled so no assignment can be
     *            done.
     */
    TagAssignementComboBox(final VaadinMessageSource i18n, final boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;

        setWidth("100%");

        this.assignableTagsComboBox = getAssignableTagsComboBox(
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_SELECT_TAG));

        this.allAssignableTags = new HashSet<>();
        this.assignableTagsComboBox.setItems(allAssignableTags);

        addComponent(assignableTagsComboBox);
    }

    private ComboBox<ProxyTag> getAssignableTagsComboBox(final String description) {
        final ComboBox<ProxyTag> tagsComboBox = new ComboBox<>();

        tagsComboBox.setId(UIComponentIdProvider.TAG_SELECTION_ID);
        tagsComboBox.setDescription(description);
        tagsComboBox.addStyleName(ValoTheme.COMBOBOX_TINY);
        tagsComboBox.setEnabled(!readOnlyMode);
        tagsComboBox.setWidth("100%");
        tagsComboBox.setEmptySelectionAllowed(true);

        tagsComboBox.setItemCaptionGenerator(ProxyTag::getName);
        tagsComboBox.addValueChangeListener(event -> assignTag(event.getValue()));

        return tagsComboBox;
    }

    private void assignTag(final ProxyTag tagData) {
        if (tagData == null || readOnlyMode) {
            return;
        }

        allAssignableTags.remove(tagData);
        assignableTagsComboBox.clear();
        assignableTagsComboBox.getDataProvider().refreshAll();

        notifyListenersTagAssigned(tagData);
    }

    /**
     * Initializes the Combobox with all assignable tags.
     * 
     * @param assignableTags
     *            assignable tags
     */
    void initializeAssignableTags(final List<ProxyTag> assignableTags) {
        allAssignableTags.addAll(assignableTags);
        assignableTagsComboBox.getDataProvider().refreshAll();
    }

    /**
     * Removes all Tags from Combobox.
     */
    void removeAllTags() {
        allAssignableTags.clear();
        assignableTagsComboBox.clear();
        assignableTagsComboBox.getDataProvider().refreshAll();
    }

    /**
     * Adds an assignable Tag to the combobox.
     * 
     * @param tagData
     *            the data of the Tag
     */
    void addAssignableTag(final ProxyTag tagData) {
        if (tagData == null) {
            return;
        }

        allAssignableTags.add(tagData);
        assignableTagsComboBox.getDataProvider().refreshAll();
    }

    /**
     * Updates an assignable Tag in the combobox.
     * 
     * @param tagData
     *            the data of the Tag
     */
    void updateAssignableTag(final ProxyTag tagData) {
        if (tagData == null) {
            return;
        }

        findAssignableTagById(tagData.getId()).ifPresent(tagToUpdate -> updateAssignableTag(tagToUpdate, tagData));
    }

    private Optional<ProxyTag> findAssignableTagById(final Long id) {
        return allAssignableTags.stream().filter(tag -> tag.getId().equals(id)).findAny();
    }

    private void updateAssignableTag(final ProxyTag oldTag, final ProxyTag newTag) {
        allAssignableTags.remove(oldTag);
        allAssignableTags.add(newTag);

        assignableTagsComboBox.getDataProvider().refreshAll();
    }

    /**
     * Removes an assignable tag from the combobox.
     * 
     * @param tagId
     *            the tag Id of the Tag that should be removed.
     */
    void removeAssignableTag(final Long tagId) {
        findAssignableTagById(tagId).ifPresent(this::removeAssignableTag);
    }

    /**
     * Removes an assignable tag from the combobox.
     * 
     * @param tagData
     *            the {@link ProxyTag} of the Tag that should be removed.
     */
    void removeAssignableTag(final ProxyTag tagData) {
        allAssignableTags.remove(tagData);
        assignableTagsComboBox.getDataProvider().refreshAll();
    }

    /**
     * Registers an {@link TagAssignmentListener} on the combobox.
     * 
     * @param listener
     *            the listener to register
     */
    void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a {@link TagAssignmentListener} from the combobox,
     * 
     * @param listener
     *            the listener that should be removed.
     */
    void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssigned(final ProxyTag tagData) {
        listeners.forEach(listener -> listener.assignTag(tagData));
    }
}
