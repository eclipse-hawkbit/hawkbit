/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanelLayout.TagAssignmentListener;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.rollout.ProxyFontIcon;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A panel that shows the assigned tags. A click on a tag unsassigns the tag
 * from the {@link Target} or {@link DistributionSet}.
 */
public class TagListField extends CssLayout {
    private static final long serialVersionUID = 1L;

    private final transient Map<ProxyTag, HorizontalLayout> tagButtons = new TreeMap<>(
            Comparator.comparing(ProxyTag::getName, String.CASE_INSENSITIVE_ORDER));
    private final transient Set<TagAssignmentListener> listeners = Sets.newConcurrentHashSet();
    private final VaadinMessageSource i18n;
    private final boolean readOnlyMode;

    /**
     * Constructor.
     * 
     * @param i18n
     * @param readOnlyMode
     *            if <code>true</code> no unassignment can be done
     */
    TagListField(final VaadinMessageSource i18n, final boolean readOnlyMode) {
        this.i18n = i18n;
        this.readOnlyMode = readOnlyMode;
        setSizeFull();
    }

    /**
     * Initializes the Tag panel with all assigned tags.
     * 
     * @param assignedTags
     *            assigned tags
     */
    void initializeAssignedTags(final List<ProxyTag> assignedTags) {
        removeAllComponents();

        assignedTags.forEach(tag -> {
            final HorizontalLayout tagButtonLayout = buildTagButtonLayout(tag);
            tagButtons.put(tag, tagButtonLayout);
        });

        addTagButtonsAsComponents();
    }

    /**
     * Adds a tag
     * 
     * @param tagName
     * @param tagColor
     */
    void addTag(final ProxyTag tagData) {
        if (!tagButtons.containsKey(tagData)) {
            removeAllComponents();

            final HorizontalLayout tagButtonLayout = buildTagButtonLayout(tagData);
            tagButtons.put(tagData, tagButtonLayout);

            addTagButtonsAsComponents();
        }
    }

    private void addTagButtonsAsComponents() {
        tagButtons.values().forEach(this::addComponent);
    }

    private HorizontalLayout buildTagButtonLayout(final ProxyTag tagData) {
        final Label colourIcon = buildColourIcon(tagData.getId(), tagData.getColour());
        final Button tagButton = buildTagButton(tagData);

        final HorizontalLayout tagButtonLayout = new HorizontalLayout();
        tagButtonLayout.setSpacing(false);
        tagButtonLayout.setMargin(false);
        tagButtonLayout.addStyleName(SPUIStyleDefinitions.TAG_BUTTON_WITH_BACKGROUND);

        tagButtonLayout.addComponent(colourIcon);
        tagButtonLayout.setComponentAlignment(colourIcon, Alignment.MIDDLE_LEFT);
        tagButtonLayout.setExpandRatio(colourIcon, 0.0F);

        tagButtonLayout.addComponent(tagButton);
        tagButtonLayout.setComponentAlignment(tagButton, Alignment.MIDDLE_LEFT);
        tagButtonLayout.setExpandRatio(tagButton, 1.0F);

        return tagButtonLayout;
    }

    private static final Label buildColourIcon(final Long clickedFilterId, final String colour) {
        final ProxyFontIcon colourFontIcon = new ProxyFontIcon(VaadinIcons.CIRCLE, ValoTheme.LABEL_TINY, "", colour);
        final String colourIconId = new StringBuilder(UIComponentIdProvider.ASSIGNED_TAG_ID_PREFIX)
                .append(".colour-icon.").append(clickedFilterId).toString();

        return SPUIComponentProvider.getLabelIcon(colourFontIcon, colourIconId);
    }

    private Button buildTagButton(final ProxyTag tagData) {
        final String tagButtonId = new StringBuilder(UIComponentIdProvider.ASSIGNED_TAG_ID_PREFIX).append(".")
                .append(tagData.getId()).toString();

        final Button tagButton = new Button(tagData.getName().concat(" ×"), e -> removeTagAssignment(tagData));
        tagButton.setId(tagButtonId);
        tagButton.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLICK_TO_REMOVE));
        tagButton.addStyleName(SPUIDefinitions.TEXT_STYLE);
        tagButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        tagButton.addStyleName("button-no-border");

        tagButton.setEnabled(!readOnlyMode);

        return tagButton;
    }

    private void removeTagAssignment(final ProxyTag tagData) {
        removeTag(tagData);
        notifyListenersTagAssignmentRemoved(tagData);
    }

    /**
     * Updates a tag.
     * 
     * @param tagData
     */
    void updateTag(final ProxyTag tagData) {
        findTagById(tagData.getId()).ifPresent(tagToUpdate -> {
            tagButtons.remove(tagToUpdate);
            addTag(tagData);
        });
    }

    private Optional<ProxyTag> findTagById(final Long id) {
        return tagButtons.keySet().stream().filter(tagData -> tagData.getId().equals(id)).findAny();
    }

    /**
     * Removes a tag from the field.
     * 
     * @param tagData
     */
    void removeTag(final ProxyTag tagData) {
        final HorizontalLayout buttonLayout = tagButtons.get(tagData);
        if (buttonLayout != null) {
            tagButtons.remove(tagData);
            removeComponent(buttonLayout);
        }
    }

    /**
     * Removes a tag from the field.
     * 
     * @param tagData
     */
    void removeTag(final Long tagId) {
        findTagById(tagId).ifPresent(this::removeTag);
    }

    /**
     * Removes all tags from the field.
     */
    void removeAllTags() {
        removeAllComponents();
        tagButtons.clear();
    }

    /**
     * Registers a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener to register
     */
    void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a {@link TagAssignmentListener}.
     * 
     * @param listener
     *            the listener to remove
     */
    void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssignmentRemoved(final ProxyTag tagData) {
        listeners.forEach(listener -> listener.unassignTag(tagData));
    }

    /**
     * Returns all assigned tags shown in the field.
     * 
     * @return a {@link List} with tags
     */
    List<ProxyTag> getTags() {
        return Lists.newArrayList(tagButtons.keySet());
    }
}
