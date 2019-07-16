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

import org.eclipse.hawkbit.ui.common.tagdetails.TagPanel.TagAssignmentListener;
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

public class TagListField extends CssLayout {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, Button> tagButtons = new ConcurrentHashMap<>();
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();
    private final VaadinMessageSource i18n;
    private final boolean readOnlyMode;

    public TagListField(final VaadinMessageSource i18n, final boolean readOnlyMode) {
        this.i18n = i18n;
        this.readOnlyMode = readOnlyMode;
        setSizeFull();
    }

    public void addTag(final String tagName, final String tagColor) {
        if (!tagButtons.containsKey(tagName)) {
            final Button tagButton = SPUIComponentProvider.getButton(
                    UIComponentIdProvider.ASSIGNED_TAG_ID_PREFIX + tagName, tagName,
                    i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLICK_TO_REMOVE), null, false, null,
                    SPUITagButtonStyle.class);
            tagButtons.put(tagName, tagButton);
            tagButton.setData(tagName);
            tagButton.addClickListener(e -> removeTagAssignment((String) e.getButton().getData()));
            tagButton.addStyleName(SPUIStyleDefinitions.TAG_BUTTON_WITH_BACKGROUND);
            tagButton.setEnabled(!readOnlyMode);
            addComponent(tagButton, getComponentCount());
            updateTag(tagName, tagColor);
        }
    }

    public void updateTag(final String tagName, final String tagColor) {
        final Button button = tagButtons.get(tagName);
        if (button != null) {
            button.addStyleName(SPUIDefinitions.TEXT_STYLE + " " + SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
            button.setCaption("<span style=\" color:" + tagColor + " !important;\">" + FontAwesome.CIRCLE.getHtml()
                    + "</span>" + " " + tagName.concat("  ×"));
            button.setCaptionAsHtml(true);
        }
    }

    private void removeTagAssignment(final String tagName) {
        removeTag(tagName);
        notifyListenersTagAssignmentRemoved(tagName);
    }

    public void removeTag(final String tagName) {
        final Button button = tagButtons.get(tagName);
        if (button != null) {
            tagButtons.remove(tagName);
            removeComponent(button);
        }
    }

    public void removeAllTags() {
        removeAllComponents();
        tagButtons.clear();
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssignmentRemoved(final String tagName) {
        listeners.forEach(listener -> listener.unassignTag(tagName));
    }
}
