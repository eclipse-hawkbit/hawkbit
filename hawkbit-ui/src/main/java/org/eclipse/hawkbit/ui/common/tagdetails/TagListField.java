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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanelLayout.TagAssignmentListener;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.google.common.collect.Lists;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;

/**
 * A panel that shows the assigned tags. A click on a tag unsassigns the tag
 * from the {@link Target} or {@link DistributionSet}.
 */
public class TagListField extends CssLayout {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, Button> tagButtons = new ConcurrentHashMap<>();
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();
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
     * Adds a tag
     * 
     * @param tagName
     * @param tagColor
     */
    void addTag(final String tagName, final String tagColor) {
        if (!tagButtons.containsKey(tagName)) {
            final Button tagButton = createButton(tagName, tagColor);
            addComponent(tagButton, getComponentCount());
            tagButtons.put(tagName, tagButton);
        }
    }

    private Button createButton(final String tagName, final String tagColor) {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.ASSIGNED_TAG_ID_PREFIX + tagName,
                tagName, i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLICK_TO_REMOVE), null, false, null,
                SPUITagButtonStyle.class);
        button.setData(tagName);
        button.addClickListener(e -> removeTagAssignment((String) e.getButton().getData()));
        button.addStyleName(SPUIStyleDefinitions.TAG_BUTTON_WITH_BACKGROUND);
        button.addStyleName(SPUIDefinitions.TEXT_STYLE + " " + SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
        button.setEnabled(!readOnlyMode);
        button.setCaption("<span style=\" color:" + tagColor + " !important;\">" + FontAwesome.CIRCLE.getHtml()
                + "</span>" + " " + tagName.concat("  ×"));
        button.setCaptionAsHtml(true);
        return button;
    }

    private void removeTagAssignment(final String tagName) {
        removeTag(tagName);
        notifyListenersTagAssignmentRemoved(tagName);
    }

    /**
     * Removes a tag from the field.
     * 
     * @param tagName
     */
    void removeTag(final String tagName) {
        final Button button = tagButtons.get(tagName);
        if (button != null) {
            tagButtons.remove(tagName);
            removeComponent(button);
        }
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

    private void notifyListenersTagAssignmentRemoved(final String tagName) {
        listeners.forEach(listener -> listener.unassignTag(tagName));
    }

    /**
     * Returns all assigned tags shown in the field.
     * 
     * @return a {@link List} with tags
     */
    List<String> getTags() {
        return Lists.newArrayList(tagButtons.keySet());
    }
}
